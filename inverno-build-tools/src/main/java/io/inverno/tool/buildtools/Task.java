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

import java.util.function.Consumer;

/**
 * <p>
 * A build task. 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <U> the type of task result
 * @param <V> the type of the task
 */
public interface Task<U, V extends Task<U,V>> {

	/**
	 * <p>
	 * Adds behaviour when the task is executed successfully.
	 * </p>
	 * 
	 * @param onComplete the callback invoked with the task execution result
	 * 
	 * @return the task
	 */
	V doOnComplete(Consumer<U> onComplete);
	
	/**
	 * <p>
	 * Executes the task.
	 * </p>
	 * 
	 * @return the task execution result
	 * 
	 * @throws TaskExecutionException if an error was raised during the execution of the task
	 */
	U execute() throws TaskExecutionException;
}
