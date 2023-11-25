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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * <p>
 * Wraps a {@link Dependency} provided in the {@link Project} for processing within {@link Task} implementations.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class BuildDependency implements Dependency {

	private final BuildProject project;
	private final Dependency dependency;
	private final ModuleDescriptor moduleDescriptor;
	private final boolean named;
	private final boolean marked;
	
	/**
	 * <p>
	 * Creates a build dependency.
	 * </p>
	 * 
	 * @param project          the build project
	 * @param dependency       the original project dependency
	 * @param moduleDescriptor the dependency module descriptor
	 */
	public BuildDependency(BuildProject project, Dependency dependency, ModuleDescriptor moduleDescriptor) {
		this.dependency = dependency;
		this.project = project;
		this.moduleDescriptor = moduleDescriptor;
		
		try(JarFile jarFile = new JarFile(this.dependency.getJarPath().toFile(), true, ZipFile.OPEN_READ, Runtime.version())) {
			// if the module is not "named" the name should be groupId.artifactId...
			this.named = jarFile.getEntry("module-info.class") != null || (jarFile.getManifest() != null && jarFile.getManifest().getMainAttributes().containsKey(new Attributes.Name("Automatic-Module-Name")));
			
			Path jmodPath = this.getModulePath();
			this.marked = !Files.exists(jmodPath) || Files.getLastModifiedTime(this.dependency.getJarPath()).compareTo(Files.getLastModifiedTime(jmodPath)) > 0;
		}
		catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/**
	 * <p>
	 * Returns the wrapped project dependency.
	 * </p>
	 * 
	 * @return the original project dependency
	 */
	public Dependency unwrap() {
		return this.dependency;
	}
	
	@Override
	public String getGroup() {
		return this.dependency.getGroup();
	}

	@Override
	public String getName() {
		return this.dependency.getName();
	}

	@Override
	public String getVersion() {
		return this.dependency.getVersion();
	}
	
	@Override
	public Path getJarPath() {
		return this.dependency.getJarPath();
	}
	
	/**
	 * <p>
	 * Returns the module descriptor.
	 * </p>
	 * 
	 * @return the module descriptor
	 */
	public ModuleDescriptor getModuleDescriptor() {
		return this.moduleDescriptor;
	}
	
	/**
	 * <p>
	 * Returns the dependency module name.
	 * </p>
	 * 
	 * @return the module name
	 */
	public String getModuleName() {
		if(this.named) {
			return this.getModuleDescriptor().name();
		}
		else {
			return this.getGroup() + "." + this.getModuleDescriptor().name();
		}
	}
	
	/**
	 * <p>
	 * Returns the dependency module version.
	 * </p>
	 * 
	 * @return the module version
	 */
	public String getModuleVersion() {
		return this.getModuleDescriptor().version().map(ModuleDescriptor.Version::toString).orElse(this.getVersion());
	}
	
	/**
	 * <p>
	 * Returns the path to the dependency modular JAR archive.
	 * </p>
	 * 
	 * @return the path to the modular JAR archive
	 */
	public final Path getModulePath() {
		return this.project.getModulesPath().resolve(this.getModuleName() + "-" + this.getModuleVersion() + ".jar");
	}
	
	/**
	 * <p>
	 * Determines whether the dependency is an automatic module.
	 * </p>
	 * 
	 * @return true if the dependency is an automatic module, false otherwise
	 */
	public boolean isAutomatic() {
		return this.getModuleDescriptor().isAutomatic();
	}
	
	/**
	 * <p>
	 * Determines whether the dependency is a named module.
	 * </p>
	 * 
	 * @return true if the dependency is a named module, false otherwise
	 */
	public boolean isNamed() {
		return this.named;
	}
	
	/**
	 * <p>
	 * Determines whether the dependency has changed since last build.
	 * </p>
	 * 
	 * @return true if the dependency changed, false otherwise
	 */
	public boolean isMarked() {
		return this.marked;
	}

	/**
	 * <p>
	 * Returns the path of the exploded dependency module folder.
	 * </p>
	 * 
	 * @return the path to the exploded module
	 */
	public final Path getExplodedModulePath() {
		return this.project.getModulesExplodedPath().resolve(this.getModuleName());
	}
	
	/**
	 * <p>
	 * Returns the path of the unnamed dependency module.
	 * </p>
	 * 
	 * @return the path to the unnamed module
	 */
	public final Path getUnnamedModulePath() {
		return this.project.getModulesUnnamedPath().resolve(this.getModuleName() + "-" + this.getModuleVersion() + ".jar");
	}
	
	/**
	 * <p>
	 * Returns the path to the {@code module-info.java} in the exploded module folder.
	 * </p>
	 * 
	 * @return the path to the {@code module-info.java}
	 */
	public final Path getModuleInfoPath() {
		return this.getExplodedModulePath().resolve("module-info.java");
	}

	@Override
	public String toString() {
		return this.getModuleName() + "@" + this.getModuleVersion();
	}
}
