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

import io.inverno.tool.maven.internal.MavenInvernoProject;
import java.io.File;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugins.annotations.Parameter;
import io.inverno.tool.buildtools.PackageApplicationTask;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>
 * Builds and packages the project application image.
 * </p>
 * 
 * <p>
 * A project application package is a native self-contained Java application including all the necessary dependencies. It can be used to distribute a complete application.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
@Mojo(name = "package-app", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class PackageApplicationMojo extends BuildRuntimeMojo {

	/**
	 * Skips the build and packaging of the application image.
	 */
	@Parameter(property = "inverno.app.skip", required = false)
	private boolean skip;
	
	/**
	 * The application copyright.
	 */
	@Parameter(property = "inverno.app.copyright", required = false)
	private String copyright;
	
	/**
	 * The application vendor.
	 */
	@Parameter(property = "inverno.app.vendor", defaultValue = "${project.organization.name}", required = false)
	private String vendor;
	
	/**
	 * The application's home page URL.
	 */
	@Parameter(property = "inverno.app.aboutURL", defaultValue = "${project.url}", required = false)
	private String aboutURL;
	
	/**
	 * Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on
	 * Windows.
	 */
	@Parameter(property = "inverno.app.installDirectory", required = false)
	private String installDirectory;
	
	/**
	 * The path to the application license file.
	 */
	@Parameter(property = "inverno.app.licenseFile", defaultValue = "${project.basedir}/LICENSE", required = false)
	private File licenseFile;
	
	/**
	 * The path to resources that override resulting package resources.  
	 */
	@Parameter(property = "inverno.app.resourceDirectory", required = false)
	private File resourceDirectory;
	
	/**
	 * Files to add to the application payload.
	 */
	@Parameter(property = "inverno.app.contentFiles", required = false)
	private List<File> contentFiles;
	
	/**
	 * Enables the automatic generation of launchers based on the main classes
	 * extracted from the application module.
	 * 
	 * If enabled, a launcher is generated for all main classes other than the main
	 * launcher.
	 */
	@Parameter(property = "inverno.app.automaticLaunchers", defaultValue = "false", required = false)
	private boolean automaticLaunchers;
	
	/**
	 * A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)
	 */
	@Parameter(property = "inverno.app.packageTypes", required = true)
	protected Set<String> packageTypes;
	
	
	/**
	 * Linux specific configuration.
	 */
	@Parameter(required = false)
	private LinuxConfigurationParameters linuxConfiguration;
	
	/**
	 * MacOS specific configuration.
	 */
	@Parameter(required = false)
	private MacOSConfigurationParameters macOSConfiguration;
	
	/**
	 * Windows specific configuration.
	 */
	@Parameter(required = false)
	private WindowsConfigurationParameters windowsConfiguration;
	
	/**
	 * The specific list of launchers to include in the resulting application.
	 * 
	 * The first launcher in the list will be considered as the main launcher.
	 */
	@Parameter(required = false)
	protected List<ApplicationLauncherParameters> launchers;
	
	@Override
	protected void doExecute(MavenInvernoProject project) throws Exception {
		// We need to get intermediary results as well... like a callback
		Set<Path> appArchives = new HashSet<>();
		appArchives.addAll(project
			.modularizeDependencies(this::configureTask)
			.buildJmod(this::configureTask)
			.buildRuntime(this::configureTask)
			.packageApplication(this::configureTask)
			.doOnComplete(appArchives::addAll)
			.archive(this::configureTask)
			.execute()
		);
		
		this.attachArchives(appArchives);
	}
	
	/**
	 * <p>
	 * Configures the package application task.
	 * </p>
	 * 
	 * @param packageApplicationTask the package application task
	 * 
	 * @return the package application task
	 */
	protected PackageApplicationTask configureTask(PackageApplicationTask packageApplicationTask) {
		return packageApplicationTask
			.copyright(this.copyright)
			.vendor(this.vendor)
			.aboutURL(StringUtils.isNotBlank(this.aboutURL) ? URI.create(this.aboutURL) : null)
			.installDirectory(this.installDirectory)
			.licensePath(this.licenseFile != null ? this.licenseFile.toPath().toAbsolutePath() : null)
			.resourcesPath(this.resourceDirectory != null ? this.resourceDirectory.toPath().toAbsolutePath() : null)
			.appContents(this.contentFiles != null ? this.contentFiles.stream().map(file -> file.toPath().toAbsolutePath()).collect(Collectors.toSet()) : null)
			.automaticLaunchers(this.automaticLaunchers)
			.launchers(this.launchers)
			.linuxConfiguration(this.linuxConfiguration)
			.macOSConfiguration(this.macOSConfiguration)
			.windowsConfiguration(this.windowsConfiguration)
			.types(this.packageTypes != null ? this.packageTypes.stream().map(PackageApplicationTask.PackageType::fromFormat).collect(Collectors.toSet()) : null);
	}
}
