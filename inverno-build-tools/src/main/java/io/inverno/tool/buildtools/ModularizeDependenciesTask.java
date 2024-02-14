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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * A task for modularizing the dependencies of the project.
 * </p>
 *
 * <p>
 * A project can have different kind of module dependencies:
 * </p>
 * 
 * <ul>
 * <li><b>Modules</b> which define module descriptor.</li>
 * <li><b>Automatic modules</b> which define the {@code Automatic-Module-Name} entry in their {@code MANIFEST.MF} and are then named modules.</li>
 * <li><b>Unnamed modules</b> which unlike the others can't hardly be used as-is in a fully modular application.</li>
 * </ul>
 * 
 * <p>
 * The purpose of the modularize dependencies task is to repackage each automatic and unnamed dependency with a proper module descriptor (i.e. {@code module-info.java}) in order to be able to package 
 * the project in a Jmod, create an optimized runtime and native application delivrable.
 * </p>
 * 
 * <p>
 * After modularization, the module of an unnamed module dependency is named as {@code [GROUP].[NAME]} with {@code [NAME]} being escaped to be a valid Java identifier (i.e. {@code "-"} replaced by 
 * {@code "."}...).
 * </p>
 * 
 * <p>
 * Module descriptors are generated for automatic and unnamed modules, they can be extended by merging {@link ModuleInfo} instances provided with {@link #moduleOverrides(java.util.List) }. Complete 
 * module descriptors can also be provided with {@link #moduleOverridesPath(java.nio.file.Path) } in which case descriptor generation shall be skipped.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface ModularizeDependenciesTask extends Task<Set<Dependency>, ModularizeDependenciesTask> {

	/**
	 * <p>
	 * Specifies the path where to find module descritors to use for automatic and unnamed module dependencies instead of trying to generate them.
	 * </p>
	 * 
	 * <p>
	 * Each overridden {@code module-info.java} descriptor must be placed in a folder named after the module at the root of the specified path.
	 * </p>
	 * 
	 * @param moduleOverridesPath the path to the module descriptors
	 * 
	 * @return the task
	 */
	ModularizeDependenciesTask moduleOverridesPath(Path moduleOverridesPath);
	
	/**
	 * <p>
	 * Specifies a list of module info to extend the module descriptor generated for automatic and unnamed module dependencies.
	 * </p>
	 * 
	 * <p>
	 * The specified module info will be merged into the generated module descriptor for each
	 * </p>
	 * 
	 * @param moduleOverrides a list of module info
	 * 
	 * @return the task
	 */
	ModularizeDependenciesTask moduleOverrides(List<? extends ModuleInfo> moduleOverrides);
	
	/**
	 * <p>
	 * Creates a start task.
	 * </p>
	 * 
	 * @return a start task
	 */
	StartTask start();
	
	/**
	 * <p>
	 * Creates and configures a start task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured start task
	 */
	default StartTask start(Consumer<StartTask> configurer) {
		StartTask start = this.start();
		configurer.accept(start);
		return start;
	}
	
	/**
	 * <p>
	 * Creates a run task.
	 * </p>
	 * 
	 * @return a run task
	 */
	RunTask run();
	
	/**
	 * <p>
	 * Creates and configures a run task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured run task
	 */
	default RunTask run(Consumer<RunTask> configurer) {
		RunTask run = this.run();
		configurer.accept(run);
		return run;
	}
	
	/**
	 * <p>
	 * Creates a debug task.
	 * </p>
	 * 
	 * @return a debug task
	 */
	DebugTask debug();
	
	/**
	 * <p>
	 * Creates and configures a debug task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured debug task
	 */
	default DebugTask debug(Consumer<DebugTask> configurer) {
		DebugTask debug = this.debug();
		configurer.accept(debug);
		return debug;
	}
	
	/**
	 * <p>
	 * Creates a jmod build task.
	 * </p>
	 * 
	 * @return a jmod build task
	 */
	BuildJmodTask buildJmod();
	
	/**
	 * <p>
	 * Creates and configures a jmod build task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured jmod build task
	 */
	default BuildJmodTask buildJmod(Consumer<BuildJmodTask> configurer) {
		BuildJmodTask buildJmod = this.buildJmod();
		configurer.accept(buildJmod);
		return buildJmod;
	}
}
