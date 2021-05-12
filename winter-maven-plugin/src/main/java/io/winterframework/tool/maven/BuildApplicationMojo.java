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
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.StringUtils;

import io.winterframework.tool.maven.internal.DependencyModule;
import io.winterframework.tool.maven.internal.ProgressBar;
import io.winterframework.tool.maven.internal.ProjectModule;
import io.winterframework.tool.maven.internal.task.CompileModuleDescriptorsTask;
import io.winterframework.tool.maven.internal.task.CreateImageArchivesTask;
import io.winterframework.tool.maven.internal.task.CreateProjectApplicationTask;
import io.winterframework.tool.maven.internal.task.CreateProjectJmodTask;
import io.winterframework.tool.maven.internal.task.CreateProjectRuntimeTask;
import io.winterframework.tool.maven.internal.task.ModularizeDependenciesTask;
import io.winterframework.tool.maven.internal.task.PackageModularizedDependenciesTask;

/**
 * <p>
 * Builds the project application package.
 * </p>
 * 
 * <p>
 * A project application package is a native self-contained Java application
 * including all the necessary dependencies. It can be used to distribute a
 * complete application.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "build-app", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class BuildApplicationMojo extends BuildRuntimeMojo {

	/**
	 * Skips the generation of the application.
	 */
	@Parameter(property = "winter.app.skip", required = false)
	private boolean skip;
	
	/**
	 * The application copyright.
	 */
	@Parameter(property = "winter.app.copyright", required = false)
	private String copyright;
	
	/**
	 * The description of the application.
	 */
	@Parameter(property = "winter.app.description", defaultValue = "${project.description}", required = false)
	private String description;
	
	/**
	 * The application vendor.
	 */
	@Parameter(property = "winter.app.vendor", defaultValue = "${project.organization.name}", required = false)
	private String vendor;
	
	/**
	 * The path to the application license file.
	 */
	@Parameter(property = "winter.app.licenseFile", defaultValue = "${project.basedir}/LICENSE", required = false)
	private File licenseFile;
	
	/**
	 * The path to resources that override resulting package resources.  
	 */
	@Parameter(property = "winter.app.resourceDirectory", required = false)
	private File resourceDirectory;
	
	/**
	 * Absolute path of the installation directory of the application on OS X or
	 * Linux. Relative sub-path of the installation location of the application such
	 * as "Program Files" or "AppData" on Windows.
	 */
	@Parameter(property = "winter.app.installDirectory", required = false)
	private String installDirectory;
	
	/**
	 * Linux specific configuration.
	 */
	@Parameter(required = false)
	private CreateProjectApplicationTask.LinuxConfiguration linuxConfiguration;
	
	/**
	 * MacOS specific configuration.
	 */
	@Parameter(required = false)
	private CreateProjectApplicationTask.MacOSConfiguration macOSConfiguration;
	
	/**
	 * Windows specific configuration.
	 */
	@Parameter(required = false)
	private CreateProjectApplicationTask.WindowsConfiguration windowsConfiguration;
	
	/**
	 * Enables the automatic generation of launchers based on the main classes
	 * extracted from the application module.
	 * 
	 * If enabled, a launcher is generated for all main classes other than the main
	 * launcher.
	 */
	@Parameter(property = "winter.app.automaticLaunchers", defaultValue = "false", required = false)
	private boolean automaticLaunchers;
	
	/**
	 * A list of extra launchers to include in the resulting application.
	 */
	@Parameter(required = false)
	protected List<CreateProjectApplicationTask.Launcher> launchers;
	
	@Override
	protected void doExecute() throws MojoExecutionException, MojoFailureException {
		if(this.jpackage == null) {
			throw new MojoExecutionException("'jdk.jpackage' module is missing, before JDK 16 it must be activated explicitly: MAVEN_OPTS=\"--add-modules jdk.incubator.jpackage\"");
		}
		try {
			Set<DependencyModule> dependencies = this.getResolveDependenciesTask().call();
			
			ModuleReference projectModuleReference = ModuleFinder.of(Paths.get(this.project.getBuild().getOutputDirectory())).findAll().stream().findFirst().get();
			this.projectModule = new ProjectModule(this.project, projectModuleReference.descriptor(), dependencies, this.winterBuildPath, this.jmodsPath, ProjectModule.Classifier.APPLICATION, this.formats);
			
			this.getLog().info("Building application image: " + this.projectModule.getApplicationImagePath() + "...");
			ProgressBar progressBar = this.createProgressBar();
			
			CreateProjectJmodTask createProjectJmodTask = this.getCreateProjectJmodTask(progressBar.addStep(1, 100));
			ModularizeDependenciesTask modularizeDependenciesTask = this.getModularizeDependenciesTask(progressBar.addStep(20, 100));
			CompileModuleDescriptorsTask compileModuleDescriptorsTask = this.getCompileModuleDescriptorsTask(progressBar.addStep(1, 100));
			PackageModularizedDependenciesTask packageModularizedDependenciesTask = this.getPackageModularizedDependenciesTask(progressBar.addStep(10, 100));
			CreateProjectRuntimeTask createProjectRuntimeTask = this.getCreateProjectRuntimeTask(progressBar.addStep(34, 100));
			CreateProjectApplicationTask createProjectApplicationTask = this.getCreateProjectApplicationTask(progressBar.addStep(1, 100));
			CreateImageArchivesTask createImageArchivesTask = this.getCreateImageArchivesTask(progressBar.addStep(33, 100));
			
			progressBar.display();
			
			createProjectJmodTask.call();
			modularizeDependenciesTask.call();
			compileModuleDescriptorsTask.call();
			packageModularizedDependenciesTask.call();
			createProjectRuntimeTask.call();
			createProjectApplicationTask.call();
			createImageArchivesTask.call();
			
			if(this.attach) {
				for(Entry<String, Path> e : this.projectModule.getImageArchivesPaths().entrySet()) {
					this.projectHelper.attachArtifact(this.project, e.getKey(), this.projectModule.getClassifier().getClassifier(), e.getValue().toFile());
				}
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error building application image", e);
		}
	}
	
	protected CreateImageArchivesTask getCreateImageArchivesTask(ProgressBar.Step step) {
		CreateImageArchivesTask task = new CreateImageArchivesTask(this, this.projectModule, projectModule.getApplicationImagePath());
		
		task.setVerbose(this.verbose);
		
		task.setPrefix(this.project.getBuild().getFinalName() + File.separator);
		task.setExcludeFormats(CreateProjectApplicationTask.JPACKAGE_TYPES);
		task.setStep(step);
		
		return task;
	}
	
	protected CreateProjectApplicationTask getCreateProjectApplicationTask(ProgressBar.Step step) {
		CreateProjectApplicationTask task = new CreateProjectApplicationTask(this, this.jpackage, this.projectModule, this.launchersPath);
		
		task.setVerbose(this.verbose);
		
		task.setAutomaticLaunchers(this.automaticLaunchers);
		task.setLaunchers(this.launchers);
//		task.setArguments(Optional.ofNullable(this.arguments).filter(StringUtils::isNotEmpty));
		task.setCopyright(Optional.ofNullable(this.copyright).filter(StringUtils::isNotEmpty));
		task.setDescription(Optional.ofNullable(this.description).filter(StringUtils::isNotEmpty));
//		task.setIconPath(Optional.ofNullable(this.iconFile).map(file -> file.toPath().toAbsolutePath()));
		task.setInstallDirectory(Optional.ofNullable(this.installDirectory).filter(StringUtils::isNotEmpty));
		task.setLaunchers(this.launchers);
		task.setLicensePath(Optional.ofNullable(this.licenseFile).map(file -> file.toPath().toAbsolutePath()));
//		task.setMainClass(Optional.ofNullable(this.mainClass).filter(StringUtils::isNotEmpty));
//		task.setModule(Optional.ofNullable(this.module).filter(StringUtils::isNotEmpty));
//		task.setName(this.name);
		task.setResourcePath(Optional.ofNullable(this.resourceDirectory).map(file -> file.toPath().toAbsolutePath()));
		task.setVendor(Optional.ofNullable(this.vendor).filter(StringUtils::isNotEmpty));
//		task.setVmOptions(Optional.ofNullable(this.vmOptions).filter(StringUtils::isNotEmpty));
		task.setLinuxConfiguration(Optional.ofNullable(this.linuxConfiguration));
		task.setMacOSConfiguration(Optional.ofNullable(this.macOSConfiguration));
		task.setWindowsConfiguration(Optional.ofNullable(this.windowsConfiguration));
//		task.setAddUnnamedModules(this.addUnnamedModules);
		
		task.setStep(step);
		
		return task;
	}
}
