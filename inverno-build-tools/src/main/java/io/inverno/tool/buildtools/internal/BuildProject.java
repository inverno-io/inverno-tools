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

import io.inverno.tool.buildtools.Dependency;
import io.inverno.tool.buildtools.Project;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Wraps the {@link Project} for processing within {@link Task} implementations.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class BuildProject extends Project {

	private final Project project;
	
	private ModuleDescriptor moduleDescriptor;
	private Set<String> mainClasses;
	private Optional<String> defaultMainClass;
	private Set<BuildDependency> buildDependencies;

	/**
	 * <p>
	 * Creates a build project.
	 * </p>
	 * 
	 * @param project the original project
	 */
	public BuildProject(Project project) {
		super(project.getTargetPath(), project.getWorkingPath());
		this.project = project;
	}
	
	/**
	 * <p>
	 * Returns the wrapped project.
	 * </p>
	 * 
	 * @return the original project
	 */
	public Project unwrap() {
		return this.project;
	}
	
	@Override
	public String getGroup() {
		return this.project.getGroup();
	}

	@Override
	public String getName() {
		return this.project.getName();
	}

	@Override
	public String getVersion() {
		return this.project.getVersion();
	}
	
	@Override
	public Path getClassesPath() {
		return this.project.getClassesPath();
	}

	@Override
	public Set<BuildDependency> getDependencies() {
		if(this.buildDependencies == null) {
			Set<? extends Dependency> dependencies = this.project.getDependencies();
			ModuleFinder moduleFinder = ModuleFinder.of(dependencies.stream().map(Dependency::getJarPath).toArray(Path[]::new));
			Map<Path, ModuleDescriptor> modulesByDependencyJarPath = moduleFinder.findAll().stream().collect(Collectors.toMap(moduleRef -> Path.of(moduleRef.location().get()), ModuleReference::descriptor));
			
			this.buildDependencies = dependencies.stream()
				.map(dependency -> new BuildDependency(this, dependency, modulesByDependencyJarPath.get(dependency.getJarPath())))
				.collect(Collectors.toSet());
		}
		return this.buildDependencies;
	}
	
	@Override
	public Path getWorkingPath() {
		return this.project.getWorkingPath();
	}

	@Override
	public Path getTargetPath() {
		return this.project.getTargetPath();
	}
	
	@Override
	public String getFinalName() {
		return this.project.getFinalName();
	}
	
	/**
	 * <p>
	 * Returns the module descriptor.
	 * </p>
	 * 
	 * @return the module descriptor
	 */
	public ModuleDescriptor getModuleDescriptor() {
		if(this.moduleDescriptor == null) {
			this.moduleDescriptor = ModuleFinder.of(this.getClassesPath()).findAll().stream().findFirst().get().descriptor();
		}
		return this.moduleDescriptor;
	}
	
	/**
	 * <p>
	 * Returns the module name.
	 * </p>
	 * 
	 * @return the module name
	 */
	public String getModuleName() {
		return this.getModuleDescriptor().name();
	}
	
	/**
	 * <p>
	 * Returns the module version.
	 * </p>
	 * 
	 * @return the module version
	 */
	public String getModuleVersion() {
		return this.getModuleDescriptor().version().map(ModuleDescriptor.Version::toString).orElse(this.getVersion());
	}
	
	/**
	 * <p>
	 * Returns the path to the module JMOD archive.
	 * </p>
	 * 
	 * @return the path to the JMOD archive
	 */
	public final Path getModulePath() {
		return this.getModulesPath().resolve(this.getModuleName() + "-" + this.getModuleVersion() + ".jmod");
	}
	
	/**
	 * <p>
	 * Determines whether the project has changed since last build.
	 * </p>
	 * 
	 * @return true if the project changed, false otherwise
	 */
	public boolean isMarked() {
		Path jmodPath = this.getModulePath();
		if(!Files.exists(jmodPath)) {
			return true;
		}
		Path classesDirectory = this.getClassesPath();
		try(Stream<Path> walk = Files.walk(classesDirectory)) {
			FileTime jmodLastModified = Files.getLastModifiedTime(jmodPath);
			for(Iterator<Path> pathIterator = walk.iterator(); pathIterator.hasNext();) {
				if(Files.getLastModifiedTime(pathIterator.next()).compareTo(jmodLastModified) > 0) {
					return true;
				}
			}
		} 
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return false;
	}
	
	/**
	 * <p>
	 * Returns the path to project modules.
	 * </p>
	 * 
	 * @return the path to project modules
	 */
	public final Path getModulesPath() {
		return this.getWorkingPath().resolve("modules").toAbsolutePath();
	}
	
	/**
	 * <p>
	 * Returns the path to project exploded modules.
	 * </p>
	 * 
	 * @return the path to project exploded modules
	 */
	public final Path getModulesExplodedPath() {
		return this.getWorkingPath().resolve("modules-exploded").toAbsolutePath();
	}
	
	/**
	 * <p>
	 * Returns the path to project unnamed modules.
	 * </p>
	 * 
	 * @return the path to project unnamed modules
	 */
	public final Path getModulesUnnamedPath() {
		return this.getWorkingPath().resolve("modules-unnamed").toAbsolutePath();
	}
	
	/**
	 * <p>
	 * Returns path to the specified project image
	 * </p>
	 * 
	 * @param imageType the type of the image
	 * 
	 * @return the path to the project image.
	 */
	public final Path getImagePath(ImageType imageType) {
		switch(imageType) {
			case RUNTIME:
			case APPLICATION: return this.getTargetPath().resolve(this.getFinalName() + "-" + imageType.getNativeQualifier());
			case CONTAINER: return this.getTargetPath().resolve(this.getFinalName() + "-" + ImageType.CONTAINER.getNativeQualifier() + ".tar");
			default: throw new IllegalStateException("Unsupported image type: " + imageType);
		}
	}
	
	public final Map<String, Path> getImageArchivesPaths(ImageType imageType, Set<String> formats) {
		return formats != null ? formats.stream()
			.collect(Collectors.toMap(Function.identity(), 
				format -> this.getTargetPath().resolve(this.getFinalName() + "-" + imageType.getNativeQualifier() + "." + format)
			)) : Map.of();
	}
	
	public final Path getLaunchersPath() {
		return this.getWorkingPath().resolve("launchers").toAbsolutePath();
	}
	
	/**
	 * <p>
	 * Resolves the main classes defined in the project module.
	 * </p>
	 *
	 * @return a set of classes or an empty set if the module doesn't define any main class.
	 *
	 * @throws IOException            if there was an error reading a module classes
	 * @throws ClassNotFoundException if a class was not found when trying to load a module classes
	 */
	public Set<String> getMainClasses() throws IOException, ClassNotFoundException {
		if(this.mainClasses == null) {
			this.mainClasses = new HashSet<>();
			Path moduleClassesPath = this.getClassesPath();
			
			Set<URL> urls = new HashSet<>();
			for(Dependency d : this.getDependencies()) {
				urls.add(d.getJarPath().toUri().toURL());
			}
			urls.add(moduleClassesPath.toUri().toURL());
			try(Stream<Path> walk = Files.walk(moduleClassesPath);URLClassLoader classLoader = new URLClassLoader(urls.stream().toArray(URL[]::new));) {
				Iterator<Path> pathsIterator = walk.filter(path -> {
					String fileName = path.getFileName().toString();
					return fileName.endsWith(".class") && !fileName.equals("module-info.class");
				}).iterator();
				
				while(pathsIterator.hasNext()) {
					String className = moduleClassesPath.relativize(pathsIterator.next()).toString();
					className = className.replace(java.io.File.separatorChar, '.').substring(0, className.length() - 6);
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
	 * If the project module defines multiple main classes, the first one is returned.
	 * </p>
	 *
	 * @return an optional returning a main class or an empty optional if the doesn't define any main class.
	 *
	 * @throws IOException            if there was an error reading a module classes
	 * @throws ClassNotFoundException if a class was not found when trying to load a module classes
	 */
	public Optional<String> getDefaultMainClass() throws ClassNotFoundException, IOException {
		if(this.defaultMainClass == null) {
			this.defaultMainClass = Optional.of(this.getMainClasses()).filter(classes -> !classes.isEmpty()).map(classes -> classes.iterator().next());
		}
		return this.defaultMainClass;
	}
	
	/**
	 * <p>
	 * Sets the default main class explicitly, typically when generating project JMOD with a main class.
	 * </p>
	 * 
	 * @param defaultMainClass the default main class within the project module
	 * 
	 * @throws ClassNotFoundException   if a class was not found when trying to load a module classes
	 * @throws IOException              if there was an error reading a module classes
	 * @throws IllegalArgumentException if the specified class is defined in the project module
	 */
	void setDefaultMainClass(String defaultMainClass) throws ClassNotFoundException, IOException, IllegalArgumentException {
		if(!this.getMainClasses().contains(defaultMainClass)) {
			throw new IllegalArgumentException(defaultMainClass + " is not defined in project module " + this.getModuleName());
		}
		this.defaultMainClass = Optional.of(defaultMainClass);
	}
	
	/**
	 * <p>
	 * Returns the path to the project module pidfile generated when running the application.
	 * </p>
	 *
	 * @return the path to the pidfile
	 */
	public Path getPidfile() {
		return this.getWorkingPath().resolve(this.getName()+ ".pid");
	}

	@Override
	public String toString() {
		return this.getModuleName() + "@" + this.getModuleVersion();
	}
}
