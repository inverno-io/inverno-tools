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
package io.winterframework.tools.maven.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * @author jkuhn
 *
 */
public class ProjectModule implements ImageModule {
	
	public static enum Classifier {
		RUNTIME_IMAGE("runtime"),
		PACKAGE("package");

		private final String classifier;
		
		private Classifier(String classifier) {
			this.classifier = classifier + "_" + System.getProperty("os.arch");
		}
		
		public String getClassifier() {
			return classifier;
		}
	}
	
	private final MavenProject project;
	private final ModuleDescriptor moduleDescriptor;
	private final Set<DependencyModule> moduleDependencies;
	private final Path workingPath;
	private final Path jmodsPath;
	private final Classifier classifier;
	private final Map<String, Path> moduleArchivesPaths;
	
	private Set<String> mainClasses;
	private Optional<String> defaultMainClass;
	
	private boolean marked;
	
	public ProjectModule(MavenProject project, ModuleDescriptor moduleDescriptor, Set<DependencyModule> moduleDependencies, Path workingPath, Path jmodsPath, Classifier classifier, Set<String> formats) {
		this.project = project;
		this.moduleDescriptor = moduleDescriptor;
		this.moduleDependencies = moduleDependencies;
		this.workingPath = workingPath;
		this.jmodsPath = jmodsPath;
		this.classifier = classifier;
		
		this.moduleArchivesPaths = formats.stream().collect(Collectors.toMap(Function.identity(), format -> Paths.get(this.project.getBuild().getDirectory()).toAbsolutePath().resolve(this.project.getBuild().getFinalName() + "-" + this.classifier.getClassifier() + "." + format)));
		
		Path jmodPath = this.getJmodPath();
		if(Files.exists(jmodPath)) {
			Path classesDirectory = Paths.get(this.project.getBuild().getOutputDirectory());
			try(Stream<Path> walk = Files.walk(classesDirectory)) {
				FileTime jmodLastModified = Files.getLastModifiedTime(jmodPath);
				for(Iterator<Path> pathIterator = walk.iterator(); pathIterator.hasNext();) {
					if(Files.getLastModifiedTime(pathIterator.next()).compareTo(jmodLastModified) > 0) {
						this.marked = true;
					}
				}
			} 
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		else {
			this.marked = true;
		}
	}
	
	public Set<String> getMainClasses() throws IOException, ClassNotFoundException {
		if(this.mainClasses == null) {
			this.mainClasses = new HashSet<>();
			Path moduleClassesPath = Paths.get(this.project.getBuild().getOutputDirectory());
			
			Set<URL> urls = new HashSet<>();
			for(DependencyModule d : this.moduleDependencies) {
				urls.add(d.getSourcePath().toUri().toURL());
			}
			urls.add(moduleClassesPath.toUri().toURL());
			try(Stream<Path> walk = Files.walk(moduleClassesPath);URLClassLoader classLoader = new URLClassLoader(urls.stream().toArray(URL[]::new));) {
				Iterator<Path> pathsIterator = walk.filter(path -> {
					String fileName = path.getFileName().toString();
					return fileName.endsWith(".class") && !fileName.equals("module-info.class");
				}).iterator();
				
				while(pathsIterator.hasNext()) {
					String className = moduleClassesPath.relativize(pathsIterator.next()).toString();
					className = className.replace('/', '.').substring(0, className.length() - 6);
					Class<?> cl = classLoader.loadClass(className);
					try {
						Method m = cl.getMethod("main", String[].class);
						if(m.getReturnType().equals(void.class)) {
							this.mainClasses.add(className);
						}
					} 
					catch (NoSuchMethodException | NoClassDefFoundError e) {
						// an error can occur if the method doesn't exist OR if there's a return type
						// that could not be found (NoClassDefFoundError)
						continue;
					}
				}
			}
		}
		return this.mainClasses;
	}
	
	public Optional<String> getDefaultMainClass() throws ClassNotFoundException, IOException {
		if(this.defaultMainClass == null) {
			this.defaultMainClass = Optional.of(this.getMainClasses()).filter(classes -> !classes.isEmpty()).map(classes -> classes.iterator().next());
		}
		return this.defaultMainClass;
	}
	
	@Override
	public Artifact getArtifact() {
		return this.project.getArtifact();
	}

	@Override
	public ModuleDescriptor getModuleDescriptor() {
		return this.moduleDescriptor;
	}

	@Override
	public String getModuleName() {
		return this.moduleDescriptor.name();
	}

	@Override
	public String getModuleVersion() {
		return this.project.getVersion();
	}

	@Override
	public Path getJmodPath() {
		return this.jmodsPath.resolve(this.getModuleName() + "-" + this.getModuleVersion() + ".jmod");
	}

	@Override
	public boolean isMarked() {
		return this.marked;
	}
	
	public Path getClassesPath() {
		return Paths.get(this.project.getBuild().getOutputDirectory()).toAbsolutePath();
	}
	
	public Path getRuntimeImagePath() {
		return this.workingPath.resolve(Paths.get(Classifier.RUNTIME_IMAGE.getClassifier(), this.project.getBuild().getFinalName()));
	}
	
	public Path getPackageImagePath() {
		return this.workingPath.resolve(Paths.get(Classifier.PACKAGE.getClassifier(), this.project.getBuild().getFinalName()));
	}
	
	public Map<String, Path> getImageArchivesPaths() {
		return this.moduleArchivesPaths;
	}
	
	public Set<DependencyModule> getModuleDependencies() {
		return moduleDependencies;
	}
	
	public Classifier getClassifier() {
		return classifier;
	}
	
	@Override
	public String toString() {
		return this.getModuleName() + "@" + this.getModuleVersion();
	}
}
