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
import io.inverno.tool.buildtools.Task;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.util.function.Consumer;

/**
 * <p>
 * Base {@link Task} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <U> the type of task result
 * @param <V> the type of the task
 */
public abstract class AbstractTask<U, V extends Task<U, V>> implements Task<U, V> {

	private final Project project;
	
	private final AbstractTask<?, ?> parentTask;
	
	private Consumer<U> onComplete;
	
	/**
	 * <p>
	 * Creates a root task.
	 * </p>
	 * 
	 * @param project the project
	 */
	public AbstractTask(Project project) {
		this.project = project;
		this.parentTask = null;
	}
	
	/**
	 * <p>
	 * Creates a task.
	 * </p>
	 * 
	 * @param parentTask the parent task 
	 */
	public AbstractTask(AbstractTask<?, ?> parentTask) {
		this.project = parentTask.project;
		this.parentTask = parentTask;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V doOnComplete(Consumer<U> onComplete) {
		if(this.onComplete == null) {
			this.onComplete = onComplete;
		}
		else {
			this.onComplete = this.onComplete.andThen(onComplete);
		}
		return (V)this;
	}
	
	@Override
	public final U execute() throws TaskExecutionException {
		BuildProject buildProject = new BuildProject(this.project);
		return this.execute(buildProject, Boolean.parseBoolean(System.getProperty(Project.PROPERY_DISPLAY_PROGRESS_BAR)) ? new ProgressBar(this.getTaskCompletionMessage(buildProject)) : null);
	}
	
	/**
	 * <p>
	 * Executes the task by executing its parent tasks first from the root task.
	 * </p>
	 * 
	 * <p>
	 * In a progress bar is specified, a step is created for each task using {@link #getTaskWeight(io.inverno.tool.buildtools.internal.BuildProject) } as weight.
	 * </p>
	 * 
	 * @param buildProject the build project
	 * @param progressBar  the progress bar or null 
	 * 
	 * @return the task execution result
	 * 
	 * @throws TaskExecutionException if there was an error executing the task
	 */
	private U execute(BuildProject buildProject, ProgressBar progressBar) throws TaskExecutionException {
		ProgressBar.Step step = progressBar != null ? progressBar.addStepFirst(this.getTaskWeight(buildProject)) : null;
		if(this.parentTask != null) {
			this.parentTask.execute(buildProject, progressBar);
		}
		else if(progressBar != null) {
			progressBar.display();
		}

		try {
			U result = this.doExecute(buildProject, step);
			if(this.onComplete != null) {
				this.onComplete.accept(result);
			}
			return result;
		}
		finally {
			if(step != null) {
				step.done();
			}
		}
	}
	
	/**
	 * <p>
	 * Returns the task completion message that must be displayed in the progress bar when the leaf task completes successfully.
	 * </p>
	 * 
	 * @param project the build project
	 * 
	 * @return the completion message
	 */
	protected abstract String getTaskCompletionMessage(BuildProject project);
	
	/**
	 * <p>
	 * 
	 * </p>
	 * 
	 * @param project the build project
	 * @return 
	 */
	protected abstract int getTaskWeight(BuildProject project);
	
	/**
	 * <p>
	 * Executes the task.
	 * </p>
	 * 
	 * @param project the build project
	 * @param step    the progress step or null
	 * 
	 * @return the task execution result
	 * 
	 * @throws TaskExecutionException if there was an error executing the task
	 */
	protected abstract U doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException;
}
