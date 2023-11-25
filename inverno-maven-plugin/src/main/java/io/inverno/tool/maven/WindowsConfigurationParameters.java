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
import java.net.URI;
import java.util.Optional;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * <p>
 * Parameters for the creation a Windows application package.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class WindowsConfigurationParameters implements PackageApplicationTask.WindowsConfiguration {

	/**
	 * Creates a console launcher for the application, should be specified for application which requires console interactions.
	 */
	@Parameter(property = "inverno.app.windows.console", required = false)
	private boolean console;

	/**
	 * Adds a dialog to enable the user to choose a directory in which the product is installed.
	 */
	@Parameter(property = "inverno.app.windows.dirChooser", required = false)
	private boolean dirChooser;

	/**
	 * Adds the application to the system menu.
	 */
	@Parameter(property = "inverno.app.windows.menu", required = false)
	private boolean menu;

	/**
	 * Start Menu group this application is placed in.
	 */
	@Parameter(property = "inverno.app.windows.menuGroup", required = false)
	private String menuGroup;

	/**
	 * Requests to perform an install on a per-user basis.
	 */
	@Parameter(property = "inverno.app.windows.perUserInstall", required = false)
	private boolean perUserInstall;

	/**
	 * Creates a desktop shortcut for the application.
	 */
	@Parameter(property = "inverno.app.windows.shortcut", required = false)
	private boolean shortcut;

	
	/**
	 * Adds a dialog to enable the user to choose if shortcuts will be created by installer.
	 */
	@Parameter(property = "inverno.app.windows.shortcutPrompt", required = false)
	private boolean shortcutPrompt;

	/**
	 * URL of available application update information.
	 */
	@Parameter(property = "inverno.app.windows.updateURL", required = false)
	private String updateURL;
	
	/**
	 * UUID associated with upgrades for this package.
	 */
	@Parameter(property = "inverno.app.windows.upgradeUUID", required = false)
	private String upgradeUUID;

	/**
	 * <p>
	 * Sets whether the application should be started with a console launcher.
	 * </p>
	 * 
	 * @param console true to start with a console launcher, false otherwise
	 */
	public void setConsole(boolean console) {
		this.console = console;
	}
	
	@Override
	public boolean isConsole() {
		return this.console;
	}

	/**
	 * <p>
	 * Sets whether user should be enabled to choose a directory in which the product is installed.
	 * </p>
	 * 
	 * @param dirChooser true to add a directory chooser, false otherwise
	 */
	public void setDirChooser(boolean dirChooser) {
		this.dirChooser = dirChooser;
	}

	@Override
	public boolean isDirChooser() {
		return this.dirChooser;
	}

	/**
	 * <p>
	 * Sets whether a Start Menu shortcut should be added for the application.
	 * </p>
	 * 
	 * @param menu true to add a start menu shortcut, false otherwise
	 */
	public void setMenu(boolean menu) {
		this.menu = menu;
	}

	@Override
	public boolean isMenu() {
		return this.menu;
	}

	/**
	 * <p>
	 * Sets the Start Menu group the application is placed in.
	 * </p>
	 * 
	 * @param menuGroup the Start menu group
	 */
	public void setMenuGroup(String menuGroup) {
		this.menuGroup = menuGroup;
	}

	@Override
	public Optional<String> getMenuGroup() {
		return Optional.ofNullable(this.menuGroup).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets whether the application is installed per-user.
	 * </p>
	 * 
	 * @param perUserInstall true to install the application per-user, false otherwise
	 */
	public void setPerUserInstall(boolean perUserInstall) {
		this.perUserInstall = perUserInstall;
	}
	
	@Override
	public boolean isPerUserInstall() {
		return this.perUserInstall;
	}

	/**
	 * <p>
	 * Sets whether a shortcut should be created for the application.
	 * </p>
	 * 
	 * @param shortcut true to created a shortcut, false otherwise
	 */
	public void setShortcut(boolean shortcut) {
		this.shortcut = shortcut;
	}

	@Override
	public boolean isShortcut() {
		return this.shortcut;
	}
	
	/**
	 * <p>
	 * Sets whether user should be enabled to choose if shortcuts will be created by installer.
	 * </p>
	 * 
	 * @param shortcutPrompt true to let the user choose whether shortcuts should be added, false otherwise
	 */
	public void setShortcutPrompt(boolean shortcutPrompt) {
		this.shortcutPrompt = shortcutPrompt;
	}

	@Override
	public boolean isShortcutPrompt() {
		return this.shortcutPrompt;
	}

	/**
	 * <p>
	 * Sets the URL of available application update information.
	 * </p>
	 * 
	 * @param updateURL the update URL
	 */
	public void setUpdateURL(String updateURL) {
		this.updateURL = updateURL;
	}
	
	@Override
	public Optional<URI> getUpdateURL() {
		return Optional.ofNullable(this.updateURL).map(URI::create).map(URI::normalize);
	}
	
	/**
	 * <p>
	 * Sets the UUID associated with upgrades for this package.
	 * </p>
	 * 
	 * @param upgradeUUID the upgrade UUID
	 */
	public void setUpgradeUUID(String upgradeUUID) {
		this.upgradeUUID = upgradeUUID;
	}

	@Override
	public Optional<String> getUpgradeUUID() {
		return Optional.ofNullable(this.upgradeUUID).filter(StringUtils::isNotEmpty);
	}
}
