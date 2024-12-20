/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.tool.buildtools.internal;

import io.inverno.tool.buildtools.BuildJmodTask;
import io.inverno.tool.buildtools.DebugTask;
import io.inverno.tool.buildtools.Dependency;
import io.inverno.tool.buildtools.ModularizeDependenciesTask;
import io.inverno.tool.buildtools.ModuleInfo;
import io.inverno.tool.buildtools.Project;
import io.inverno.tool.buildtools.RunTask;
import io.inverno.tool.buildtools.StartTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import io.inverno.tool.buildtools.internal.parser.ModuleInfoParser;
import io.inverno.tool.buildtools.internal.parser.ParseException;
import io.inverno.tool.buildtools.internal.parser.StreamProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

/**
 * <p>
 * Generic {@link ModularizeDependenciesTask} implementation.
 * </p>
 * 
 * <p>
 * This implementation relies on JDK's {@code jdeps} tool for the generation of module descriptors.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericModularizeDependenciesTask extends AbstractTask<Set<Dependency>, ModularizeDependenciesTask> implements ModularizeDependenciesTask {

	private static final Logger LOGGER = LogManager.getLogger(GenericModularizeDependenciesTask.class);
	
	private static final int UNITARY_WEIGHT = 15;
	
	private static final PrintStream OUT = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.INFO)
			.setAutoFlush(true)
			.buildPrintStream();
	
	private static final PrintStream ERR = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.ERROR)
			.setAutoFlush(true)
			.buildPrintStream();
	
	private Optional<Path> moduleOverridesPath = Optional.empty();
	
	private Map<String, ? extends ModuleInfo> moduleOverrides = Map.of();

	/**
	 * <p>
	 * Creates a generic modularize dependencies task.
	 * </p>
	 * 
	 * @param project the project.
	 */
	public GenericModularizeDependenciesTask(Project project) {
		super(project);
	}

	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		long modularizedDependenciesCount = project.getDependencies().stream()
			.filter(dependency -> dependency.isMarked() && dependency.isAutomatic())
			.count();
		
		return modularizedDependenciesCount + " project dependencies modularized";
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		int modularizedDependenciesCount = Long.valueOf(project.getDependencies().stream()
			.filter(dependency -> dependency.isMarked() && dependency.isAutomatic())
			.count()).intValue();
		
		return modularizedDependenciesCount * UNITARY_WEIGHT;
	}
	
	@Override
	public ModularizeDependenciesTask moduleOverridesPath(Path moduleOverridesPath) {
		this.moduleOverridesPath = Optional.ofNullable(moduleOverridesPath);
		return this;
	}

	@Override
	public ModularizeDependenciesTask moduleOverrides(List<? extends ModuleInfo> moduleOverrides) {
		this.moduleOverrides = moduleOverrides != null ? moduleOverrides.stream().collect(Collectors.toMap(ModuleInfo::getName, Function.identity())) : Map.of();
		return this;
	}
	
	/**
	 * <p>
	 * Returns the path to the user-provided {@code module-info.java} for the specified dependency.
	 * </p>
	 * 
	 * <p>
	 * When provided, no descriptor is generated for that dependency.
	 * </p>
	 * 
	 * @param dependency a build dependency
	 * 
	 * @return an optional returning the path to the overriding module descriptor or an empty optional
	 */
	private Optional<Path> getOverriddingModuleInfoPath(BuildDependency dependency) {
		return this.moduleOverridesPath
			.map(path -> path.resolve(Path.of(dependency.getModuleName(), "module-info.java")))
			.filter(Files::exists);
	}
	
	@Override
	protected Set<Dependency> doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Modularizing, compiling and repackaging project dependencies...");
		}
		
		LOGGER.info("[ Modularizing, compiling and repackaging dependencies for project {}... ]", project);

		try {
			Files.createDirectories(project.getModulesPath());
			Files.createDirectories(project.getModulesExplodedPath());
			Files.createDirectories(project.getModulesUnnamedPath());
		}
		catch(IOException e) {
			throw new TaskExecutionException("Error initializing working directory");
		}
		
		if(project.getDependencies().stream().anyMatch(BuildDependency::isMarked)) {
			ProgressBar.Step modularizeStep = step != null ? step.addStep(80, "Modularizing project dependencies...") : null;
			ProgressBar.Step compileStep = step != null ? step.addStep(10, "Compiling project dependencies...") : null;
			ProgressBar.Step repackageStep = step != null ? step.addStep(10, "Repackaging project dependencies...") : null;
		
			Set<Dependency> modularizeDependencies;
			try {
				modularizeDependencies = this.modularizeDependencies(project);
			}
			finally {
				if(modularizeStep != null) {
					modularizeStep.done();
				}
			}

			try {
				this.compileModularizedDependencies(project);
			}
			finally {
				if(compileStep != null) {
					compileStep.done();
				}
			}

			try {
				this.packageModularizedDependencies(project);
			}
			finally {
				if(repackageStep != null) {
					repackageStep.done();
				}
			}
			return modularizeDependencies;
		}
		else {
			LOGGER.info("[ Project dependencies are up to date ]");
			return Set.of();
		}
	}
	
	/**
	 * <p>
	 * Modularizes the project dependencies.
	 * </p>
	 * 
	 * <p>
	 * A module decriptor is generated for automatic and unnamed dependencies only when no descriptor was provided in {@link #moduleOverridesPath(java.nio.file.Path) } for that particular dependency. 
	 * If a {@link ModuleInfo} was specified with {@link #moduleOverrides(java.util.List) } it is merged with the generated descriptor.
	 * </p>
	 * 
	 * @param project the build project
	 * 
	 * @return the set of dependencies that were modularized
	 * 
	 * @throws TaskExecutionException if there was an error modularizing the dependencies
	 */
	private Set<Dependency> modularizeDependencies(BuildProject project) throws TaskExecutionException {
		LOGGER.info("[ Modularizing project dependencies... ]");
		try {
			Set<Dependency> modularizedDependencies = new HashSet<>();
			for(BuildDependency dependency : project.getDependencies()) {
				if(dependency.isMarked()) {
					if(dependency.isAutomatic()) {
						Path explodedJmodPath = dependency.getExplodedModulePath();
						LOGGER.info(" - modularizing dependency {} to {}...", dependency, explodedJmodPath);
						try {
							if(Files.exists(explodedJmodPath)) {
								try (Stream<Path> walk = Files.walk(explodedJmodPath)) {
									for(Iterator<Path> pathIterator = walk.sorted(Comparator.reverseOrder()).iterator(); pathIterator.hasNext();) {
										Files.delete(pathIterator.next());
									}
								}
								catch(IOException e) {
									throw new TaskExecutionException("Error cleaning dependency " + dependency, e);
								}
							}
							Files.deleteIfExists(dependency.getModulePath());
							Files.copy(dependency.getJarPath(), dependency.getModulePath());

							this.unpackDependency(dependency);

							Optional<Path> overriddenModuleInfoPath = this.getOverriddingModuleInfoPath(dependency);
							if(overriddenModuleInfoPath.isPresent()) {
								LOGGER.info("   - using ", overriddenModuleInfoPath.get());
								Files.copy(overriddenModuleInfoPath.get(), dependency.getModuleInfoPath());
							}
							else {
								this.generateModuleInfo(project, dependency);
							}
							modularizedDependencies.add(dependency.unwrap());
						}
						finally {
							Files.deleteIfExists(dependency.getModulePath());
						}
					}
					else {
						LOGGER.info(" - copying modular dependency {} to {}...", dependency, dependency.getModulePath());
						Files.deleteIfExists(dependency.getModulePath());
						Files.copy(dependency.getJarPath(), dependency.getModulePath());
					}
				}
				else {
					LOGGER.info(" - skipping dependency {} which is up to date", dependency);
				}
			}
			return modularizedDependencies;
		}
		catch (IOException e) {
			throw new TaskExecutionException("Error modularizing dependencies", e);
		}
	}
	
	/**
	 * <p>
	 * Unpacks the specified dependency JAR to the exploded module path.
	 * </p>
	 * 
	 * @param dependency the dependency to unpack
	 * 
	 * @throws TaskExecutionException if there was an error unpacking the dependency JAR
	 */
	private void unpackDependency(BuildDependency dependency) throws TaskExecutionException {
		Path jarSourcePath = dependency.getJarPath();
		if(!dependency.isNamed()) {
			LOGGER.info("   - setting Automatic-Module-Name in unnamed module JAR: {}", dependency.getUnnamedModulePath());

			// copy to modules-unnamed
			try {
				Files.deleteIfExists(dependency.getUnnamedModulePath());
				Files.copy(jarSourcePath, dependency.getUnnamedModulePath());
				jarSourcePath = dependency.getUnnamedModulePath();
				// Set Automatic-Module-Name
				
				try(FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + jarSourcePath.toUri()), Map.of())) {
					Path manifestPath = jarFs.getPath("META-INF", "MANIFEST.MF");
					if(Files.exists(manifestPath)) {
						try (InputStream is = Files.newInputStream(manifestPath)) {
							Manifest manifest = new Manifest(is);
							manifest.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), dependency.getModuleName());
							try (OutputStream jarOutput = Files.newOutputStream(manifestPath)) {
								manifest.write(jarOutput);
							}
						}
					}
					else {
						if(!Files.exists(manifestPath.getParent())) {
							jarFs.provider().createDirectory(manifestPath.getParent());
						}
						Manifest manifest = new Manifest();
						manifest.getMainAttributes().put(new Attributes.Name("Automatic-Module-Name"), dependency.getModuleName());
						try (OutputStream jarOutput = Files.newOutputStream(manifestPath)) {
							manifest.write(jarOutput);
						}
					}
				}
			}
			catch(IOException e) {
				throw new TaskExecutionException("Error copying unnamed dependency " + dependency + "", e);
			}
		}
		
		Path explodedModulePath = dependency.getExplodedModulePath();
		try(JarFile moduleJar = new JarFile(jarSourcePath.toFile(), true, ZipFile.OPEN_READ, Runtime.version())) {
			boolean webjar = dependency.getModuleName().startsWith("org.webjars");
			String webjarName = null;
			if(webjar) {
				webjarName = dependency.getModuleName().substring(dependency.getGroup().length() + 1);
			}
			
			Path webjarResourcesPath = Path.of("META-INF/resources/webjars/");
			LOGGER.info("   - unpacking {} {} to {}", webjar ? "WebJar " : "", dependency, explodedModulePath);
			
			for(JarEntry jarEntry : moduleJar.stream().collect(Collectors.toList())) {
				Path jarEntryPath = Path.of(jarEntry.getName());
				Path targetEntry;
				if(webjar && jarEntryPath.startsWith(webjarResourcesPath) && jarEntryPath.getNameCount() > webjarResourcesPath.getNameCount()) {
					if(jarEntryPath.getNameCount() == webjarResourcesPath.getNameCount() + 1) {
						continue;
					}
					targetEntry = explodedModulePath.resolve(webjarResourcesPath.resolve(webjarName).resolve(jarEntryPath.subpath(webjarResourcesPath.getNameCount() + 1, jarEntryPath.getNameCount())));
				}
				else {
					targetEntry = explodedModulePath.resolve(jarEntry.getName()).normalize();
				}
				
				if(!targetEntry.startsWith(explodedModulePath)) {
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
			throw new TaskExecutionException("Error unpacking dependency " + dependency + "", e);
		}
	}
	
	/**
	 * <p>
	 * Generates the {@code module-info.java} descriptor for the specified dependency.
	 * </p>
	 * 
	 * @param project    the build project
	 * @param dependency the build dependency
	 * 
	 * @throws TaskExecutionException if there was an error generating the module descriptor
	 */
	private void generateModuleInfo(BuildProject project, BuildDependency dependency) throws TaskExecutionException {
		try {
			Set<URL> urls = new HashSet<>();
			for(Dependency d : project.getDependencies()) {
				urls.add(d.getJarPath().toUri().toURL());
			}
			
			// We should first look at the META-INF/services defined in the jar:
			// if a service correspond to an interface that can't be resolved it must be excluded
			// If we get there we must have a JAR resource because unpack uses JarFile
			URI dependencyJarURI = URI.create("jar:" + dependency.getModulePath().toAbsolutePath().toUri());
			try (FileSystem fs = FileSystems.newFileSystem(dependencyJarURI, Map.of("create", "false"));URLClassLoader classLoader = new URLClassLoader(urls.stream().toArray(URL[]::new));) {
				Path services = fs.getPath("META-INF", "services").toAbsolutePath();
				if(Files.exists(services)) {
					for(Path servicePath : Files.list(services).filter(Files::isRegularFile).toArray(Path[]::new)) {
						try {
							classLoader.loadClass(servicePath.getFileName().toString());
						} 
						catch (ClassNotFoundException e1) {
							LOGGER.warn("Ignoring service {} provided in module {} which doesn't exist on the classpath", servicePath.getFileName().toString(), dependency);
							Files.delete(servicePath);
						}
					}
				}
			}
			
			String version = Integer.toString(Runtime.version().feature());
			
			String jdeps_modulePath = project.getDependencies().stream().map(d -> {
					if(d.isNamed()) {
						return d.getJarPath().toString();
					}
					else {
						return d.getUnnamedModulePath().toString();
					}
				}).collect(Collectors.joining(System.getProperty("path.separator")));
			
			Optional<ModuleInfo> moduleOverride = Optional.ofNullable(this.moduleOverrides.get(dependency.getModuleName()));
			
			List<String> jdeps_args = new LinkedList<>();
			
			jdeps_args.add("--ignore-missing-deps");
			jdeps_args.add("--multi-release");
			jdeps_args.add(version);
			jdeps_args.add("--module-path");
			jdeps_args.add(jdeps_modulePath);
			jdeps_args.add("--generate-module-info");
			// When generating a open module, no exports is created so let's keep it that way
			/*if(moduleOverride.map(override -> override.isOpen()).orElse(false)) {
				jdeps_args.add("--generate-open-module");
			}
			else {
				jdeps_args.add("--generate-module-info");
			}*/
			jdeps_args.add(project.getModulesExplodedPath().toString());
			jdeps_args.add(dependency.getModulePath().toString());
			
			LOGGER.info("   - jdeps {}", jdeps_args.stream().collect(Collectors.joining(" ")));
			if(JavaTools.JDEPS.run(OUT, ERR, jdeps_args.stream().toArray(String[]::new)) == 0) {
				Path explodedModulePath = dependency.getExplodedModulePath();
				Files.move(explodedModulePath.resolve(Path.of("versions", version, "module-info.java")), dependency.getModuleInfoPath());
				Files.delete(explodedModulePath.resolve(Path.of("versions", version)));
				Files.delete(explodedModulePath.resolve(Path.of("versions")));
				
				if(moduleOverride.isPresent()) {
					this.mergeModuleInfo(dependency, moduleOverride.get());
				}
			}
			else {
				throw new TaskExecutionException("Error generating module-info.java for " + dependency + "");
			}
		}
		catch (IOException e) {
			throw new TaskExecutionException("Error generating module-info.java for " + dependency + "", e);
		}
	}
	
	/**
	 * <p>
	 * Merges the specified module info override with the module descriptor of the specified dependency.
	 * </p>
	 * 
	 * @param dependency the build dependency
	 * @param moduleInfoOverride the module info override
	 * 
	 * @throws TaskExecutionException if there was an error merging the module info override with the dependency module descriptor
	 */
	private void mergeModuleInfo(BuildDependency dependency, ModuleInfo moduleInfoOverride) throws TaskExecutionException {
		LOGGER.info("   - overriding module-info.java");
		ModuleInfo moduleInfo;
		try(BufferedReader moduleInfoReader = Files.newBufferedReader(dependency.getModuleInfoPath())) {
			moduleInfo = new ModuleInfoParser(new StreamProvider(moduleInfoReader)).ModuleInfo();
			moduleInfo.setOpen(moduleInfoOverride.isOpen());
			
			// Requires
			Map<String, ModuleInfo.RequiresDirective> overriddenRequiresByName = moduleInfoOverride.getRequires().stream()
				.collect(Collectors.toMap(ModuleInfo.RequiresDirective::getModule, Function.identity()));
			ListIterator<ModuleInfo.RequiresDirective> requiresIterator = moduleInfo.getRequires().listIterator();
			while(requiresIterator.hasNext()) {
				if(overriddenRequiresByName.containsKey(requiresIterator.next().getModule())) {
					requiresIterator.remove();
				}
			}
			moduleInfo.getRequires().addAll(moduleInfoOverride.getRequires().stream().filter(directive -> !directive.isRemove()).collect(Collectors.toSet()));

			// Exports
			Map<String, ModuleInfo.ExportsDirective> overriddenExportsByName = moduleInfoOverride.getExports().stream()
				.collect(Collectors.toMap(ModuleInfo.ExportsDirective::getPackage, Function.identity()));
			ListIterator<ModuleInfo.ExportsDirective> exportsIterator = moduleInfo.getExports().listIterator();
			while(exportsIterator.hasNext()) {
				if(overriddenExportsByName.containsKey(exportsIterator.next().getPackage())) {
					exportsIterator.remove();
				}
			}
			moduleInfo.getExports().addAll(moduleInfoOverride.getExports().stream().filter(directive -> !directive.isRemove()).collect(Collectors.toSet()));

			// Opens
			Map<String, ModuleInfo.OpensDirective> overriddenOpensByName = moduleInfoOverride.getOpens().stream()
				.collect(Collectors.toMap(ModuleInfo.OpensDirective::getPackage, Function.identity()));
			ListIterator<ModuleInfo.OpensDirective> opensIterator = moduleInfo.getOpens().listIterator();
			while(opensIterator.hasNext()) {
				if(overriddenOpensByName.containsKey(opensIterator.next().getPackage())) {
					opensIterator.remove();
				}
			}
			moduleInfo.getOpens().addAll(moduleInfoOverride.getOpens().stream().filter(directive -> !directive.isRemove()).collect(Collectors.toSet()));
			
			// Uses
			Map<String, ModuleInfo.UsesDirective> overriddenUsesByName = moduleInfoOverride.getUses().stream()
				.collect(Collectors.toMap(ModuleInfo.UsesDirective::getType, Function.identity()));
			ListIterator<ModuleInfo.UsesDirective> usesIterator = moduleInfo.getUses().listIterator();
			while(opensIterator.hasNext()) {
				if(overriddenUsesByName.containsKey(usesIterator.next().getType())) {
					usesIterator.remove();
				}
			}
			moduleInfo.getUses().addAll(moduleInfoOverride.getUses().stream().filter(directive -> !directive.isRemove()).collect(Collectors.toSet()));
			
			// Provides
			Map<String, ModuleInfo.ProvidesDirective> overriddenProvidesByName = moduleInfoOverride.getProvides().stream()
				.collect(Collectors.toMap(ModuleInfo.ProvidesDirective::getType, Function.identity()));
			ListIterator<ModuleInfo.ProvidesDirective> providesIterator = moduleInfo.getProvides().listIterator();
			while(providesIterator.hasNext()) {
				if(overriddenProvidesByName.containsKey(providesIterator.next().getType())) {
					providesIterator.remove();
				}
			}
			moduleInfo.getProvides().addAll(moduleInfoOverride.getProvides().stream().filter(directive -> !directive.isRemove()).collect(Collectors.toSet()));
		}
		catch(IOException | ParseException e) {
			throw new TaskExecutionException("Error overriding module-info.java for " + dependency + "", e);
		}
		
		try {
			Files.write(dependency.getModuleInfoPath(), moduleInfo.toString().getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} 
		catch(IOException e) {
			throw new TaskExecutionException("Error overriding module-info.java for " + dependency + "", e);
		}
	}
	
	/**
	 * <p>
	 * Compiles the generated or user-provided module descriptors.
	 * </p>
	 * 
	 * @param project the build project
	 */
	private void compileModularizedDependencies(BuildProject project) {
		LOGGER.info("[ Compiling modularized project dependencies... ]");

		String javac_modulePath = project.getDependencies().stream()
			.filter(dependency -> !dependency.isAutomatic())
			.map(d -> d.getJarPath().toString())
			.collect(Collectors.joining(System.getProperty("path.separator")));

		List<String> javac_args = new ArrayList<>();

		javac_args.add("-verbose");
		//javac_args.add("-nowarn");
		javac_args.add("--module-source-path");
		javac_args.add(project.getModulesExplodedPath().toString());
		javac_args.add("-d");
		javac_args.add(project.getModulesExplodedPath().toString());
		javac_args.add("--module-path");
		javac_args.add(javac_modulePath);

		int size = javac_args.size();
		javac_args.addAll(project.getDependencies().stream()
			.filter(dependency -> dependency.isAutomatic() && dependency.isMarked())
			.map(d -> d.getModuleInfoPath().toString())
			.collect(Collectors.toList())
		);
		if(javac_args.size() == size) {
			// Nothing to compile
			LOGGER.info("Nothing to compile");
		}
		else {
			LOGGER.info(" - javac {}", javac_args.stream().collect(Collectors.joining(" ")));
			if(JavaTools.JAVAC.run(OUT, ERR, javac_args.stream().toArray(String[]::new)) != 0) {
				throw new TaskExecutionException("Error compiling generated module descriptors");
			}
		}
	}
	
	/**
	 * <p>
	 * Packages the dependencies that were modularized in JAR archives and put them in the working modules path.
	 * </p>
	 * 
	 * @param project the build project
	 * 
	 * @throws TaskExecutionException if there was an error packaging the modularized dependencies
	 */
	private void packageModularizedDependencies(BuildProject project) throws TaskExecutionException {
		LOGGER.info("[ Packaging modularized project dependencies... ]");
		for(BuildDependency dependency : project.getDependencies()) {
			if(dependency.isMarked() && dependency.isAutomatic()) {
				LOGGER.info(" - ", dependency);
				this.packageDependency(dependency);
			}
		}
	}
	
	/**
	 * <p>
	 * Packages a single dependency in a JAR archive and put it in the working modules path.
	 * </p>
	 * 
	 * @param dependency the build dependency
	 * 
	 * @throws TaskExecutionException if there was an error packaging the dependency
	 */
	private void packageDependency(BuildDependency dependency) throws TaskExecutionException {
		String[] jar_args = {
			"--create",
			"--no-manifest",
			"--file", dependency.getModulePath().toString(),
			"--module-version", dependency.getModuleVersion(),
			"-C", dependency.getExplodedModulePath().toString(),
			"."
		};
		
		LOGGER.info("   - jar {}", Arrays.stream(jar_args).collect(Collectors.joining(" ")));
		
		if(JavaTools.JAR.run(OUT, ERR, jar_args) != 0) {
			throw new TaskExecutionException("Error packaging dependency " + dependency);
		}
		
		try(FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + dependency.getModulePath().toUri()), Map.of())) {
			Files.deleteIfExists(jarFs.getPath("module-info.java"));
		}
		catch (IOException e) {
			throw new TaskExecutionException("Error packaging dependency " + dependency, e);
		}
	}
	
	@Override
	public StartTask start() {
		return new GenericStartTask(this);
	}

	@Override
	public RunTask run() {
		return new GenericRunTask(this);
	}
	
	@Override
	public DebugTask debug() {
		return new GenericDebugTask(this);
	}

	@Override
	public BuildJmodTask buildJmod() {
		return new GenericBuildJmodTask(this);
	}
}
