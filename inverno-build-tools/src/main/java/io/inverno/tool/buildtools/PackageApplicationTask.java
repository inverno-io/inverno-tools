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
package io.inverno.tool.buildtools;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * A task for packaging modular applications with an optimized Java runtime.
 * </p>
 * 
 * <p>
 * A project application package is a native self-contained Java application including all the necessary dependencies. It can be used to distribute full package of application including executables, 
 * legal notices, configuration, documentation... It can be generated to various OS specific formats (see {@link PackageApplicationTask.PackageType}), the {@link ArchiveTask} can also be chained 
 * after this task to package it to portable archives.
 * </p>
 * 
 * <p>
 * This task requires a runtime and depends on {@link BuildRuntimeTask}.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface PackageApplicationTask extends Task<Set<Image>, PackageApplicationTask> {
	
	/**
	 * <p>
	 * The types of package that the task can generate.
	 * </p>
	 * 
	 * <p>
	 * These types are platform specific and requires the task to run on corresponding operating systems.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	enum PackageType {
		/**
		 * <p>
		 * Packages the application as Windows executable.
		 * </p>
		 */
		EXE,
		/**
		 * <p>
		 * Packages the application as a Microsoft Installer file.
		 * </p>
		 */
		MSI,
		/**
		 * <p>
		 * Packages the application as a RedHat Package Manager package.
		 * </p>
		 */
		RPM,
		/**
		 * <p>
		 * Packages the application as a Debian package.
		 * </p>
		 */
		DEB,
		/**
		 * <p>
		 * Packages the application as a Package package.
		 * </p>
		 */
		PKG,
		/**
		 * <p>
		 * Packages the application as a Apple disk image package.
		 * </p>
		 */
		DMG;
		
		/**
		 * <p>
		 * Returns the file format of file extension corresponding to the package type.
		 * </p>
		 * 
		 * @return a fileformat
		 */
		public String getFormat() {
			return this.toString().toLowerCase();
		}

		/**
		 * <p>
		 * Returns the package type corresponding to the specified file format.
		 * </p>
		 * 
		 * @param format a file format
		 * 
		 * @return a package type
		 * 
		 * @throws IllegalArgumentException if the specified file format does correspond to any supported package type
		 */
		public static PackageType fromFormat(String format) {
			return PackageType.valueOf(format.toUpperCase());
		}
	}
	
	/**
	 * <p>
	 * Sets the copypright for the application.
	 * </p>
	 * 
	 * @param copyright a copyright
	 * 
	 * @return the task
	 */
	PackageApplicationTask copyright(String copyright);
	
	/**
	 * <p>
	 * Sets the vendor of the application.
	 * </p>
	 * 
	 * @param vendor a vendor
	 * 
	 * @return the task
	 */
	PackageApplicationTask vendor(String vendor);
	
	/**
	 * <p>
	 * Sets the application's home page URL.
	 * </p>
	 * 
	 * @param aboutURL a URI
	 * 
	 * @return the task
	 */
	PackageApplicationTask aboutURL(URI aboutURL);
	
	/**
	 * <p>
	 * Sets the path of the installation directory of the application.
	 * </p>
	 * 
	 * @param installDirectory the install directory
	 * 
	 * @return the task
	 */
	PackageApplicationTask installDirectory(String installDirectory);
	
	/**
	 * <p>
	 * Sets the path to the license file.
	 * </p>
	 * 
	 * @param licensePath the path to the license file
	 * 
	 * @return the task
	 */
	PackageApplicationTask licensePath(Path licensePath);
	
	/**
	 * <p>
	 * Sets the path to the resources overriding application package resources.
	 * </p>
	 * 
	 * @param resourcesPath the path to the overriding resources
	 * 
	 * @return the task
	 */
	PackageApplicationTask resourcesPath(Path resourcesPath);
	
	/**
	 * <p>
	 * Sets paths to files and/or directories to add to the application package.
	 * </p>
	 * 
	 * @param appContents a set of paths
	 * 
	 * @return the task
	 */
	PackageApplicationTask appContents(Set<Path> appContents);
	
	/**
	 * <p>
	 * Sets whether the task should automatically generate launchers based on the main classes extracted from the application module.
	 * </p>
	 * 
	 * <p>
	 * When enabled, a launcher is generated for each main class other than the main launcher class defined in the project module.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code true}.
	 * </p>
	 * 
	 * @param automaticLaunchers true to enable automatic launchers generation, false otherwise
	 * 
	 * @return the task
	 */
	PackageApplicationTask automaticLaunchers(boolean automaticLaunchers);
	
	/**
	 * <p>
	 * Sets the launchers to generate in the application package.
	 * </p>
	 * 
	 * <p>
	 * At least one launcher is required to generate an application package, if none are specified, the main class specified when generating the project JMOD archive is chosen, if none was 
	 * specified, then first main class found in the module is chosen. If no main class could be resolved the task execution will fail with a {@link TaskExecutionException}.
	 * </p>
	 * 
	 * @param launchers a list of launchers
	 * 
	 * @return the task
	 */
	PackageApplicationTask launchers(List<? extends Launcher> launchers);
	
	/**
	 * <p>
	 * Sets Linux specific configuration.
	 * </p>
	 * 
	 * @param linuxConfiguration a Linux configuration
	 * 
	 * @return the task
	 */
	PackageApplicationTask linuxConfiguration(LinuxConfiguration linuxConfiguration);
	
	/**
	 * <p>
	 * Sets MacOS specific configuration.
	 * </p>
	 * 
	 * @param macOSConfiguration a MacOS configuration
	 * 
	 * @return the task
	 */
	PackageApplicationTask macOSConfiguration(MacOSConfiguration macOSConfiguration);
	
	/**
	 * <p>
	 * Sets Windows specific configuration.
	 * </p>
	 * 
	 * @param windowsConfiguration a Windows configuration
	 * 
	 * @return the task
	 */
	PackageApplicationTask windowsConfiguration(WindowsConfiguration windowsConfiguration);

	/**
	 * <p>
	 * Sets the type of package to generate.
	 * </p>
	 * 
	 * @param types a set of package types
	 * 
	 * @return the task
	 */
	PackageApplicationTask types(Set<PackageType> types);
	
	/**
	 * <p>
	 * Creates an archive task.
	 * </p>
	 * 
	 * @return an archive task
	 */
	ArchiveTask archive();
	
	/**
	 * <p>
	 * Creates and configures an archive task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured archive task
	 */
	default ArchiveTask archive(Consumer<ArchiveTask> configurer) {
		ArchiveTask archive = this.archive();
		configurer.accept(archive);
		return archive;
	}
	
	/**
	 * <p>
	 * Creates a containerize task.
	 * </p>
	 * 
	 * @return a containerize task
	 */
	ContainerizeTask containerize();
	
	/**
	 * <p>
	 * Creates and configures a containerize task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a containerize task
	 */
	default ContainerizeTask containerize(Consumer<ContainerizeTask> configurer) {
		ContainerizeTask containerize = this.containerize();
		configurer.accept(containerize);
		return containerize;
	}
	
	/**
	 * <p>
	 * Linux specific configuration.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface LinuxConfiguration {
		
		/**
		 * <p>
		 * Returns the name of the linux package
		 * </p>
		 * 
		 * @return an optional returning the package name or an empty optional
		 */
		default Optional<String> getPackageName() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the Debian package maintainer.
		 * </p>
		 * 
		 * @return an optional returning the Debian package maintainer or an empty optional
		 */
		default Optional<String> getDebMaintainer() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the Menu group this application is placed in.
		 * </p>
		 * 
		 * @return an optional returning the Menu group or an empty optional
		 */
		default Optional<String> getMenuGroup() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the required packages or capabilities for the application.
		 * </p>
		 *
		 * @return an optional returning the required packages or capabilities for the application or an empty optional
		 */
		default Optional<String> getPackageDeps() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the type of the license ("License: value" of the RPM .spec).
		 * </p>
		 * 
		 * @return an optional returning the type of the license or an empty optional
		 */
		default Optional<String> getRpmLicenseType() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the release value of the RPM {@code <name>.spec} file or Debian revision value of the DEB control file.
		 * </p>
		 * 
		 * @return an optional returning the release value or an empty optional
		 */
		default Optional<String> getAppRelease() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the group value of the RPM /.spec file or Section value of DEB control file.
		 * </p>
		 * 
		 * @return an optional returning the group value or an empty optional
		 */
		default Optional<String> getAppCategory() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Determines whether a shortcut must be created when installing the application.
		 * </p>
		 * 
		 * @return true to create a shortcut, false otherwise
		 */
		default boolean isShortcut() {
			return false;
		}
	}
	
	/**
	 * <p>
	 * MacOS specific configuration.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface MacOSConfiguration {
		
		/**
		 * <p>
		 * Returns the identifier that uniquely identifies the application for macOS.
		 * </p>
		 * 
		 * @return an optional returning the package identifier or an empty optional 
		 */
		default Optional<String> getPackageIdentifier() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the name of the application as it appears in the Menu Bar.
		 * </p>
		 * 
		 * @return an optional returning the package name or an empty optional 
		 */
		default Optional<String> getPackageName() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the value prefixed to all components that need to be signed that don't have an existing package identifier when signing the application package.
		 * </p>
		 * 
		 * @return an optional returning the signing prefix or an empty optional
		 */
		default Optional<String> getPackageSigningPrefix() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Determines whether the package should be signed.
		 * </p>
		 * 
		 * @return true to sign the package, false otherwise
		 */
		default boolean isSign() {
			return false;
		}

		/**
		 * <p>
		 * Returns the name of the keychain to search for the signing identity.
		 * </p>
		 * 
		 * @return an optional returning the signing keychain or an empty optional
		 */
		default Optional<String> getSigningKeychain() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the team or user name portion in Apple signing identities.
		 * </p>
		 * 
		 * @return an optional returning the username or an empty optional
		 */
		default Optional<String> getSigningKeyUserName() {
			return Optional.empty();
		}
	}
	
	/**
	 * <p>
	 * Windows specific configuration.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface WindowsConfiguration {

		/**
		 * <p>
		 * Determines whether the application should be started with a console launcher.
		 * </p>
		 * 
		 * <p>
		 * A console launcher is needed for application requiring console interactions.
		 * </p>
		 * 
		 * @return true to start the application with a console launcher, false otherwise
		 */
		default boolean isConsole() {
			return false;
		}

		/**
		 * <p>
		 * Determines whether user should be enabled to choose a directory in which the product is installed.
		 * </p>
		 * 
		 * @return true to add a dialog to choose an install directory, false otherwise
		 */
		default boolean isDirChooser() {
			return false;
		}

		/**
		 * <p>
		 * Determines whether a Start Menu shortcut should be added for the application.
		 * </p>
		 * 
		 * @return true to add a Start Menu shortcut, false otherwise
		 */
		default boolean isMenu() {
			return false;
		}

		/**
		 * <p>
		 * Returns the Start Menu group the application is placed in.
		 * </p>
		 * 
		 * @return an optional returning the Start Menu group of the application or an empty optional
		 */
		default Optional<String> getMenuGroup() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Determines whether the application is installed per-user.
		 * </p>
		 * 
		 * @return true to install on a per-user basis, false otherwise
		 */
		default boolean isPerUserInstall() {
			return false;
		}

		/**
		 * <p>
		 * Determines whether a shortcut should be created for the application.
		 * </p>
		 * 
		 * @return true to create a shortcut, false otherwise
		 */
		default boolean isShortcut() {
			return false;
		}
		
		/**
		 * <p>
		 * Determines whether user should be enabled to choose if shortcuts will be created by installer.
		 * </p>
		 * 
		 * @return true to add a dialog for choosing if shortcuts will be created, false otherwise
		 */
		default boolean isShortcutPrompt() {
			return false;
		}

		/**
		 * <p>
		 * Returns the URL of available application update information.
		 * </p>
		 * 
		 * @return an optional returning the URL of available application update information or an empty optional
		 */
		default Optional<URI> getUpdateURL() {
			return Optional.empty();
		}
		
		/**
		 * <p>
		 * Returns the UUID associated with upgrades for this package.
		 * </p>
		 * 
		 * @return an optional returning the UUID associated with upgrades for this package or an empty optional
		 */
		default Optional<String> getUpgradeUUID() {
			return Optional.empty();
		}
	}
	
	/**
	 * <p>
	 * Parameters describing an application launcher.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface Launcher {
		
		/**
		 * <p>
		 * Returns the name of the application launcher.
		 * </p>
		 * 
		 * <p>
		 * Defaults to the project's name.
		 * </p>
		 * 
		 * @return an optional returning the launcher name or an empty optional
		 */
		default Optional<String> getName() {
			return Optional.empty();
		}
		
		/**
		 * <p>
		 * Returns the description of the application launcher.
		 * </p>
		 * 
		 * @return an optional returning the launcher description or an empty optional
		 */
		default Optional<String> getDescription() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the name of the module defining the main class executed with the application launcher.
		 * </p>
		 * 
		 * <p>
		 * Defaults to the project's module.
		 * </p>
		 * 
		 * @return an optional returning the name of the module defining the main class or an empty optional
		 */
		default Optional<String> getModule() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the canonical name of the main class executed with the application launcher.
		 * </p>
		 * 
		 * <p>
		 * If not specified, the task will scan for main classes in the launcher's module and pick the first one it found. Note that this is undeterministic, in case the module defines multiple main 
		 * classes it is recommended to define main class explicitly.
		 * </p>
		 * 
		 * @return an optional returning the canonical name of the main class or an empty optional
		 */
		default Optional<String> getMainClass() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the VM options to use when executing the application launcher.
		 * </p>
		 * 
		 * @return an optional returning the VM options or an empty optional
		 */
		default Optional<String> getVmOptions() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the default arguments to pass to the application launcher.
		 * </p>
		 * 
		 * @return an opional returning the arguments or an empty optional
		 */
		default Optional<String> getArguments() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the path to the application launcher icon file.
		 * </p>
		 * 
		 * @return an optional returnin the path to the icon file or an empty optional
		 */
		default Optional<Path> getIconPath() {
			return Optional.empty();
		}

		/**
		 * <p>
		 * Returns the application launcher version.
		 * </p>
		 * 
		 * @return an optional returning the application launcher version or an empty optional
		 */
		default Optional<String> getAppVersion() {
			return Optional.empty();
		}
		
		/**
		 * <p>
		 * Determines whether unnamed modules should be added when executing the application launcher.
		 * </p>
		 * 
		 * @return true to add unnamed modules, false otherwise
		 */
		default boolean isAddUnnamedModules() {
			return false;
		}
		
		/**
		 * <p>
		 * Determines whether the application launcher should be registered as a background service-type application.
		 * </p>
		 * 
		 * @return true to create a service-type application launcher, false otherwise
		 */
		default boolean isLauncherAsService() {
			return false;
		}
		
		/**
		 * <p>
		 * Determines whether the application launcher should be started in a console to enable console interaction.
		 * </p>
		 * 
		 * @return true to create a console launcher, false otherwise
		 */
		default boolean isWinConsole() {
			return false;
		}
		
		/**
		 * <p>
		 * Determines whether a shortcut should be created for the application launcher.
		 * </p>
		 * 
		 * @return true to create a shortcut, false otherwise
		 */
		default boolean isWinShortcut() {
			return false;
		}
		
		/**
		 * <p>
		 * Determines whether a Start Menu shortcut should be added for the application launcher.
		 * </p>
		 * 
		 * @return true to add a Start Menu shortcut, false otherwise
		 */
		default boolean isWinMenu() {
			return false;
		}
		
		/**
		 * <p>
		 * Returns the group value of the RPM /.spec file or Section value of DEB control file.
		 * </p>
		 * 
		 * @return an optional returning the group value or an empty optional
		 */
		default Optional<String> getLinuxAppCategory() {
			return Optional.empty();
		}
		
		/**
		 * <p>
		 * Determines whether a shortcut must be created when installing the application launcher.
		 * </p>
		 * 
		 * @return true to create a shortcut, false otherwise
		 */
		default boolean isLinuxShortcut() {
			return false;
		}
	}
}
