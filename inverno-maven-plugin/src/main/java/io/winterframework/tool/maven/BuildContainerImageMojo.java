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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.inverno.tool.maven.internal.ProgressBar;
import io.inverno.tool.maven.internal.task.CreateProjectContainerImageTask;

/**
 * <p>
 * Builds a container image and publishes it to a registry.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "build-image", defaultPhase = LifecyclePhase.INSTALL, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class BuildContainerImageMojo extends AbstractContainerImageMojo {

	/**
	 * The user name to use to authenticate to the registry.
	 */
	@Parameter(property = "inverno.container.registry.username", required = false)
	private String registryUsername;
	
	/**
	 * The password to use to authenticate to the registry.
	 */
	@Parameter(property = "inverno.container.registry.password", required = false)
	private String registryPassword;
	
	protected CreateProjectContainerImageTask getCreateProjectContainerImageTask(ProgressBar.Step step) {
		CreateProjectContainerImageTask task = super.getCreateProjectContainerImageTask(step);

		task.setTarget(CreateProjectContainerImageTask.Target.REGISTRY);
		task.setRegistryUsername(Optional.ofNullable(this.registryUsername));
		task.setRegistryPassword(Optional.ofNullable(this.registryPassword));
		
		return task;
	}
}
