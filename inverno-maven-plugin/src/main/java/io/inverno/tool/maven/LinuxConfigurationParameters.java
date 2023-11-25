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
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Parameters for the creation a Linux application package.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class LinuxConfigurationParameters implements PackageApplicationTask.LinuxConfiguration {

	/**
	 * Name for Linux package, defaults to the application name.
	 */
	@Parameter(property = "inverno.app.linux.packageName", required = false)
	private String packageName;
	
	/**
	 * Maintainer for .deb bundle.
	 */
	@Parameter(property = "inverno.app.linux.debMaintainer", defaultValue="", required = false)
	private String debMaintainer;
	
	/**
	 * Menu group this application is placed in.
	 */
	@Parameter(property = "inverno.app.linux.menuGroup", required = false)
	private String menuGroup;

	/**
	 * Required packages or capabilities for the application.
	 */
	@Parameter(property = "inverno.app.linux.packageDeps", required = false)
	private String packageDeps;

	/**
	 * Type of the license ("License: {@literal <value>} of the RPM .spec).
	 */
	@Parameter(property = "inverno.app.linux.rpmLicenseType", required = false)
	private String rpmLicenseType;

	/**
	 * Release value of the RPM <name>.spec file or Debian revision value of the DEB control file.
	 */
	@Parameter(property = "inverno.app.linux.appRelease", required = false)
	private String appRelease;

	/**
	 * Group value of the RPM <name>.spec file or Section value of DEB control file.
	 */
	@Parameter(property = "inverno.app.linux.appCategory", required = false)
	private String appCategory;

	/**
	 * Creates a shortcut for the application
	 */
	@Parameter(property = "inverno.app.linux.shortcut", required = false)
	private boolean shortcut;

	/**
	 * <p>
	 * Sets the name of the linux package.
	 * </p>
	 * 
	 * @param packageName the package name
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
	@Override
	public Optional<String> getPackageName() {
		return Optional.ofNullable(this.packageName).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the Debian package maintainer.
	 * </p>
	 * 
	 * @param debMaintainer the Debian package maintainer
	 */
	public void setDebMaintainer(String debMaintainer) {
		this.debMaintainer = debMaintainer;
	}
	
	@Override
	public Optional<String> getDebMaintainer() {
		return Optional.ofNullable(this.debMaintainer).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the Menu group this application is placed in.
	 * </p>
	 * 
	 * @param menuGroup the menu group
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
	 * Sets the required packages or capabilities for the application.
	 * </p>
	 * 
	 * @param packageDeps the package dependencies
	 */
	public void setPackageDeps(String packageDeps) {
		this.packageDeps = packageDeps;
	}
	
	@Override
	public Optional<String> getPackageDeps() {
		return Optional.ofNullable(this.packageDeps).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the type of the license ("License: value" of the RPM .spec).
	 * </p>
	 * 
	 * @param rpmLicenseType the RPM license type
	 */
	public void setRpmLicenseType(String rpmLicenseType) {
		this.rpmLicenseType = rpmLicenseType;
	}

	@Override
	public Optional<String> getRpmLicenseType() {
		return Optional.ofNullable(this.rpmLicenseType).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the release value of the RPM {@code <name>.spec} file or Debian revision value of the DEB control file.
	 * </p>
	 * 
	 * @param appRelease the application release
	 */
	public void setAppRelease(String appRelease) {
		this.appRelease = appRelease;
	}

	@Override
	public Optional<String> getAppRelease() {
		return Optional.ofNullable(this.appRelease).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the group value of the RPM /.spec file or Section value of DEB control file.
	 * </p>
	 * 
	 * @param appCategory the application category
	 */
	public void setAppCategory(String appCategory) {
		this.appCategory = appCategory;
	}

	@Override
	public Optional<String> getAppCategory() {
		return Optional.ofNullable(this.appCategory).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets whether a shortcut must be created when installing the application.
	 * </p>
	 * 
	 * @param shortcut true ti create a shortcut, false otherwise
	 */
	public void setShortcut(boolean shortcut) {
		this.shortcut = shortcut;
	}

	@Override
	public boolean isShortcut() {
		return this.shortcut;
	}
}
