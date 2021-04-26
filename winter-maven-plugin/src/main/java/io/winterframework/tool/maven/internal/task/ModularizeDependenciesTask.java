/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.winterframework.tool.maven.internal.task;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.AbstractMojo;

import io.winterframework.tool.maven.internal.DependencyModule;
import io.winterframework.tool.maven.internal.NullPrintStream;
import io.winterframework.tool.maven.internal.ProjectModule;
import io.winterframework.tool.maven.internal.Task;
import io.winterframework.tool.maven.internal.TaskExecutionException;
import io.winterframework.tool.maven.internal.ProgressBar.Step;

/**
 * <p>
 * Modularizes non-modular project dependencies.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class ModularizeDependenciesTask extends Task<Set<DependencyModule>> {

	private final ToolProvider jdeps;
	private final ProjectModule projectModule;
	private final Path jmodsExplodedPath;
	
	public ModularizeDependenciesTask(AbstractMojo mojo, ToolProvider jdeps, ProjectModule projectModule, Path jmodsExplodedPath) {
		super(mojo);
		this.jdeps = jdeps;
		this.projectModule = projectModule;
		this.jmodsExplodedPath = jmodsExplodedPath;
	}
	
	@Override
	public void setStep(Step step) {
		if(step != null) {
			step.setDescription("Modularizing project dependencies...");
		}
		super.setStep(step);
	}

	@Override
	protected Set<DependencyModule> execute() throws TaskExecutionException {
		if(this.projectModule.getModuleDependencies().stream().anyMatch(DependencyModule::isMarked)) {
			if(this.verbose) {
				this.getLog().info("[ Modularizing project dependencies... ]");
			}
			try {
				Set<DependencyModule> modularizedDependencies = new HashSet<>();
				for(DependencyModule dependency : this.projectModule.getModuleDependencies()) {
					if(dependency.isMarked()) {
						if(dependency.isAutomatic()) {
							if(this.verbose) {
								this.getLog().info(" - modularizing dependency " + dependency + " to " + dependency.getExplodedJmodPath() + "...");
							}
							try {
								if(Files.exists(dependency.getExplodedJmodPath())) {
									try (Stream<Path> walk = Files.walk(dependency.getExplodedJmodPath())) {
										for(Iterator<Path> pathIterator = walk.sorted(Comparator.reverseOrder()).iterator(); pathIterator.hasNext();) {
											Files.delete(pathIterator.next());
										}
									}
									catch(IOException e) {
										throw new TaskExecutionException("Error cleaning dependency " + dependency, e);
									}
								}
								Files.deleteIfExists(dependency.getJmodPath());
								Files.copy(dependency.getSourcePath(), dependency.getJmodPath());
								
								this.unpackDependency(dependency);
								if(dependency.getOverriddenModuleInfoPath().map(Files::exists).orElse(false)) {
									if(this.verbose) {
										this.getLog().info("   - using " + dependency.getOverriddenModuleInfoPath().get());
									}
									Files.copy(dependency.getOverriddenModuleInfoPath().get(), dependency.getModuleInfoPath());
								}
								else {
									this.generateModuleInfo(dependency);
								}
								modularizedDependencies.add(dependency);
							}
							finally {
								Files.deleteIfExists(dependency.getJmodPath());
							}
						}
						else {
							if(this.verbose) {
								this.getLog().info(" - copying modular dependency " + dependency + " to " + dependency.getJmodPath() + "...");
							}
							Files.deleteIfExists(dependency.getJmodPath());
							Files.copy(dependency.getSourcePath(), dependency.getJmodPath());
						}
					}
					else {
						if(this.verbose) {
							this.getLog().info(" - skipping dependency " + dependency + " which is up to date");
						}
					}
				}
				return modularizedDependencies;
			}
			catch (IOException e) {
				throw new TaskExecutionException("Error modularizing dependencies, activate '-Dwinter.verbose=true' to display full log", e);
			}
		}
		else {
			if(this.verbose) {
				this.getLog().info("[ Project dependencies are up to date ]");
			}
			return this.projectModule.getModuleDependencies();
		}
	}
	
	private void unpackDependency(DependencyModule dependency) throws TaskExecutionException {
		Path explodedJmodPath = dependency.getExplodedJmodPath();
		try(JarFile moduleJar = new JarFile(dependency.getSourcePath().toFile(), true, ZipFile.OPEN_READ, Runtime.version())) {
			boolean webjar = dependency.getModuleName().startsWith("org.webjars");
			String webjarName = null;
			if(webjar) {
				webjarName = dependency.getModuleName().substring(dependency.getArtifact().getGroupId().length() + 1);
			}
			
			Path webjarResourcesPath = Paths.get("META-INF/resources/webjars/");
			if(this.verbose) {
				this.getLog().info("   - unpacking " + (webjar ? "WebJar " : "") + dependency + " to " + explodedJmodPath);
			}
			
			for(JarEntry jarEntry : moduleJar.stream().collect(Collectors.toList())) {
				Path jarEntryPath = Paths.get(jarEntry.getName());
				Path targetEntry = explodedJmodPath.resolve(jarEntry.getName()).normalize();
				if(webjar && jarEntryPath.startsWith(webjarResourcesPath) && jarEntryPath.getNameCount() > webjarResourcesPath.getNameCount()) {
					if(jarEntryPath.getNameCount() == webjarResourcesPath.getNameCount() + 1) {
						continue;
					}
					targetEntry = explodedJmodPath.resolve(webjarResourcesPath.resolve(webjarName).resolve(jarEntryPath.subpath(webjarResourcesPath.getNameCount() + 1, jarEntryPath.getNameCount())));
				}
				else {
					targetEntry = explodedJmodPath.resolve(jarEntry.getName()).normalize();
				}
				
				if(!targetEntry.startsWith(explodedJmodPath)) {
					throw new IOException("Entry is outside of the module output dir: " + jarEntry.getName());
				}
				
				if(jarEntry.isDirectory()) {
					Files.createDirectories(targetEntry);
				}
				else {
					Files.createDirectories(targetEntry.getParent());
					Files.copy(moduleJar.getInputStream(jarEntry), targetEntry);
				}
			}
		} 
		catch (IOException e) {
			throw new TaskExecutionException("Error unpacking dependency " + dependency + ", activate '-Dwinter.verbose=true' to display full log", e);
		}
	}
	
	private void generateModuleInfo(DependencyModule dependency) throws TaskExecutionException {
		try {
			Set<URL> urls = new HashSet<>();
			for(DependencyModule d : this.projectModule.getModuleDependencies()) {
				urls.add(d.getSourcePath().toUri().toURL());
			}
			
			// We should first look at the META-INF/services defined in the jar:
			// if a service correspond to an interface that can't be resolved it must be excluded
			// If we get there we must have a JAR resource because unpack uses JarFile
			URI dependencyJarURI = URI.create("jar:" + dependency.getJmodPath().toAbsolutePath().toUri());
			try (FileSystem fs = FileSystems.newFileSystem(dependencyJarURI, Map.of("create", "false"));URLClassLoader classLoader = new URLClassLoader(urls.stream().toArray(URL[]::new));) {
				Path services = fs.getPath("META-INF", "services").toAbsolutePath();
				if(Files.exists(services)) {
					for(Path servicePath : Files.list(services).toArray(Path[]::new)) {
						try {
							classLoader.loadClass(servicePath.getFileName().toString());
						} 
						catch (ClassNotFoundException e1) {
							if(this.verbose) {
								this.getLog().warn("Ignoring service " + servicePath.getFileName().toString() + " provided in module " + dependency + " which doesn't exist on the classpath");
							}
							Files.delete(servicePath);
						}
					}
				}
			}
			
			String version = Integer.toString(Runtime.version().major());
			
			String jdeps_modulePath = this.projectModule.getModuleDependencies().stream().map(d -> d.getSourcePath().toString()).collect(Collectors.joining(System.getProperty("path.separator")));
			String[] jdeps_args = {
				"--ignore-missing-deps", 
				"--multi-release", version, 
				"--module-path", jdeps_modulePath, 
				"--generate-module-info", 
				this.jmodsExplodedPath.toString(), 
				dependency.getJmodPath().toString()
			};
			
			if(verbose) {
				this.getLog().info("   - jdeps " + Arrays.stream(jdeps_args).collect(Collectors.joining(" ")));
			}
			if(this.jdeps.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), jdeps_args) == 0) {
				Files.move(dependency.getExplodedJmodPath().resolve(Paths.get("versions", version, "module-info.java")), dependency.getModuleInfoPath());
				Files.delete(dependency.getExplodedJmodPath().resolve(Paths.get("versions", version)));
				Files.delete(dependency.getExplodedJmodPath().resolve(Paths.get("versions")));
			}
			else {
				throw new TaskExecutionException("Error generating module-info.java for " + dependency + ", activate '-Dwinter.verbose=true' to display full log");
			}
		}
		catch (IOException e) {
			throw new TaskExecutionException("Error generating module-info.java for " + dependency + ", activate '-Dwinter.verbose=true' to display full log", e);
		}
	}
}