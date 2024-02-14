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
package io.inverno.tool.maven;

import io.inverno.tool.buildtools.DebugTask;
import io.inverno.tool.maven.internal.MavenInvernoProject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>
 * Debugs the project application.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
@Mojo(name = "debug", defaultPhase = LifecyclePhase.VALIDATE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
public class DebugMojo extends AbstractExecMojo {

	/**
	 * Skips the execution.
	 */
	@Parameter(property = "inverno.debug.skip", required = false)
	private boolean skip;
	
	/**
	 * The command line arguments to pass to the application. This parameter overrides {@link AbstractExecMojo#arguments} when specified.
	 */
	@Parameter(property = "inverno.debug.arguments", required = false)
	private String commandLineArguments;
	
	/**
	 * The debug port.
	 */
	@Parameter(property = "inverno.debug.port", defaultValue = "8000", required = false)
	private int port;
	
	/**
	 * Indicates whether to suspend execution until a debugger is attached.
	 */
	@Parameter(property = "inverno.debug.suspend", defaultValue = "true", required = false)
	private boolean suspend;

	@Override
	protected boolean isSkipped() {
		return this.skip;
	}
	
	@Override
	protected void doExecute(MavenInvernoProject project) throws Exception {
		project
			.modularizeDependencies(this::configureTask)
			.debug(this::configureTask)
			.execute();
	}
	
	/**
	 * <p>
	 * Configures the debug task.
	 * </p>
	 * 
	 * @param debugTask the debug task
	 * 
	 * @return the debug task
	 */
	protected DebugTask configureTask(DebugTask debugTask) {
		super.configureTask(debugTask);
		return debugTask
			.arguments(StringUtils.isNotEmpty(this.commandLineArguments) ? this.commandLineArguments : this.arguments)
			.port(this.port)
			.suspend(this.suspend);
	}
}
