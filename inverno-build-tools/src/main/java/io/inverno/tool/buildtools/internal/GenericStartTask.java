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

import io.inverno.tool.buildtools.StartTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Generic {@link StartTask} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericStartTask extends AbstractExecTask<Long, StartTask> implements StartTask {

	private static final Logger LOGGER = LogManager.getLogger(GenericStartTask.class);

	private static final long DEFAULT_TIMEOUT = 60000l;
	private static final long POLL_TIMEOUT = 250l;

	private Optional<Path> pidfile = Optional.empty();
	private long timeout = DEFAULT_TIMEOUT;
	
	/**
	 * <p>
	 * Creates a generic start task.
	 * </p>
	 * 
	 * @param parentTask the parent task
	 */
	public GenericStartTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}
	
	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		return "Project " + project + " started";
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		return 15;
	}
	
	@Override
	public StartTask pidfile(Path pidfile) {
		this.pidfile = Optional.ofNullable(pidfile);
		return this;
	}

	@Override
	public StartTask timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	protected Long doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Starting project...");
		}
		
		LOGGER.info("[ Starting project {}... ]", project);
		
		Process proc = this.startProject(project);
		if(proc.isAlive()) {
			// We must wait for the pidfile to appear
			Path projectPidfile = this.pidfile.orElse(project.getPidfile());
			try(WatchService watchService = FileSystems.getDefault().newWatchService()) {
				projectPidfile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
				int tries = (int) (this.timeout/POLL_TIMEOUT);
				WatchKey watchKey = null;
				for(int i = 0;i<tries;i++) {
					watchKey = watchService.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
					if(watchKey == null) {
						if(!proc.isAlive()) {
							throw new TaskExecutionException("Application exited: exit(" + proc.exitValue() + ")");
						}
					}
					else if(watchKey.pollEvents().stream().map(WatchEvent::context).anyMatch(path -> path.equals(projectPidfile.getFileName()))) {
						watchKey.cancel();
						for(int j=i;j<tries;j++) {
							try {
								return Long.valueOf(Files.readString(projectPidfile));
							}
							catch(IOException | NumberFormatException e) {
								if(j < tries-1) {
									continue;
								}
								try {
									LOGGER.error("Unable to get pid, trying to stop the process gracefully...", e);
									this.destroyProcess(proc);
								}
								finally {
									throw new TaskExecutionException("Error reading pidfile: " + projectPidfile, e);
								}
							}
						}
					}
					else {
						watchKey.cancel();
					}
				}
				
				if(watchKey == null) {
					// proc is alive at this stage
					LOGGER.error("Application startup timeout exceeded, trying to stop the process gracefully...");
					try {
						this.destroyProcess(proc);
					}
					finally {
						throw new TaskExecutionException("Application startup timeout exceeded");
					}
				}
				return null;
			}
			catch (IOException | InterruptedException e) {
				try {
					if(proc.isAlive()) {
						LOGGER.error("Fatal error, trying to stop the process gracefully...");
						this.destroyProcess(proc);
					}
				}
				finally {
					throw new TaskExecutionException("Fatal error", e);
				}
			}
		}
		else {
			LOGGER.warn("Application exited: exit(" + proc.exitValue() + ")");
			return null;
		}
	}
	
	/**
	 * <p>
	 * Destroys the specified process.
	 * </p>
	 * 
	 * @param proc the process to destroy
	 */
	private void destroyProcess(Process proc) {
		ProcessHandle ph = proc.toHandle();
		ph.destroy();
		try {
			ph.onExit().get(this.timeout, TimeUnit.MILLISECONDS);
		} 
		catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e);
		}
		catch (TimeoutException e) {
			ph.destroyForcibly();
		}
	}
}
