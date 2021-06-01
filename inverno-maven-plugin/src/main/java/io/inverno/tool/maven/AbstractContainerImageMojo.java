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

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import com.google.cloud.tools.jib.api.buildplan.ImageFormat;

import io.inverno.tool.maven.internal.DependencyModule;
import io.inverno.tool.maven.internal.ProgressBar;
import io.inverno.tool.maven.internal.ProjectModule;
import io.inverno.tool.maven.internal.task.CompileModuleDescriptorsTask;
import io.inverno.tool.maven.internal.task.CreateProjectApplicationTask;
import io.inverno.tool.maven.internal.task.CreateProjectContainerImageTask;
import io.inverno.tool.maven.internal.task.CreateProjectJmodTask;
import io.inverno.tool.maven.internal.task.CreateProjectRuntimeTask;
import io.inverno.tool.maven.internal.task.ModularizeDependenciesTask;
import io.inverno.tool.maven.internal.task.PackageModularizedDependenciesTask;

/**
 * <p>
 * Base Container image mojo.
 * <p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractContainerImageMojo extends BuildApplicationMojo {
	
	/**
	 * The base container image.
	 */
	@Parameter(property = "inverno.container.from", defaultValue = "debian:buster-slim", required = true)
	private String from;
	
	/**
	 * The executable in the application image to use as image entry point.
	 * 
	 * The specified name should correspond to a declared application image
	 * launchers or the project artifact id if no launcher was specified.
	 */
	@Parameter(property = "inverno.app.executable", defaultValue = "${project.artifactId}", required = true)
	private String executable;
	
	/**
	 * The registry part of the target image reference defined as: <code>${registry}/${repository}/${name}:${project.version}</code>
	 */
	@Parameter(property = "inverno.container.registry", required = false)
	private String registry;
	
	/**
	 * The repository part of the target image reference defined as: <code>${registry}/${repository}/${name}:${project.version}</code>
	 */
	@Parameter(property = "inverno.container.repository", required = false)
	private String repository;
	
	/**
	 * The format of the container image.
	 */
	@Parameter(property = "inverno.container.imageFormat", defaultValue = "Docker", required = false)
	private ImageFormat imageFormat;

	/**
	 * The labels to apply to the container image.
	 */
	@Parameter(required = false)
	private Map<String, String> labels;
	
	/**
	 * The ports exposed by the container at runtime defined as: {@code port_number [ "/" udp/tcp ] }
	 */
	@Parameter(required = false)
	private Set<String> ports;
	
	/**
	 * The container's mount points.
	 */
	@Parameter(required = false)
	private Set<String> volumes;
	
	/**
	 * The user and group used to run the container defined as: {@code user / uid [ ":" group / gid ]}
	 */
	@Parameter(required = false)
	private String user;
	
	/**
	 * The container's environment variables.
	 */
	@Parameter(required = false)
	private Map<String, String> environment;
	
	@Override
	protected void doExecute() throws MojoExecutionException, MojoFailureException {
		if(this.jpackage == null) {
			throw new MojoExecutionException("'jdk.jpackage' module is missing, before JDK 16 it must be activated explicitly: MAVEN_OPTS=\"--add-modules jdk.incubator.jpackage\"");
		}
		try {
			Set<DependencyModule> dependencies = this.getResolveDependenciesTask().call();
			
			ModuleReference projectModuleReference = ModuleFinder.of(Paths.get(this.project.getBuild().getOutputDirectory())).findAll().stream().findFirst().get();
			this.projectModule = new ProjectModule(this.project, projectModuleReference.descriptor(), dependencies, this.invernoBuildPath, this.jmodsPath, ProjectModule.Classifier.CONTAINER, Set.of());
			
			this.getLog().info("Building project container image...");
			ProgressBar progressBar = this.createProgressBar();
			
			CreateProjectJmodTask createProjectJmodTask = this.getCreateProjectJmodTask(progressBar.addStep(1, 100));
			ModularizeDependenciesTask modularizeDependenciesTask = this.getModularizeDependenciesTask(progressBar.addStep(16, 100));
			CompileModuleDescriptorsTask compileModuleDescriptorsTask = this.getCompileModuleDescriptorsTask(progressBar.addStep(1, 100));
			PackageModularizedDependenciesTask packageModularizedDependenciesTask = this.getPackageModularizedDependenciesTask(progressBar.addStep(4, 100));
			CreateProjectRuntimeTask createProjectRuntimeTask = this.getCreateProjectRuntimeTask(progressBar.addStep(24, 100));
			CreateProjectApplicationTask createProjectApplicationTask = this.getCreateProjectApplicationTask(progressBar.addStep(1, 100));
			CreateProjectContainerImageTask createProjectContainerImageTask = this.getCreateProjectContainerImageTask(progressBar.addStep(53, 100));
			
			progressBar.display();
			
			createProjectJmodTask.call();
			modularizeDependenciesTask.call();
			compileModuleDescriptorsTask.call();
			packageModularizedDependenciesTask.call();
			createProjectRuntimeTask.call();
			createProjectApplicationTask.call();
			createProjectContainerImageTask.call();
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error building project package", e);
		}
	}
	
	protected CreateProjectContainerImageTask getCreateProjectContainerImageTask(ProgressBar.Step step) {
		CreateProjectContainerImageTask task = new CreateProjectContainerImageTask(this, this.projectModule);
		
		task.setVerbose(this.verbose);

		if(this.launchers != null && !this.launchers.isEmpty()) {
			task.setExecutable(this.launchers.stream()
				.map(CreateProjectApplicationTask.Launcher::getName)
				.filter(launcherName -> launcherName.equals(this.executable))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Executable " + this.executable + " does not exist in project application image"))
			);
		}
		else {
			task.setExecutable(this.project.getArtifactId());
		}
		
		task.setFrom(this.from);
		task.setRegistry(Optional.ofNullable(this.registry).filter(StringUtils::isNotEmpty));
		task.setRepository(Optional.ofNullable(this.repository).filter(StringUtils::isNotEmpty));
		task.setImageFormat(Optional.ofNullable(this.imageFormat));
		task.setLabels(Optional.ofNullable(this.labels).filter(labels -> !labels.isEmpty()));
		task.setPorts(Optional.ofNullable(this.ports).filter(ports -> !ports.isEmpty()));
		task.setVolumes(Optional.ofNullable(this.volumes).filter(volumes -> !volumes.isEmpty()));
		task.setUser(Optional.ofNullable(this.user).filter(StringUtils::isNotEmpty));
		task.setEnvironment(Optional.ofNullable(this.environment).filter(environment -> !environment.isEmpty()));
		
		task.setStep(step);
		
		return task;
	}
}
