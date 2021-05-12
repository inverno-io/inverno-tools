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
package io.winterframework.tool.maven;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.cloud.tools.jib.api.buildplan.ImageFormat;

import io.winterframework.tool.maven.internal.ProgressBar;
import io.winterframework.tool.maven.internal.task.CreateProjectContainerImageTask;

/**
 * <p>
 * Builds a Docker container image to a local Docker daemon.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "build-image-docker", defaultPhase = LifecyclePhase.INSTALL, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class BuildContainerImageDockerMojo extends AbstractContainerImageMojo {

	/**
	 * The path to the Docker CLI executable used to load the image in the Docker daemon.
	 */
	@Parameter(property = "winter.container.docker.executable", required = false)
	private File dockerExecutable;
	
	/**
	 * The Docker environment variables used by the Docker CLI executable.
	 */
	@Parameter(required = false)
	private Map<String, String> dockerEnvironment;
	
	protected CreateProjectContainerImageTask getCreateProjectContainerImageTask(ProgressBar.Step step) {
		CreateProjectContainerImageTask task = super.getCreateProjectContainerImageTask(step);

		task.setTarget(CreateProjectContainerImageTask.Target.DOCKER);
		task.setImageFormat(Optional.of(ImageFormat.Docker));
		task.setDockerExecutable(Optional.ofNullable(this.dockerExecutable).map(file -> file.toPath().toAbsolutePath()));
		task.setDockerEnvironment(Optional.ofNullable(this.dockerEnvironment).filter(environment -> !environment.isEmpty()));
		
		return task;
	}
}
