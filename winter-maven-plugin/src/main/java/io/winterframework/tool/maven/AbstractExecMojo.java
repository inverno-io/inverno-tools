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
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import io.winterframework.tool.maven.internal.DependencyModule;
import io.winterframework.tool.maven.internal.ProgressBar;
import io.winterframework.tool.maven.internal.ProjectModule;
import io.winterframework.tool.maven.internal.task.CompileModuleDescriptorsTask;
import io.winterframework.tool.maven.internal.task.ExecuteProjectTask;
import io.winterframework.tool.maven.internal.task.ModularizeDependenciesTask;
import io.winterframework.tool.maven.internal.task.PackageModularizedDependenciesTask;
import io.winterframework.tool.maven.internal.task.ResolveDependenciesTask;

/**
 * <p>
 * Base project execution mojo
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractExecMojo extends AbstractWinterMojo {

	protected static final String PROPERTY_PID_FILE = "winter.application.pid_file";
	
	/**
	 * Skips the execution.
	 */
	@Parameter(property = "winter.exec.skip", required = false)
	private boolean skip;
	
	/**
	 * A directory containing module descriptors to use to modularize unnamed
	 * dependency modules and which override the ones that are otherwise generated.
	 */
	@Parameter(property = "winter.exec.jmodsOverrideDirectory", defaultValue = "${project.basedir}/src/jmods/", required = false)
	protected File jmodsOverrideDirectory;
	
	/**
	 * A directory containing user-editable configuration files that will be copied
	 * to the image to execute.
	 */
	@Parameter(property = "winter.exec.configurationDirectory", defaultValue = "${project.basedir}/src/main/conf/", required = false)
	protected File configurationDirectory;
	
	/**
	 * Overwrites dependencies that don't exist or are older than the source.
	 */
	@Parameter(property = "winter.exec.overWriteIfNewer", defaultValue = "true", required = false)
	protected boolean overWriteIfNewer = true;
	
	/**
	 * The main class to use to run the application. If not specified, a main class
	 * is automatically selected.
	 */
	@Parameter(property = "winter.exec.mainClass", required = false)
	protected String mainClass;
	
	/**
	 * The VM options to use when executing the application.
	 */
	@Parameter(property = "winter.exec.vmOptions", required = false)
	protected String vmOptions;
	
	/**
	 * Adds the unnamed modules when executing the application.
	 */
	@Parameter(property = "winter.exec.addUnnamedModules", defaultValue = "true", required = false)
	protected boolean addUnnamedModules = true;
	
	/**
	 * The arguments to pass to the application.
	 */
	@Parameter
	protected String arguments;
	
	/**
	 * The working directory of the application.
	 */
	@Parameter(property = "winter.run.workingDirectory", defaultValue = "${project.build.directory}/maven-winter/working", required = false)
	protected File workingDirectory;
	
	// src
	protected Optional<Path> jmodsOverridePath;
	protected Optional<Path> confPath;
	
	// target
	protected Path jmodsExplodedPath;
	protected Path jmodsPath;
	
	/*
 	 *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/javac.html
 	 */
	protected ToolProvider javac;
 	/*
 	 *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/jar.html
 	 */
	protected ToolProvider jar;
 	/*
     *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/jdeps.html
     */
	protected ToolProvider jdeps;
	
	public AbstractExecMojo() {
		ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).forEach(toolProvider -> {
			switch(toolProvider.name()) {
				case "javac": this.javac = toolProvider;
					break;
				case "jar": this.jar = toolProvider;
					break;
				case "jdeps": this.jdeps = toolProvider;
					break;
			}
		});
	}

	@Override
	protected boolean isSkip() {
		return this.skip;
	}
	
	@Override
	public void doExecute() throws MojoExecutionException, MojoFailureException {
		try {
			Set<DependencyModule> dependencies = this.getResolveDependenciesTask().call();
			
			ModuleReference projectModuleReference = ModuleFinder.of(Paths.get(this.project.getBuild().getOutputDirectory())).findAll().stream().findFirst().get();
			ProjectModule projectModule = new ProjectModule(this.project, projectModuleReference.descriptor(), dependencies, this.winterBuildPath, this.jmodsPath, ProjectModule.Classifier.RUNTIME, Set.of());

			this.getLog().info("Running project: " + projectModule + "...");
			
			if(dependencies.stream().anyMatch(dependency -> dependency.isMarked())) {
				ProgressBar progressBar = this.createProgressBar();
				
				ModularizeDependenciesTask modularizeDependenciesTask = this.getModularizeDependenciesTask(progressBar.addStep(74, 100), projectModule);
				CompileModuleDescriptorsTask compileModuleDescriptorsTask = this.getCompileModuleDescriptorsTask(progressBar.addStep(6, 100), projectModule);
				PackageModularizedDependenciesTask packageModularizedDependenciesTask = this.getPackageModularizedDependenciesTask(progressBar.addStep(10, 100), projectModule);

				progressBar.display();
				
				modularizeDependenciesTask.call();
				compileModuleDescriptorsTask.call();
				packageModularizedDependenciesTask.call();
			}
			else {
				if(this.verbose) {
					this.getLog().info("[ Project dependencies are up to date ]");
				}
			}
			this.handleProcess(projectModule, this.getExecuteProjectTask(projectModule).call());
		}
		catch (MojoExecutionException | MojoFailureException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error running project", e);
		}
		
	}
	
	protected abstract void handleProcess(ProjectModule projectModule, Process proc) throws MojoExecutionException, MojoFailureException;
	
	protected void initializePaths() throws IOException {
		super.initializePaths();
		
		this.jmodsOverridePath = Optional.ofNullable(this.jmodsOverrideDirectory).map(f -> f.toPath().toAbsolutePath());
		this.confPath = Optional.ofNullable(this.configurationDirectory).map(f -> f.toPath().toAbsolutePath());
		
		this.jmodsExplodedPath = this.winterBuildPath.resolve("jmods-exploded").toAbsolutePath();
		Files.createDirectories(this.jmodsExplodedPath);
		
		this.jmodsPath = this.winterBuildPath.resolve("jmods").toAbsolutePath();
		Files.createDirectories(this.jmodsPath);
	}
	
	private ResolveDependenciesTask getResolveDependenciesTask() {
		ResolveDependenciesTask task = new ResolveDependenciesTask(this, this.project.getArtifacts(), this.jmodsOverridePath, this.jmodsExplodedPath, this.jmodsPath);
		
		task.setVerbose(this.verbose);
		
		return task;
	}
	
	private ModularizeDependenciesTask getModularizeDependenciesTask(ProgressBar.Step step, ProjectModule projectModule) {
		ModularizeDependenciesTask task = new ModularizeDependenciesTask(this, this.jdeps, projectModule, this.jmodsExplodedPath);
		
		task.setVerbose(this.verbose);
		task.setStep(step);
		
		return task;
	}
	
	private CompileModuleDescriptorsTask getCompileModuleDescriptorsTask(ProgressBar.Step step, ProjectModule projectModule) {
		CompileModuleDescriptorsTask task = new CompileModuleDescriptorsTask(this, this.javac, projectModule, this.jmodsExplodedPath);
		
		task.setVerbose(this.verbose);
		task.setStep(step);
		
		return task;
	}
	
	private PackageModularizedDependenciesTask getPackageModularizedDependenciesTask(ProgressBar.Step step, ProjectModule projectModule) {
		PackageModularizedDependenciesTask task = new PackageModularizedDependenciesTask(this, this.jar, projectModule);
		
		task.setVerbose(this.verbose);
		task.setStep(step);
		
		return task;
	}
	
	protected ExecuteProjectTask getExecuteProjectTask(ProjectModule projectModule) {
		ExecuteProjectTask task = new ExecuteProjectTask(this, projectModule);

		String pidfileVmOption = "-D" + PROPERTY_PID_FILE + "=" + projectModule.getPidfile();
		
		task.setVerbose(this.verbose);
		
		task.setMainClass(Optional.ofNullable(this.mainClass).filter(StringUtils::isNotEmpty));
		task.setArguments(Optional.ofNullable(this.arguments).filter(StringUtils::isNotEmpty));
		task.setVmOptions(Optional.ofNullable(this.vmOptions).map(vmOptions -> pidfileVmOption + " " + vmOptions).or(() -> Optional.of(pidfileVmOption)));
		task.setWorkingPath(Optional.ofNullable(this.workingDirectory).filter(File::exists).map(file -> file.toPath().toAbsolutePath()));
		task.setAddUnnamedModules(this.addUnnamedModules);
		
		return task;
	}
}
