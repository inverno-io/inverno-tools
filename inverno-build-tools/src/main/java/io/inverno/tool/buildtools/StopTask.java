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
 * A task for stopping a running project whose pid is provided in a file.
 * </p>
 * 
 * <p>
 * This task is typically executed to stop a project started using {@link StartTask}.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface StopTask extends Task<Long, StopTask> {

	/**
	 * <p>
	 * Sets the path to the file containing the pid of the running project.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code [WORKING_PATH]/[PROJECT_NAME].pid}
	 * </p>
	 * 
	 * @param pidfile The path tp the pidfile
	 * 
	 * @return the task
	 */
	StopTask pidfile(Path pidfile);
	
	/**
	 * <p>
	 * Sets the time to wait for the running project to stop after sending the SIGINT signal.
	 * </p>
	 * 
	 * <p>
	 * If the process is not stopped after the timeout, task execution fails with a {@link TaskExecutionException}.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 60000}.
	 * </p>
	 * 
	 * @param timeout the timeout in milliseconds
	 * 
	 * @return the task
	 */
	StopTask timeout(long timeout);
}
