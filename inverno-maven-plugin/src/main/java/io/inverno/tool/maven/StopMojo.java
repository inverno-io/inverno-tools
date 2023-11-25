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

import io.inverno.tool.buildtools.StopTask;
import io.inverno.tool.maven.internal.MavenInvernoProject;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Stops the project application that has been previously started using the {@code start} goal.
 * </p>
 *
 * <p>
 * This goal is used together with the {@code start} goal in the {@code pre-integration-test} and {@code post-integration-test} phases to run integration tests.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, requiresProject = true)
public class StopMojo extends AbstractInvernoMojo {

	/**
	 * Skips the execution.
	 */
	@Parameter(property = "inverno.stop.skip", required = false)
	private boolean skip;
	
	/**
	 * The amount of time in milliseconds to wait for the application to stop.
	 */
	@Parameter(property = "inverno.stop.timeout", defaultValue = "60000", required = false)
	private long timeout;
	
	@Override
	protected boolean isSkipped() {
		return this.skip;
	}

	@Override
	protected void doExecute(MavenInvernoProject project) throws Exception {
		project
			.stop(stopTask -> this.configureTask(project, stopTask))
			.execute();
	}
	
	/**
	 * <p>
	 * Configures the stop task.
	 * </p>
	 * 
	 * @param project   the Maven Inverno project
	 * @param stopTask  the stop task
	 * 
	 * @return the stop task
	 */
	protected StopTask configureTask(MavenInvernoProject project, StopTask stopTask) {
		return stopTask
			.pidfile(project.getPidfile())
			.timeout(this.timeout);
	}
}
