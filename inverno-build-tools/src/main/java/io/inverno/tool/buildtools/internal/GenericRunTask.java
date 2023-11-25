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
package io.inverno.tool.buildtools.internal;

import io.inverno.tool.buildtools.RunTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Generic {@link RunTask} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericRunTask extends AbstractExecTask<Integer, RunTask> implements RunTask {

	private static final Logger LOGGER = LogManager.getLogger(GenericRunTask.class);
	
	/**
	 * <p>
	 * Creates a generic run task.
	 * <p>
	 * 
	 * @param parentTask the parent task
	 */
	public GenericRunTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}

	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		return "Running project " + project + "...";
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		return 0;
	}
	
	@Override
	protected Integer doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Running project...");
		}
		
		LOGGER.info("[ Running project {}... ]", project);
		
		Process proc = this.startProject(project);
		
		try {
			return proc.waitFor();
		} 
		catch (InterruptedException e) {
			throw new TaskExecutionException("Fatal error", e);
		}
	}
}
