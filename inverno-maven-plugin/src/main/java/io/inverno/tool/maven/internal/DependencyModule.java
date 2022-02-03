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

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;

/**
 * <p>
 * A dependency module contains metadata of a project dependency module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class DependencyModule implements ImageModule {

	private final Artifact artifact;
	private final ModuleReference moduleReference;
	private final Optional<Path> jmodsOverridePath;
	private final Path jmodsExplodedPath;
	private final Path jmodsUnnamedPath;
	private final Path jmodsPath;
	private final boolean marked;
	
	private boolean named;
	
	/**
	 * <p>
	 * Creates a dependency module.
	 * </p>
	 * 
	 * @param artifact          the Maven artifact
	 * @param moduleReference   the module reference
	 * @param jmodsOverridePath the path to user-defined module descriptors
	 * @param jmodsExplodedPath the path to exploded modules
	 * @param jmodsUnnamedPath  the path to unnamed modules
	 * @param jmodsPath         the path to modular and modularized modules
	 * @param overWriteIfNewer  true to mark the dependency module if it's older
	 *                          than the source
	 * 
	 * @throws IOException if an I/O error occurs while analyzing the dependency JAR
	 */
	public DependencyModule(Artifact artifact, ModuleReference moduleReference, Optional<Path> jmodsOverridePath, Path jmodsExplodedPath, Path jmodsUnnamedPath, Path jmodsPath, boolean overWriteIfNewer) throws IOException {
		this.artifact = artifact;
		this.moduleReference = moduleReference;
		this.jmodsOverridePath = jmodsOverridePath;
		this.jmodsExplodedPath = jmodsExplodedPath;
		this.jmodsUnnamedPath = jmodsUnnamedPath;
		this.jmodsPath = jmodsPath;
		
		// if the module is not "named" the name should be groupId.artifactId...
		// => we have to copy modules... NO
		try(JarFile jarFile = new JarFile(new File(moduleReference.location().get()), true, ZipFile.OPEN_READ, Runtime.version())) {
			this.named = jarFile.getEntry("module-info.class") != null || (jarFile.getManifest() != null && jarFile.getManifest().getMainAttributes().containsKey(new Attributes.Name("Automatic-Module-Name")));
		}

		Path jmodPath = this.getJmodPath();
		this.marked = !Files.exists(jmodPath) || (overWriteIfNewer && Files.getLastModifiedTime(artifact.getFile().toPath()).compareTo(Files.getLastModifiedTime(jmodPath)) > 0);
	}

	@Override
	public Artifact getArtifact() {
		return artifact;
	}
	
	@Override
	public ModuleDescriptor getModuleDescriptor() {
		return moduleReference.descriptor();
	}
	
	@Override
	public String getModuleName() {
		// What if we have the same dependency with different classifier (linux-x86_64, linux-aarch_64...)?
		// This is not supported: when creating an image, it has to be created FOR a
		// specific architecture, classifiers are not supported in JDK so we can't
		// include a dependency with different qualifiers
		if(this.named) {
			return this.moduleReference.descriptor().name();			
		}
		else {
			return this.artifact.getGroupId() + "." + this.moduleReference.descriptor().name();
		}
	}
	
	@Override
	public String getModuleVersion() {
		return this.moduleReference.descriptor().version().map(Version::toString).orElse(this.artifact.getVersion());
	}

	@Override
	public Path getJmodPath() {
		return this.jmodsPath.resolve(this.getModuleName() + "-" + this.getModuleVersion() + ".jar");
	}
	
	@Override
	public boolean isMarked() {
		return marked;
	}

	/**
	 * <p>
	 * Determines whether this dependency is an automatic module.
	 * </p>
	 * 
	 * @return true if the dependency is an automatic module, false otherwise
	 */
	public boolean isAutomatic() {
		return this.moduleReference.descriptor().isAutomatic();
	}
	
	/**
	 * <p>
	 * Determines whether this dependency is a named module.
	 * </p>
	 * 
	 * @return true if the dependency is a named module, false otherwise
	 */
	public boolean isNamed() {
		return this.named;
	}
	
	/**
	 * <p>
	 * Returns the path to the dependency source JAR.
	 * </p>
	 * 
	 * @return the path to the source JAR
	 */
	public Path getSourcePath() {
		return this.artifact.getFile().toPath();
	}
	
	/**
	 * <p>
	 * Returns the path to the unnamed source JAR.
	 * </p>
	 * 
	 * <p>
	 * This jar file is a copy of the source path whose manifest has been updated
	 * with an Automatic-Module-Name field.
	 * </p>
	 * 
	 * @return the path to the unnamed JAR
	 */
	public Path getUnnamedPath() {
		return this.jmodsUnnamedPath.resolve(this.getModuleName() + "-" + this.getModuleVersion() + ".jar");
	}
	
	/**
	 * <p>
	 * Returns the path to the exploded module used to modularize a non-modular
	 * dependency.
	 * </p>
	 * 
	 * @return the path to the exploded module
	 */
	public Path getExplodedJmodPath() {
		return this.jmodsExplodedPath.resolve(this.getModuleName());
	}
	
	/**
	 * <p>
	 * Returns the path to the user-defined module descriptor to use instead of the
	 * generated one when modularizing a non-modular dependency.
	 * </p>
	 * 
	 * @return the path to the module overrides
	 */
	public Optional<Path> getOverriddenModuleInfoPath() {
		return this.jmodsOverridePath.map(path -> path.resolve(Paths.get(this.getModuleName(), "module-info.java"))).filter(Files::exists);
	}
	
	/**
	 * <p>
	 * Returns the path to the dependency module descriptor.
	 * </p>
	 * 
	 * <p>
	 * the dependency module descriptor is either generated in the exploded JMOD
	 * path or copied from the user-defined module descriptor path.
	 * </p>
	 * 
	 * @return the path to the module descriptor in the exploded path
	 */
	public Path getModuleInfoPath() {
		return this.jmodsExplodedPath.resolve(Paths.get(this.getModuleName(), "module-info.java"));
	}
	
	@Override
	public String toString() {
		return this.moduleReference.descriptor().toNameAndVersion();
	}
}
