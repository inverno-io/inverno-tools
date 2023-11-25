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

import io.inverno.tool.buildtools.PackageApplicationTask;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * <p>
 * Parameters for the creation of an application launcher.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class ApplicationLauncherParameters implements PackageApplicationTask.Launcher {

	/**
	 * The name of the application launcher.
	 */
	@Parameter(required = false)
	private String name;

	/**
	 * The description of the application launcher.
	 */
	@Parameter(required = false)
	private String description;
	
	/**
	 * The module containing the main class of the application launcher. If not specified the project's module is selected.
	 */
	@Parameter(required = false)
	private String module;

	/**
	 * The main class of the application launcher. If not specified the specified module must provide a main class.
	 */
	@Parameter(required = false)
	private String mainClass;

	/**
	 *  The VM options to use when executing the application launcher. 
	 */
	@Parameter(defaultValue = "-Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO", required = false)
	private String vmOptions = "-Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO";

	/**
	 * The default arguments to pass to the application launcher 
	 */
	@Parameter(required = false)
	private String arguments;

	/**
	 * The path to the application launcher icon file. 
	 */
	@Parameter(required = false)
	private File iconFile;
	
	/**
	 * The application launcher version.
	 */
	@Parameter(required = false)
	private String appVersion;

	/**
	 * Adds the unnamed modules when running the launcher.
	 */
	@Parameter(defaultValue = "true", required = true)
	private boolean addUnnamedModules = true;

	/**
	 * Registers the application launcher as a background service-type application.
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean launcherAsService;
	
	/**
	 * Creates a console for the application launcher, should be specified for application which requires console interactions.
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean winConsole;
	
	/**
	 * Adds a desktop shortcut for this application launcher.
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean winShortcut;
	
	/**
	 * Adds a Start Menu shortcut for this application launcher.
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean winMenu;
	
	/**
	 * The application launcher application category.
	 */
	@Parameter(required = true)
	private String linuxAppCategory;
	
	/**
	 * Adds a shortcut for this application launcher.
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean linuxShortcut;

	/**
	 * <p>
	 * Sets the name of the application launcher.
	 * </p>
	 * 
	 * @param name the name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public Optional<String> getName() {
		return Optional.ofNullable(this.name).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the description of the application launcher.
	 * </p>
	 * 
	 * @param description the description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Optional<String> getDescription() {
		return Optional.ofNullable(this.description).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the name of the module defining the main class executed with the application launcher.
	 * </p>
	 * 
	 * @param module the module
	 */
	public void setModule(String module) {
		this.module = module;
	}

	@Override
	public Optional<String> getModule() {
		return Optional.ofNullable(this.module).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the canonical name of the main class executed with the application launcher.
	 * </p>
	 * 
	 * @param mainClass the main class
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	@Override
	public Optional<String> getMainClass() {
		return Optional.ofNullable(this.mainClass).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the VM options to use when executing the application launcher.
	 * </p>
	 * 
	 * @param vmOptions the VM options
	 */
	public void setVmOptions(String vmOptions) {
		this.vmOptions = vmOptions;
	}

	@Override
	public Optional<String> getVmOptions() {
		return Optional.ofNullable(this.vmOptions).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the default arguments to pass to the application launcher.
	 * </p>
	 * 
	 * @param arguments the arguments
	 */
	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	@Override
	public Optional<String> getArguments() {
		return Optional.ofNullable(this.arguments).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the path to the application launcher icon file.
	 * </p>
	 * 
	 * @param iconFile the path to the icon file
	 */
	public void setIconFile(File iconFile) {
		this.iconFile = iconFile;
	}

	@Override
	public Optional<Path> getIconPath() {
		return Optional.ofNullable(this.iconFile).map(file -> file.toPath().toAbsolutePath());
	}

	/**
	 * <p>
	 * Sets the application launcher version.
	 * </p>
	 * 
	 * @param appVersion the launcher version
	 */
	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	@Override
	public Optional<String> getAppVersion() {
		return Optional.ofNullable(this.appVersion).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets whether unnamed modules should be added when executing the application launcher.
	 * </p>
	 * 
	 * @param addUnnamedModules true to add unnamed module, false otherwise
	 */
	public void setAddUnnamedModules(boolean addUnnamedModules) {
		this.addUnnamedModules = addUnnamedModules;
	}

	@Override
	public boolean isAddUnnamedModules() {
		return this.addUnnamedModules;
	}

	/**
	 * <p>
	 * Sets whether the application launcher should be registered as a background service-type application.
	 * </p>
	 * 
	 * @param launcherAsService true to create a service-type application launcher, false otherwise
	 */
	public void setLauncherAsService(boolean launcherAsService) {
		this.launcherAsService = launcherAsService;
	}

	@Override
	public boolean isLauncherAsService() {
		return this.launcherAsService;
	}

	/**
	 * <p>
	 * Sets whether the application launcher should be started in a console to enable console interaction.
	 * </p>
	 * 
	 * @param winConsole true to create a console launcher, false otherwise
	 */
	public void setWinConsole(boolean winConsole) {
		this.winConsole = winConsole;
	}

	@Override
	public boolean isWinConsole() {
		return this.winConsole;
	}

	/**
	 * <p>
	 * Sets whether a shortcut should be created for the application launcher.
	 * </p>
	 * 
	 * @param winShortcut true to create a shortcut, false otherwise
	 */
	public void setWinShortcut(boolean winShortcut) {
		this.winShortcut = winShortcut;
	}

	@Override
	public boolean isWinShortcut() {
		return this.winShortcut;
	}

	/**
	 * <p>
	 * Sets whether a Start Menu shortcut should be added for the application launcher.
	 * </p>
	 * 
	 * @param winMenu true to add a Start Menu shortcut, false otherwise
	 */
	public void setWinMenu(boolean winMenu) {
		this.winMenu = winMenu;
	}

	@Override
	public boolean isWinMenu() {
		return this.winMenu;
	}

	/**
	 * <p>
	 * Sets the group value of the RPM /.spec file or Section value of DEB control file.
	 * </p>
	 * 
	 * @param linuxAppCategory the linux category
	 */
	public void setLinuxAppCategory(String linuxAppCategory) {
		this.linuxAppCategory = linuxAppCategory;
	}
	
	@Override
	public Optional<String> getLinuxAppCategory() {
		return Optional.ofNullable(this.linuxAppCategory).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets whether a shortcut must be created when installing the application launcher.
	 * </p>
	 * 
	 * @param linuxShortcut true to create a shortcut, false otherwise
	 */
	public void setLinuxShortcut(boolean linuxShortcut) {
		this.linuxShortcut = linuxShortcut;
	}

	@Override
	public boolean isLinuxShortcut() {
		return this.linuxShortcut;
	}
}
