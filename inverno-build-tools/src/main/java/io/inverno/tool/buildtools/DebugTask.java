/*
 * Copyright 2024 Jeremy Kuhn
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

/**
 * <p>
 * Debugs the project in a separate process and wait for the application to stop.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface DebugTask extends ExecTask<Integer, DebugTask> {

	/**
	 * <p>
	 * Sets the port exposed by the application where to attach the debugger.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 8000}.
	 * </p>
	 * 
	 * @param port the debug port
	 * 
	 * @return the task
	 */
	DebugTask port(int port);
	
	/**
	 * <p>
	 * Sets whether to suspend VM execution when starting the application.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code true}.
	 * </p>
	 * 
	 * @param suspend true to suspend the execution, false otherwise
	 * 
	 * @return the task
	 */
	DebugTask suspend(boolean suspend);
}
