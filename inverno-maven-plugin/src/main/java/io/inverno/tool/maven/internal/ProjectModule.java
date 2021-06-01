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
package io.inverno.tool.maven.internal;

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
 * <p>
 * A project module contains metadata of the project module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class ProjectModule implements ImageModule {
	
	/**
	 * <p>
	 * Represents a project module classifier used to build a particular project
	 * image.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum Classifier {
		/**
		 * Indicates a runtime classifier. 
		 */
		RUNTIME("runtime"),
		/**
		 * Indicates an application classifier. 
		 */
		APPLICATION("application"),
		/**
		 * Indicates a container classifier. 
		 */
		CONTAINER("container");

		private final String classifier;
		
		private Classifier(String classifier) {
			this.classifier = classifier + "_" + Platform.getSystemPlatform();
		}
		
		/**
		 * <p>
		 * Returns the classifier name.
		 * </p>
		 * 
		 * @return The classifier name
		 */
		public String getClassifier() {
			return classifier;
		}
		
		/**
		 * <p>
		 * Returns the operating system.
		 * </p>
		 * 
		 * @return the operating system
		 */
		public String getOs() {
			return Platform.getSystemPlatform().getOs();
		}
		
		/**
		 * <p>
		 * Returns the system architecture.
		 * </p>
		 * 
		 * @return the system architecture
		 */
		public String getArch() {
			return Platform.getSystemPlatform().getArch();
		}
	}
	
	private final MavenProject project;
	private final ModuleDescriptor moduleDescriptor;
	private final Set<DependencyModule> moduleDependencies;
	private final Path invernoBuildPath;
	private final Path jmodsPath;
	private final Classifier classifier;
	private final Map<String, Path> moduleArchivesPaths;
	
	private Set<String> mainClasses;
	private Optional<String> defaultMainClass;
	
	private boolean marked;
	
	/**
	 * <p>
	 * Creates a project module.
	 * </p>
	 * 
	 * @param project            the Maven project
	 * @param moduleDescriptor   the module descriptor
	 * @param moduleDependencies the module dependency modules
	 * @param invernoBuildPath   the inverno plugin build path
	 * @param jmodsPath          the path to modular and modularized modules
	 * @param classifier         the project module classifier
	 * @param formats            the formats of the project module archives to
	 *                           build
	 */
	public ProjectModule(MavenProject project, ModuleDescriptor moduleDescriptor, Set<DependencyModule> moduleDependencies, Path invernoBuildPath, Path jmodsPath, Classifier classifier, Set<String> formats) {
		this.project = project;
		this.moduleDescriptor = moduleDescriptor;
		this.moduleDependencies = moduleDependencies;
		this.invernoBuildPath = invernoBuildPath;
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
	
	/**
	 * <p>
	 * Returns the path to the project module pidfile generated when running the
	 * application.
	 * </p>
	 * 
	 * @return the path to the pidfile
	 */
	public Path getPidfile() {
		return this.invernoBuildPath.resolve(this.getModuleName() + ".pid");
	}
	
	/**
	 * <p>
	 * Resolves the main classes defined in the project module.
	 * </p>
	 * 
	 * @return a set of classes or an empty set if the module doesn't define any
	 *         main class.
	 * 
	 * @throws IOException            if there was an error reading a module's class
	 * @throws ClassNotFoundException if a class was not found when trying to load a
	 *                                module's class
	 */
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
	
	/**
	 * <p>
	 * Returns the default main class of the project module.
	 * </p>
	 * 
	 * <p>
	 * If the project module defines multiple main classes, the first one is
	 * returned.
	 * </p>
	 * 
	 * @return an optional returning a main class or an empty optional if the
	 *         doesn't define any main class.
	 * 
	 * @throws IOException            if there was an error reading a module's class
	 * @throws ClassNotFoundException if a class was not found when trying to load a
	 *                                module's class
	 */
	public Optional<String> getDefaultMainClass() throws ClassNotFoundException, IOException {
		if(this.defaultMainClass == null) {
			this.defaultMainClass = Optional.of(this.getMainClasses()).filter(classes -> !classes.isEmpty()).map(classes -> classes.iterator().next());
		}
		return this.defaultMainClass;
	}
	
	/**
	 * <p>
	 * Returns the path to the classes directory which contains project module
	 * {@code .class} files.
	 * </p>
	 * 
	 * @return the path to the project module classes
	 */
	public Path getClassesPath() {
		return Paths.get(this.project.getBuild().getOutputDirectory()).toAbsolutePath();
	}
	
	/**
	 * <p>
	 * Returns the path to the project module runtime image.
	 * </p>
	 * 
	 * @return the path to the runtime image
	 */
	public Path getRuntimeImagePath() {
		return this.invernoBuildPath.resolve(Paths.get(Classifier.RUNTIME.getClassifier(), this.project.getBuild().getFinalName()));
	}
	
	/**
	 * <p>
	 * Returns the path to the project module application image.
	 * </p>
	 * 
	 * @return the path to the application image
	 */
	public Path getApplicationImagePath() {
		return this.invernoBuildPath.resolve(Paths.get(Classifier.APPLICATION.getClassifier(), this.project.getBuild().getFinalName()));
	}
	
	/**
	 * <p>
	 * Returns the path to the project module container image tar archive.
	 * </p>
	 * 
	 * @return the path to the container image tar archive.
	 */
	public Path getContainerImageTarPath() {
		return Paths.get(this.project.getBuild().getDirectory()).toAbsolutePath().resolve(this.project.getBuild().getFinalName() + "-" + this.classifier.getClassifier() + ".tar");
	}
	
	/**
	 * <p>
	 * Returns the paths to the image archives to build.
	 * </p>
	 * 
	 * @return a map with an archive format as key and the corresponding archive
	 *         path as value
	 */
	public Map<String, Path> getImageArchivesPaths() {
		return this.moduleArchivesPaths;
	}
	
	/**
	 * <p>
	 * Returns project module dependencies.
	 * </p>
	 * 
	 * @return a set of dependency modules
	 */
	public Set<DependencyModule> getModuleDependencies() {
		return moduleDependencies;
	}
	
	/**
	 * <p>
	 * Returns the classifier of the project module.
	 * </p>
	 * 
	 * @return the project module classifier
	 */
	public Classifier getClassifier() {
		return classifier;
	}
	
	@Override
	public String toString() {
		return this.getModuleName() + "@" + this.getModuleVersion();
	}
}
