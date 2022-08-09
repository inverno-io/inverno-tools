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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import org.apache.maven.artifact.Artifact;

/**
 * <p>
 * Represents a module in a runtime image.
 * </p>
 * 
 * <p>
 * An image module may refer to the project module or a dependency module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see DependencyModule
 * @see ProjectModule
 */
public interface ImageModule {

	/**
	 * <p>
	 * Returns the Maven artifact defining the module.
	 * </p>
	 * 
	 * @return the Maven artifact
	 */
	Artifact getArtifact();
	
	/**
	 * <p>
	 * Returns the module descriptor.
	 * </p>
	 * 
	 * <p>
	 * Non-modular dependency modules are modularized, this descriptor describes a
	 * module before that process occurs.
	 * </p>
	 * 
	 * @return A module descriptor
	 */
	ModuleDescriptor getModuleDescriptor();
	
	/**
	 * <p>
	 * Returns the name of the module.
	 * </p>
	 * 
	 * @return the module name
	 */
	String getModuleName();
	
	/**
	 * <p>
	 * Returns the version of the module.
	 * </p>
	 * 
	 * @return the module version
	 */
	String getModuleVersion();
	
	/**
	 * <p>
	 * Returns the path to the module archive or the modularized archive if the
	 * module was initially non-modular.
	 * </p>
	 * 
	 * @return the path to the modular archive
	 */
	Path getJmodPath();
	
	/**
	 * <p>
	 * Determines whether this module should be processed by the plugin.
	 * </p>
	 * 
	 * <p>
	 * A non-marked module is considered up-to date and left untouched.
	 * </p>
	 * 
	 * @return true to process the module, false otherwise
	 */
	boolean isMarked();
}
