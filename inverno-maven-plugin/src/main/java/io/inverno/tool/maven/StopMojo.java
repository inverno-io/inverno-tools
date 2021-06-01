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

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.inverno.tool.maven.internal.ProjectModule;

/**
 * <p>
 * Stops the project application that has been previously started using the
 * {@code start} goal.
 * </p>
 * 
 * <p>
 * This goal is used together with the {@code start} goal in the
 * {@code pre-integration-test} and {@code post-integration-test} phases to run
 * integration tests.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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

	protected Path jmodsPath;
	
	@Override
	protected boolean isSkip() {
		return this.skip;
	}
	
	@Override
	public void doExecute() throws MojoExecutionException, MojoFailureException {
			ModuleReference projectModuleReference = ModuleFinder.of(Paths.get(this.project.getBuild().getOutputDirectory())).findAll().stream().findFirst().get();
			ProjectModule projectModule = new ProjectModule(this.project, projectModuleReference.descriptor(), Set.of(), this.invernoBuildPath, this.jmodsPath, ProjectModule.Classifier.RUNTIME, Set.of());

			this.getLog().info("Stopping project: " + projectModule + "...");
			if(Files.exists(projectModule.getPidfile())) {
				try {
					ProcessHandle.of(Long.parseLong(new String(Files.readAllBytes(projectModule.getPidfile())))).ifPresent(ph -> {
						ph.destroy();
						try {
							ph.onExit().get(this.timeout, TimeUnit.MILLISECONDS);
						} 
						catch (InterruptedException | ExecutionException e) {
							this.getLog().error(e);
						}
						catch (TimeoutException e) {
							ph.destroyForcibly();
						}
					});
				}
				catch (NumberFormatException e) {
					this.getLog().error("Invalid pidfile: " + projectModule.getPidfile(), e);
				}
				catch (IOException e) {
					this.getLog().error("Error reading pidfile: " + projectModule.getPidfile(), e);
				}
			}
			else {
				this.getLog().warn("[ Project doesn't appear to be running, pidfile is not present: " + projectModule.getPidfile() + " ]");
			}
	}
	
	protected void initializePaths() throws IOException {
		super.initializePaths();
		
		this.jmodsPath = this.invernoBuildPath.resolve("jmods").toAbsolutePath();
	}
}
