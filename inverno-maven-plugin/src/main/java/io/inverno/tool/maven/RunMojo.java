/*
 * Copyright 2021 Jeremy KUHN
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

import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.StringUtils;

import io.inverno.tool.maven.internal.ProjectModule;
import io.inverno.tool.maven.internal.task.ExecuteProjectTask;

/**
 * <p>
 * Runs the project application.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.VALIDATE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
public class RunMojo extends AbstractExecMojo {
	
	/**
	 * The command line arguments to pass to the application. This parameter
	 * overrides {@link AbstractExecMojo#arguments} when specified.
	 */
	@Parameter(property = "inverno.run.arguments", required = false)
	private String commandLineArguments;
	
	@Override
	protected void handleProcess(ProjectModule projectModule, Process proc) throws MojoExecutionException, MojoFailureException {
		try {
			int exitValue = proc.waitFor();
			if (exitValue != 0) {
				throw new MojoExecutionException("Application did not exit properly: exit(" + exitValue + ")");
			}
		} 
		catch (InterruptedException e) {
			throw new MojoExecutionException("Fatal error", e);
		}
	}
	
	protected ExecuteProjectTask getExecuteProjectTask(ProjectModule projectModule) {
		ExecuteProjectTask task = super.getExecuteProjectTask(projectModule);

		if(StringUtils.isNotEmpty(this.commandLineArguments)) {
			task.setArguments(Optional.of(this.commandLineArguments));
		}
		
		return task;
	}
}
