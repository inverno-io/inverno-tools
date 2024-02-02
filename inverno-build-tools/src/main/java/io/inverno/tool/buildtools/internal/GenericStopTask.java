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

import io.inverno.tool.buildtools.Project;
import io.inverno.tool.buildtools.StopTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Generic {@link StopTask} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericStopTask extends AbstractTask<Long, StopTask> implements StopTask {
	
	private static final Logger LOGGER = LogManager.getLogger(GenericStopTask.class);

	private static final long DEFAULT_TIMEOUT = 60000l;
	
	private Optional<Path> pidfile = Optional.empty();
	private long timeout = DEFAULT_TIMEOUT;
	
	/**
	 * <p>
	 * Creates a generic stop task.
	 * </p>
	 * 
	 * @param project the project
	 */
	public GenericStopTask(Project project) {
		super(project);
	}
	
	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		return "Project stopped";
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		return 1;
	}
	
	@Override
	public StopTask pidfile(Path pidfile) {
		this.pidfile = Optional.ofNullable(pidfile);
		return this;
	}

	@Override
	public StopTask timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	protected Long doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Stopping project...");
		}
		
		LOGGER.info("[ Stopping project {}... ]", project);
		
		Path projectPidfile = this.pidfile.orElse(project.getPidfile());
		if(Files.exists(projectPidfile)) {
			try {
				Long pid = Long.valueOf(Files.readString(projectPidfile));
				ProcessHandle.of(pid).ifPresentOrElse(
					ph -> {
						ph.destroy();
						try {
							ph.onExit().get(this.timeout, TimeUnit.MILLISECONDS);
						} 
						catch (InterruptedException | ExecutionException e) {
							throw new TaskExecutionException("Error stopping project gracefully: " + project + "(" + projectPidfile + ")", e);
						}
						catch (TimeoutException e) {
							LOGGER.error("Application exit timeout exceeded, trying to stop the process forcibly...");
							ph.destroyForcibly();
							try {
								ph.onExit().get(this.timeout, TimeUnit.MILLISECONDS);
							} 
							catch (InterruptedException | ExecutionException e1) {
								throw new TaskExecutionException("Error stopping project forcibly: " + project + "(" + projectPidfile + ")", e1);
							}
							catch (TimeoutException e1) {
								throw new TaskExecutionException("Application exit timeout exceeded on both graceful and forced shutdown attempts");
							}
						}
					},
					() -> LOGGER.warn("[ Project doesn't appear to be running, removing existing pidfile {} ]", projectPidfile)
				);
				Files.deleteIfExists(projectPidfile);
				return pid;
			}
			catch (IOException | NumberFormatException e) {
				throw new TaskExecutionException("Error reading pidfile: " + projectPidfile, e);
			}
		}
		else {
			LOGGER.warn("[ Project doesn't appear to be running, pidfile is not present: {} ]", projectPidfile);
			return null;
		}
	}
}
