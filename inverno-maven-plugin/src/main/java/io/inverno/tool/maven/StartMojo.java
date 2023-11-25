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

import io.inverno.tool.buildtools.StartTask;
import io.inverno.tool.maven.internal.MavenInvernoProject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>
 * Starts the project application without blocking the Maven build.
 * </p>
 * 
 * <p>
 * This goal is used together with the {@code stop} goal in the {@code pre-integration-test} and {@code post-integration-test} phases to run integration tests.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class StartMojo extends AbstractExecMojo {

	/**
	 * The name of the property indicating the path to the application pidfile.
	 */
	private static final String PROPERTY_PID_FILE = "inverno.application.pid_file";
	
	/**
	 * Skips the execution.
	 */
	@Parameter(property = "inverno.start.skip", required = false)
	private boolean skip;
	
	@Override
	protected boolean isSkipped() {
		return this.skip;
	}

	@Override
	protected void doExecute(MavenInvernoProject project) throws Exception {
		project
			.modularizeDependencies(this::configureTask)
			.start(startTask -> this.configureTask(project, startTask))
			.execute();
	}
	
	/**
	 * <p>
	 * Configures the start task.
	 * </p>
	 * 
	 * @param project   the Maven Inverno project
	 * @param startTask the start task
	 * 
	 * @return the start task
	 */
	protected StartTask configureTask(MavenInvernoProject project, StartTask startTask) {
		super.configureTask(startTask);
		String pidfileVmOption = "-D" + PROPERTY_PID_FILE + "=" + project.getPidfile();
		return startTask
			.pidfile(project.getPidfile())
			.vmOptions(StringUtils.isNotBlank(this.vmOptions) ? pidfileVmOption + " " + this.vmOptions : pidfileVmOption);
	}
}
