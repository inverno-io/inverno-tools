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
import java.util.function.Consumer;

/**
 * <p>
 * A task for building a JMOD archive for the project module.
 * </p>
 * 
 * <p>
 * This task depends on {@link ModularizeDependenciesTask}.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface BuildJmodTask extends Task<Path, BuildJmodTask> {

	/**
	 * <p>
	 * Specifies the main class to record in the {@code module-info.class} file.
	 * </p>
	 * 
	 * @param mainClass the main class
	 * 
	 * @return the task
	 */
	BuildJmodTask mainClass(String mainClass);
	
	/**
	 * <p>
	 * Specifies whether the main class should be automatically resolved in the project module.
	 * </p>
	 * 
	 * <p>
	 * When the project module defines multiple main classes, the resolution will fail with a {@link TaskExecutionException} inviting you to explicitly select the main class with 
	 * {@link #mainClass(java.lang.String) }.
	 * </p>
	 * 
	 * @param resolveMainClass true to automatically resolve the main class, false otherwise
	 * 
	 * @return the task
	 */
	BuildJmodTask resolveMainClass(boolean resolveMainClass);
	
	/**
	 * <p>
	 * Specifies the path to user-editable configuration files to copy into the resulting JMOD file.
	 * </p>
	 * 
	 * @param configurationPath the path to configuration files
	 * 
	 * @return the task
	 */
	BuildJmodTask configurationPath(Path configurationPath);
	
	/**
	 * <p>
	 * Specifies the path to legal notices to copy into the resulting JMOD file.
	 * </p>
	 * 
	 * @param legalPath the path to legal notices
	 * 
	 * @return the task
	 */
	BuildJmodTask legalPath(Path legalPath);
	
	/**
	 * <p>
	 * Specifies the path to man pages to copy into the resulting JMOD file.
	 * </p>
	 * 
	 * @param manPath the path to man pages
	 * 
	 * @return the task
	 */
	BuildJmodTask manPath(Path manPath);
	
	/**
	 * <p>
	 * Creates a build runtime task.
	 * </p>
	 * 
	 * @return an archive task
	 */
	BuildRuntimeTask buildRuntime();
	
	/**
	 * <p>
	 * Creates and configures a build runtime task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured build runtime task
	 */
	default BuildRuntimeTask buildRuntime(Consumer<BuildRuntimeTask> configurer) {
		BuildRuntimeTask buildRuntime = this.buildRuntime();
		configurer.accept(buildRuntime);
		return buildRuntime;
	}
}
