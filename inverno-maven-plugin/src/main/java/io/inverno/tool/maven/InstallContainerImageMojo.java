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

import io.inverno.tool.buildtools.ContainerizeTask;
import io.inverno.tool.maven.internal.MavenInvernoProject;
import java.io.File;
import java.util.Map;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>
 * Builds and installs the project application container image to the local Docker daemon.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
@Mojo(name = "install-image", defaultPhase = LifecyclePhase.INSTALL, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class InstallContainerImageMojo extends AbstractContainerizeMojo {

	/**
	 * Skips the build and installation of the container image.
	 */
	@Parameter(property = "inverno.image.install.skip", required = false)
	private boolean skip;
	
	/**
	 * The path to the Docker CLI executable used to load the image in the Docker daemon.
	 */
	@Parameter(property = "inverno.container.dockerExecutable", required = false)
	private File dockerExecutableFile;
	
	/**
	 * The Docker environment variables used by the Docker CLI executable.
	 */
	@Parameter(required = false)
	private Map<String, String> dockerEnvironment;

	@Override
	protected boolean isSkipped() {
		return this.skip;
	}

	@Override
	protected void doExecute(MavenInvernoProject project) throws Exception {
		ContainerizeTask.ContainerImage image = project
			.modularizeDependencies(this::configureTask)
			.buildJmod(this::configureTask)
			.buildRuntime(this::configureTask)
			.packageApplication(this::configureTask)
			.containerize(this::configureTask)
			.execute();

		this.getLog().info("Project image " + image.getCanonicalName() + " installed to Docker");
	}
	
	@Override
	protected ContainerizeTask configureTask(ContainerizeTask packageApplicationTask) {
		return super.configureTask(packageApplicationTask)
			.target(ContainerizeTask.Target.DOCKER)
			.format(ContainerizeTask.Format.Docker)
			.dockerExecutable(this.dockerExecutableFile != null ? this.dockerExecutableFile.toPath().toAbsolutePath() : null)
			.dockerEnvironment(this.dockerEnvironment);
	}
}
