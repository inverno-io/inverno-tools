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

/**
 * <p>
 * Base execution task.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <U> the type returned by the task execution
 * @param <V> the type of the task
 */
public interface ExecTask<U, V extends ExecTask<U, V>> extends Task<U, V> {

	/**
	 * <p>
	 * Sets the main class to be executed by the task.
	 * </p>
	 * 
	 * @param mainClass the canonical name of the main class in the project module
	 * 
	 * @return the task
	 */
	V mainClass(String mainClass);
	
	/**
	 * <p>
	 * Sets the arguments to pass to the application.
	 * </p>
	 * 
	 * @param arguments the application arguments
	 * 
	 * @return the task
	 */
	V arguments(String arguments);
	
	/**
	 * <p>
	 * Sets the VM options to pass to the application.
	 * </p>
	 * 
	 * @param vmOptions the VM options
	 * 
	 * @return the task
	 */
	V vmOptions(String vmOptions);
	
	/**
	 * <p>
	 * Sets the working path of the application.
	 * </p>
	 * 
	 * @param workingPath the working path
	 * 
	 * @return the task
	 */
	V workingPath(Path workingPath);

	/**
	 * <p>
	 * Sets whether unnamed modules should be added when executing the application.
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @param addUnnamedModules true to add unnamed modules, false otherwise
	 * 
	 * @return 
	 */
	V addUnnamedModules(boolean addUnnamedModules);
	
	/**
	 * <p>
	 * Sets where to redirect the application input stream.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link ProcessBuilder.Redirect#INHERIT}.
	 * </p>
	 * 
	 * @param redirectInput the input source
	 * 
	 * @return the task
	 */
	V redirectInput(ProcessBuilder.Redirect redirectInput);
	
	/**
	 * <p>
	 * Sets where to redirect the application output stream.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link ProcessBuilder.Redirect#INHERIT}.
	 * </p>
	 * 
	 * @param redirectOutput the output destination
	 * 
	 * @return 
	 */
	V redirectOutput(ProcessBuilder.Redirect redirectOutput);
	
	/**
	 * <p>
	 * Sets where to redirect the application error stream.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link ProcessBuilder.Redirect#INHERIT}.
	 * </p>
	 * 
	 * @param redirectError the error destination
	 * 
	 * @return the task
	 */
	V redirectError(ProcessBuilder.Redirect redirectError);
}
