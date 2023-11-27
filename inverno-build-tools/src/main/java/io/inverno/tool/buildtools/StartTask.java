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
 * Starts the project in a separate process and return.
 * </p>
 * 
 * <p>
 * Unlike {@link RunTask}, the execution returns right after the project application is running, leaving it running in background. A subsequent execution of {@link StopTask} is used to stop it.
 * </p>
 * 
 * <p>
 * A project is considered as running when a pifdile has been created which releases the execution thread. As a result, the project application is then expected to generate a pidfile. If no pidfile is
 * provided after the timeout, a {@link TaskExecutionException} is thrown and the process is killed.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface StartTask extends ExecTask<Long, StartTask> {

	/**
	 * <p>
	 * Sets the path of the file where the project pid is stored.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code [WORKING_PATH]/[PROJECT_NAME].pid"}
	 * </p>
	 * 
	 * @param pidfile the path to the pidfile
	 * 
	 * @return the task
	 */
	StartTask pidfile(Path pidfile);
	
	/**
	 * <p>
	 * Sets the time to wait for the pidfile to be created by the project application.
	 * </p>
	 * 
	 * <p>
	 * If no pidfile is present after the timeout, task execution fails with a {@link TaskExecutionException} and the process is killed.
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
	StartTask timeout(long timeout);
}
