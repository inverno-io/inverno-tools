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
 * @author jkuhn
 *
 */
public class DependencyModule implements ImageModule {

	private final Artifact artifact;
	private final ModuleReference moduleReference;
	private final Optional<Path> projectJModsOverridePath;
	private final Path jmodsExplodedPath;
	private final Path jmodsPath;
	private final boolean marked;
	
	private boolean named;
	
	public DependencyModule(Artifact artifact, ModuleReference moduleReference, Optional<Path> projectJModsOverridePath, Path jmodsExplodedPath, Path jmodsPath, boolean overWriteIfNewer) throws IOException {
		this.artifact = artifact;
		this.moduleReference = moduleReference;
		this.projectJModsOverridePath = projectJModsOverridePath;
		this.jmodsExplodedPath = jmodsExplodedPath;
		this.jmodsPath = jmodsPath;
		
		// if the module is not "named" the name should be groupId.artifactId...
		// => we have to copy modules... NO
		try(JarFile jarFile = new JarFile(new File(moduleReference.location().get()), true, ZipFile.OPEN_READ, Runtime.version())) {
			this.named = jarFile.getEntry("module-info.class") != null || jarFile.getManifest().getMainAttributes().containsKey(new Attributes.Name("Automatic-Module-Name"));
		}

		Path jmodPath = this.getJmodPath();
		this.marked = !Files.exists(jmodPath) || (overWriteIfNewer && Files.getLastModifiedTime(artifact.getFile().toPath()).compareTo(Files.getLastModifiedTime(jmodPath)) > 0);
	}

	public Artifact getArtifact() {
		return artifact;
	}
	
	public ModuleDescriptor getModuleDescriptor() {
		return moduleReference.descriptor();
	}
	
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
	
	public String getModuleVersion() {
		return this.moduleReference.descriptor().version().map(Version::toString).orElse(this.artifact.getVersion());
	}

	public Path getJmodPath() {
		return this.jmodsPath.resolve(this.getModuleName() + "-" + this.getModuleVersion() + ".jar");
	}
	
	public boolean isMarked() {
		return marked;
	}
	
	public Path getSourcePath() {
		return this.artifact.getFile().toPath();
	}
	
	public Path getExplodedJmodPath() {
		return this.jmodsExplodedPath.resolve(this.getModuleName());
	}
	
	public Optional<Path> getOverriddenModuleInfoPath() {
		return this.projectJModsOverridePath.map(path -> path.resolve(Paths.get(this.getModuleName(), "module-info.java"))).filter(Files::exists);
	}
	
	public Path getModuleInfoPath() {
		return this.jmodsExplodedPath.resolve(Paths.get(this.getModuleName(), "module-info.java"));
	}
	
	@Override
	public String toString() {
		return this.moduleReference.descriptor().toNameAndVersion();
	}
}
