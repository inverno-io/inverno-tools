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
package io.inverno.tool.buildtools;

import io.inverno.tool.buildtools.internal.GenericBuildRuntimeTask;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * <p>
 * A task for building a custom Java runtime image for the project.
 * </p>
 * 
 * <p>
 * The resulting Java runtime image is an optimized assembly of the project resources (module and dependency modules) and required JDK's modules. The {@link PackageApplicationTask} requires a runtime 
 * image to create an optimized application package.
 * </p>
 * 
 * <p>
 * This task depends on {@link BuildJmodTask}.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface BuildRuntimeTask extends Task<Image, BuildRuntimeTask> {

	/**
	 * <p>
	 * Specifies the modules to add to the runtime image.
	 * </p>
	 * 
	 * @param addModules a comma-separated list of modules
	 * 
	 * @return the task
	 */
	BuildRuntimeTask addModules(String addModules);
	
	/**
	 * <p>
	 * Specifies the options to prepend before any other options when invoking the JVM of the runtime image.
	 * </p>
	 * 
	 * @param addOptions the options to pass to the runtime image JVM
	 * 
	 * @return the task
	 */
	BuildRuntimeTask addOptions(String addOptions);

	/**
	 * <p>
	 * Specifies the compress level of the runtime image: 0=No compression, 1=constant string sharing, 2=ZIP.
	 * </p>
	 * 
	 * @param compress the compress level
	 * 
	 * @return the task
	 */
	BuildRuntimeTask compress(String compress);
	
	/**
	 * <p>
	 * Specifies whether to link in service provider modules must and their dependencies.
	 * </p>
	 * 
	 * @param bindServices true to link in service provide modules and their depenencies, false otherwise
	 * 
	 * @return the task
	 */
	BuildRuntimeTask bindServices(boolean bindServices);

	/**
	 * <p>
	 * Specifies whether to suppress a fatal error when signed modular JARs are linked in the runtime image.
	 * </p>
	 * 
	 * @param ignoreSigningInformation true to suppress fatal error when linking signed modular JARs, false otherwise
	 * 
	 * @return the task
	 */
	BuildRuntimeTask ignoreSigningInformation(boolean ignoreSigningInformation);
	
	/**
	 * <p>
	 * Specifies whether to strip debug information from the runtime image.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code true}.
	 * </p>
	 * 
	 * @param stripDebug true to strip debug information, false otherwise
	 * 
	 * @return the task
	 */
	BuildRuntimeTask stripDebug(boolean stripDebug);
	
	/**
	 * <p>
	 * Specifies whether to strip native command (eg. java...) from the runtime image.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code true}.
	 * </p>
	 * 
	 * @param stripNativeCommands true to strip native command, false otherwise
	 * 
	 * @return the task
	 */
	BuildRuntimeTask stripNativeCommands(boolean stripNativeCommands);
	
	/**
	 * <p>
	 * Specifies the HotSpot VM in the runtime image defined as: {@code "client" / "server" / "minimal" / "all"}
	 * </p>
	 * 
	 * @param vm the HotSpot VM in the runtime image
	 * 
	 * @return the task
	 */
	BuildRuntimeTask vm(String vm);
	
	/**
	 * <p>
	 * Specifies whether unnamed modules should be added to the runtime image.
	 * </p>
	 * 
	 * <p>
	 * Enabling this option would result in all unnamed dependencies to be added to the runtime image. Please consider using {@link #addModules(java.lang.String) } to only add specific ones. Note 
	 * that at this stage unnamed dependencies should have been modularized and should be named {@code [GROUP].[NAME]} with {@code [NAME]} being escaped to be a valid Java identifier (i.e. 
	 * {@code "-"} replaced by {@code "."}...).
	 * </p>
	 * 
	 * @param addUnnamedModules true to add all unnamed modules, false otherwise
	 * 
	 * @return the task
	 */
	BuildRuntimeTask addUnnamedModules(boolean addUnnamedModules);
	
	/**
	 * <p>
	 * Specifies a list of launchers to generate in the runtime image.
	 * </p>
	 * 
	 * @param launchers a list of launchers
	 * 
	 * @return the task
	 */
	BuildRuntimeTask launchers(List<? extends Launcher> launchers);
	
	/**
	 * <p>
	 * Creates an archive task.
	 * </p>
	 * 
	 * @return an archive task
	 */
	ArchiveTask archive();
	
	/**
	 * <p>
	 * Creates and configures an archive task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured archive task
	 */
	default ArchiveTask archive(Consumer<ArchiveTask> configurer) {
		ArchiveTask archive = this.archive();
		configurer.accept(archive);
		return archive;
	}
	
	/**
	 * <p>
	 * Creates a package application task.
	 * </p>
	 * 
	 * @return a package application task
	 */
	PackageApplicationTask packageApplication();
	
	/**
	 * <p>
	 * Creates and configures a package application task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured package application task
	 */
	default PackageApplicationTask packageApplication(Consumer<PackageApplicationTask> configurer) {
		PackageApplicationTask packageApplication = this.packageApplication();
		configurer.accept(packageApplication);
		return packageApplication;
	}
	
	/**
	 * <p>
	 * Parameters describing a runtime image launcher.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface Launcher {

		/**
		 * <p>
		 * Returns the name of the launcher.
		 * </p>
		 * 
		 * @return the name of the launcher
		 */
		String getName();

		/**
		 * <p>
		 * Returns the module to execute with the launcher.
		 * </p>
		 * 
		 * <p>
		 * Defaults to the project's module.
		 * </p>
		 * 
		 * @return an optional returning the the module to execute or an empty optional
		 */
		Optional<String> getModule();

		/**
		 * <p>
		 * Returns the main class to execute with the laucnher.
		 * </p>
		 * 
		 * <p>
		 * If not specified, the launcher's module must provide a main class.
		 * </p>
		 * 
		 * @return an optional returning the main class to execute or an empty optional
		 */
		Optional<String> getMainClass();
		
		/**
		 * <p>
		 * Creates a runtime launcher of the specified name.
		 * </p>
		 * 
		 * @param name the name of the launcher.
		 * 
		 * @return a runtime launcher
		 */
		static Launcher of(String name) {
			return of(name, null, null);
		}
		
		/**
		 * <p>
		 * Creates a runtime launcher of the specified name for the specified module.
		 * </p>
		 * 
		 * @param name       the name of the launcher
		 * @param moduleName the module to execute
		 * 
		 * @return a runtime launcher
		 */
		static Launcher of(String name, String moduleName) {
			return of(name, moduleName, null);
		}
		
		/**
		 * <p>
		 * Creates a runtime launcher of the specified name for the specified module and main class.
		 * </p>
		 * 
		 * @param name       the name of the launcher
		 * @param moduleName the module to execute
		 * @param mainClass  the main class to execute
		 * 
		 * @return a runtime launcher
		 */
		static Launcher of(String name, String moduleName, String mainClass) {
			return new GenericBuildRuntimeTask.GenericLauncher(name, moduleName, mainClass);
		}
	}
}
