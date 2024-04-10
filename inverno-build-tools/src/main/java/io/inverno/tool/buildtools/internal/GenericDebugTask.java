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
package io.inverno.tool.buildtools.internal;

import io.inverno.tool.buildtools.DebugTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Generic {@link DebugTask} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericDebugTask extends AbstractExecTask<Integer, DebugTask> implements DebugTask {

	private static final Logger LOGGER = LogManager.getLogger(GenericDebugTask.class);

	private static final int DEFAULT_PORT = 8000;

	private int port = DEFAULT_PORT;
	private boolean suspend = true;
	
	/**
	 * <p>
	 * Creates a generic debug task.
	 * </p>
	 * 
	 * @param parentTask the parent task
	 */
	public GenericDebugTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}

	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		return "Debugging project " + project + "...";
	}

	@Override
	protected int getTaskWeight(BuildProject project) {
		return 0;
	}

	@Override
	public DebugTask port(int port) {
		this.port = port;
		return this;
	}

	@Override
	public DebugTask suspend(boolean suspend) {
		this.suspend = suspend;
		return this;
	}
	
	@Override
	protected Integer doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Debugging project...");
		}
		
		LOGGER.info("[ Debugging project {}... ]", project);
		
		String debugOptions = "-Xrunjdwp:transport=dt_socket,server=y,suspend=" + (this.suspend ? "y" : "n") +",address=localhost:" + this.port;
		this.vmOptions(this.vmOptions.map(options -> options + " " + debugOptions).orElse(debugOptions));
		
		Process proc = this.startProject(project);
		
		try {
			return proc.waitFor();
		} 
		catch (InterruptedException e) {
			throw new TaskExecutionException("Fatal error", e);
		}
	}
}
