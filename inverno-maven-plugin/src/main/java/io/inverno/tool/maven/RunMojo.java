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
package io.inverno.tool.maven;

import io.inverno.tool.buildtools.RunTask;
import io.inverno.tool.maven.internal.MavenInvernoProject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>
 * Runs the project application.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.VALIDATE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
public class RunMojo extends AbstractExecMojo {
	
	/**
	 * Skips the execution.
	 */
	@Parameter(property = "inverno.run.skip", required = false)
	private boolean skip;
	
	/**
	 * The command line arguments to pass to the application. This parameter overrides {@link AbstractExecMojo#arguments} when specified.
	 */
	@Parameter(property = "inverno.run.arguments", required = false)
	private String commandLineArguments;

	@Override
	protected boolean isSkipped() {
		return this.skip;
	}

	@Override
	protected void doExecute(MavenInvernoProject project) throws Exception {
		project
			.modularizeDependencies(this::configureTask)
			.run(this::configureTask)
			.execute();
	}
	
	/**
	 * <p>
	 * Configures the run task.
	 * </p>
	 * 
	 * @param runTask the run task
	 * 
	 * @return the run task
	 */
	protected RunTask configureTask(RunTask runTask) {
		super.configureTask(runTask);
		return runTask
			.arguments(StringUtils.isNotEmpty(this.commandLineArguments) ? this.commandLineArguments : this.arguments);
	}
}
