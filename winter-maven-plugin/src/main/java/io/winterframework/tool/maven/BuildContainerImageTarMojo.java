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

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.winterframework.tool.maven.internal.ProgressBar;
import io.winterframework.tool.maven.internal.ProjectModule.Classifier;
import io.winterframework.tool.maven.internal.task.CreateProjectContainerImageTask;

/**
 * <p>
 * Builds a container image to a TAR archive that can be later loaded into Docker:
 * </p>
 * 
 * <blockquote>
 * <pre>
 * $ docker load --input target/{@literal<image>}.tar 
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "build-image-tar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildContainerImageTarMojo extends AbstractContainerImageMojo {

	@Override
	protected void doExecute() throws MojoExecutionException, MojoFailureException {
		super.doExecute();
		
		if(this.attach) {
			Path tarContainerImagePath = this.projectModule.getContainerImageTarPath();
			this.projectHelper.attachArtifact(this.project, "tar", Classifier.CONTAINER.getClassifier(), tarContainerImagePath.toFile());
		}
	}
	
	protected CreateProjectContainerImageTask getCreateProjectContainerImageTask(ProgressBar.Step step) {
		CreateProjectContainerImageTask task = super.getCreateProjectContainerImageTask(step);

		task.setTarget(CreateProjectContainerImageTask.Target.TAR);
		
		return task;
	}
}
