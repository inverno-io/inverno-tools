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
package io.inverno.tool.maven.internal.task;

import io.inverno.tool.maven.internal.DependencyModule;
import io.inverno.tool.maven.internal.NullPrintStream;
import io.inverno.tool.maven.internal.Platform;
import io.inverno.tool.maven.internal.ProgressBar.Step;
import io.inverno.tool.maven.internal.ProjectModule;
import io.inverno.tool.maven.internal.Task;
import io.inverno.tool.maven.internal.TaskExecutionException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * <p>
 * Creates the project module application image.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class CreateProjectApplicationTask extends Task<Void> {

	public static final Set<String> JPACKAGE_TYPES = Set.of("exe", "msi", "rpm", "deb", "pkg", "dmg");
	
	private final ToolProvider jpackage;
	private final ProjectModule projectModule;
	private final Path launchersPath;
	
	private Optional<String> copyright = Optional.empty();
	
	private Optional<String> description = Optional.empty();
	
	private Optional<String> vendor = Optional.empty();
	
	private Optional<Path> licensePath = Optional.empty();
	
	private Optional<Path> resourcePath = Optional.empty();
	
	private Optional<String> installDirectory = Optional.empty();
	
	private Optional<LinuxConfiguration> linuxConfiguration = Optional.empty();
	
	private Optional<MacOSConfiguration> macOSConfiguration = Optional.empty();
	
	private Optional<WindowsConfiguration> windowsConfiguration = Optional.empty();
	
	private boolean automaticLaunchers;
	
	private List<Launcher> launchers;

	public CreateProjectApplicationTask(AbstractMojo mojo, ToolProvider jpackage, ProjectModule projectModule, Path launchersPath) {
		super(mojo);
		this.jpackage = jpackage;
		this.projectModule = projectModule;
		this.launchersPath = launchersPath;
	}
	
	@Override
	public void setStep(Step step) {
		if(step != null) {
			step.setDescription("Creating project application...");
		}
		super.setStep(step);
	}

	@Override
	protected Void execute() throws TaskExecutionException {
		if(this.projectModule.isMarked() || 
			this.projectModule.getModuleDependencies().stream().anyMatch(dependency -> dependency.isMarked()) || 
			!Files.exists(this.projectModule.getApplicationImagePath()) || 
			this.projectModule.getImageArchivesPaths().entrySet().stream().filter(e -> JPACKAGE_TYPES.contains(e.getKey())).anyMatch(e -> !Files.exists(e.getValue()))) {
			Path packagePath = this.projectModule.getApplicationImagePath();
			if(this.verbose) {
				this.getLog().info("[ Creating project application: " + packagePath + "... ]");
			}
			
			Path rumtimeImagePath = this.projectModule.getRuntimeImagePath();
			if(!Files.exists(rumtimeImagePath)) {
				throw new TaskExecutionException("Missing project runtime image: " + rumtimeImagePath);
			}
			
			if(Files.exists(packagePath)) {
				try (Stream<Path> walk = Files.walk(packagePath)) {
					for(Iterator<Path> pathIterator = walk.sorted(Comparator.reverseOrder()).iterator(); pathIterator.hasNext();) {
						Files.delete(pathIterator.next());
					}
				}
				catch (IOException e) {
					throw new TaskExecutionException("Error cleaning project application", e);
				}
			}
			
			List<String> jpackage_args = new LinkedList<>();
			
			jpackage_args.add("--runtime-image");
			jpackage_args.add(rumtimeImagePath.toString());

			jpackage_args.add("--dest");
			jpackage_args.add(packagePath.getParent().toString());
			
			final Launcher mainLauncher;
			if(this.launchers != null && !this.launchers.isEmpty()) {
				mainLauncher = this.launchers.remove(0);
			}
			else {
				mainLauncher = new Launcher();
			}
			
			mainLauncher.setModule(mainLauncher.getModule().orElse(this.projectModule.getModuleName()));
			mainLauncher.setMainClass(mainLauncher.getMainClass().or(() -> {
					if(mainLauncher.getModule().get().equals(this.projectModule.getModuleName())) {
						try {
							return this.projectModule.getDefaultMainClass().map(defaultMainClass -> {
								if(this.verbose) {
									this.getLog().info(" - no main class specified for main launcher " + mainLauncher.getName() + ", defaulting to " + defaultMainClass);
								}
								return defaultMainClass;
							});
						} 
						catch (ClassNotFoundException | IOException e) {
							if(this.verbose) {
								this.getLog().warn("Could not find project main class", e);
							}
						}
					}
					return Optional.empty();
				}).orElseThrow(() -> new TaskExecutionException("Main launcher class is missing"))
			);
			
			jpackage_args.add("--name");
			jpackage_args.add(mainLauncher.getName().orElse(this.projectModule.getArtifact().getArtifactId()));
			
			jpackage_args.add("--app-version");
			jpackage_args.add(this.projectModule.getModuleVersion());
			
			mainLauncher.getIconPath().ifPresent(value -> {
				if(Files.exists(value)) {
					jpackage_args.add("--icon");
					jpackage_args.add(value.toString());
				}
				else if(this.verbose) {
					this.getLog().warn(" - ignoring icon " + value.toString() + " which does not exist");
				}
			});
			
			jpackage_args.add("--module");

			jpackage_args.add(mainLauncher.getModule().get() + "/" + mainLauncher.getMainClass().get());
			
			Optional<String> addUnnamedVmOption = mainLauncher.isAddUnnamedModules() ? Optional.ofNullable(this.projectModule.getModuleDependencies().stream()
					.filter(dependency -> !dependency.isNamed())
					.map(DependencyModule::getModuleName).collect(Collectors.joining(","))
				)
				.filter(StringUtils::isNotEmpty)
				.map(unnamedModules -> "--add-modules " + unnamedModules) : Optional.empty();
			
			if(addUnnamedVmOption.isPresent() || mainLauncher.getVmOptions().isPresent()) {
				jpackage_args.add("--java-options");
				jpackage_args.add(Stream.concat(addUnnamedVmOption.stream(), mainLauncher.getVmOptions().stream()).collect(Collectors.joining(" ")));
			}
			
			mainLauncher.getArguments().map(this::sanitizeArguments).ifPresent(value -> {
				jpackage_args.add("--arguments");
				jpackage_args.add(value);
			});
			
			this.copyright.ifPresent(value -> {
				jpackage_args.add("--copyright");
				jpackage_args.add(value);
			});
			this.description.ifPresent(value -> {
				jpackage_args.add("--description");
				jpackage_args.add(value);
			});
			this.vendor.ifPresent(value -> {
				jpackage_args.add("--vendor");
				jpackage_args.add(value);
			});
			this.licensePath.ifPresent(value -> {
				if(Files.exists(value)) {
					jpackage_args.add("--license-file");
					jpackage_args.add(value.toString());
				}
				else if(this.verbose) {
					this.getLog().warn(" - ignoring license file " + value.toString() + " which does not exist");
				}
			});
			this.resourcePath.ifPresent(value -> {
				if(Files.exists(value)) {
					jpackage_args.add("--resource-dir");
					jpackage_args.add(value.toString());
				}
				else if(this.verbose) {
					this.getLog().warn("Ignoring resource directory " + value.toString() + " which does not exist");
				}
			});
			this.installDirectory.ifPresent(value -> {
				jpackage_args.add("--install-dir");
				jpackage_args.add(value);
			});
			
			if(this.launchers != null && !this.launchers.isEmpty()) {
				try {
					Files.createDirectories(this.launchersPath);
					for(Launcher launcher : this.launchers) {
						Properties launcherProperties = new Properties();
						
						launcher.setModule(launcher.getModule().orElse(this.projectModule.getModuleName()));
						launcher.setMainClass(launcher.getMainClass().or(() -> {
								if(launcher.getModule().get().equals(this.projectModule.getModuleName())) {
									try {
										return this.projectModule.getDefaultMainClass().map(defaultMainClass -> {
											if(this.verbose) {
												this.getLog().info(" - no main class specified for launcher " + launcher.getName() + ", defaulting to " + defaultMainClass);
											}
											return defaultMainClass;
										});
									} 
									catch (ClassNotFoundException | IOException e) {
										if(this.verbose) {
											this.getLog().warn("Could not find project main class", e);
										}
									}
								}
								return Optional.empty();
							}).orElseThrow(() -> new TaskExecutionException("Main launcher class is missing: " + launcher.getName()))
						);
						launcherProperties.put("module", launcher.getModule().get() + "/" + launcher.getMainClass().get());
						
						launcher.getArguments().map(this::sanitizeArguments).ifPresent(value -> {
							launcherProperties.put("arguments", value);
						});
						
						if(addUnnamedVmOption.isPresent() || launcher.getVmOptions().isPresent()) {
							jpackage_args.add("--java-options");
							jpackage_args.add(Stream.concat(addUnnamedVmOption.stream(), launcher.getVmOptions().stream()).collect(Collectors.joining(" ")));
						}
						
						launcher.getIconPath().ifPresent(value -> {
							launcherProperties.put("icon", value.toString());
						});
						launcher.getAppVersion().ifPresent(value -> {
							launcherProperties.put("app-version", value);
						});
						
						Path launcherPropertiesPath = this.launchersPath.resolve(launcher.getName() + ".properties");
						Files.deleteIfExists(launcherPropertiesPath);
						try(OutputStream launcherPropertiesOutput = Files.newOutputStream(launcherPropertiesPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
							launcherProperties.store(launcherPropertiesOutput, null);
						}
						
						jpackage_args.add("--add-launcher");
						jpackage_args.add(launcher.getName() + "=" + launcherPropertiesPath.toString());
					}
				} 
				catch (IOException e) {
					throw new TaskExecutionException("Error creating project application launchers", e);
				}
			}

			if(this.automaticLaunchers) {
				try {
					Files.createDirectories(this.launchersPath);
					// Add launchers for project module main classes other than the main launcher
					for(String mainClass : this.projectModule.getMainClasses()) {
						if(!mainLauncher.getModule().get().equals(this.projectModule.getModuleName()) || !mainLauncher.getMainClass().get().equals(mainClass)) {
							String launcherName = mainClass;
							int classSimpleNameIndex = launcherName.lastIndexOf('.');
							if(classSimpleNameIndex > 0) {
								launcherName = launcherName.substring(classSimpleNameIndex + 1);							
							}
							launcherName = Character.toLowerCase(launcherName.charAt(0)) + launcherName.substring(1);
							Properties launcherProperties = new Properties();
							
							launcherProperties.put("module", this.projectModule.getModuleName() + "/" + mainClass);
							
							Path launcherPropertiesPath = this.launchersPath.resolve(launcherName + ".properties");
							Files.deleteIfExists(launcherPropertiesPath);
							try(OutputStream launcherPropertiesOutput = Files.newOutputStream(launcherPropertiesPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
								launcherProperties.store(launcherPropertiesOutput, null);
							}
							
							jpackage_args.add("--add-launcher");
							jpackage_args.add(launcherName + "=" + launcherPropertiesPath.toString());
						}
					}
				}
				catch (ClassNotFoundException | IOException e) {
					throw new TaskExecutionException("Could not find project main classes", e);
				}
			}
			
			if(Platform.getSystemPlatform() == Platform.LINUX) {
				this.linuxConfiguration.ifPresent(configuration -> {
					configuration.getPackageName().ifPresent(value -> {
						jpackage_args.add("--linux-package-name");
						jpackage_args.add(value);
					});
					configuration.getDebMaintainer().ifPresent(value -> {
						jpackage_args.add("--linux-deb-maintainer");
						jpackage_args.add(value);
					});
					configuration.getMenuGroup().ifPresent(value -> {
						jpackage_args.add("--linux-menu-group");
						jpackage_args.add(value);
					});
					configuration.getPackageDeps().ifPresent(value -> {
						jpackage_args.add("--linux-package-deps");
						jpackage_args.add(value);
					});
					configuration.getRpmLicenseType().ifPresent(value -> {
						jpackage_args.add("--linux-rpm-license-type");
						jpackage_args.add(value);
					});
					configuration.getAppRelease().ifPresent(value -> {
						jpackage_args.add("--linux-app-release");
						jpackage_args.add(value);
					});
					configuration.getAppCategory().ifPresent(value -> {
						jpackage_args.add("--linux-app-category");
						jpackage_args.add(value);
					});
					if(configuration.isShortcut()) {
						jpackage_args.add("--linux-shortcut");
					}
				});
			}
			else if(Platform.getSystemPlatform() == Platform.MACOS) {
				this.macOSConfiguration.ifPresent(configuration -> {
					configuration.getPackageIdentifier().ifPresent(value -> {
						jpackage_args.add("--mac-package-identifier");
						jpackage_args.add(value);
					});
					configuration.getPackageName().ifPresent(value -> {
						jpackage_args.add("--mac-package-name");
						jpackage_args.add(value);
					});
					configuration.getBundleSigningPrefix().ifPresent(value -> {
						jpackage_args.add("--mac-bundle-signing-prefix");
						jpackage_args.add(value);
					});
					if(configuration.isSign()) {
						jpackage_args.add("--mac-sign");
					}
					configuration.getSigningKeychain().ifPresent(value -> {
						jpackage_args.add("--mac-signing-keychain");
						jpackage_args.add(value.toString());
					});
					configuration.getSigningKeyUserName().ifPresent(value -> {
						jpackage_args.add("--mac-signing-key-user-name");
						jpackage_args.add(value);
					});
				});
			}
			else if(Platform.getSystemPlatform() == Platform.WINDOWS) {
				this.windowsConfiguration.ifPresent(configuration -> {
					if(configuration.isConsole()) {
						jpackage_args.add("--win-console");
					}
					if(configuration.isDirChooser()) {
						jpackage_args.add("--win-dir-chooser");
					}
					if(configuration.isMenu()) {
						jpackage_args.add("--win-menu");
					}
					configuration.getMenuGroup().ifPresent(value -> {
						jpackage_args.add("--win-menu-group");
						jpackage_args.add(value);
					});
					if(configuration.isPerUserInstall()) {
						jpackage_args.add("--win-per-user-install");
					}
					if(configuration.isShortcut()) {
						jpackage_args.add("--win-shortcut");
					}
					configuration.getUpgradeUUID().ifPresent(value -> {
						jpackage_args.add("--win-upgrade-uuid");
						jpackage_args.add(value);
					});
				});
			}
			else {
				this.getLog().warn("Could not apply platform specific configuration because the platform could not be determined");
			}

			jpackage_args.add("--type");
			jpackage_args.add("app-image");
			
			try {
				if(this.projectModule.getImageArchivesPaths().isEmpty() || !JPACKAGE_TYPES.containsAll(this.projectModule.getImageArchivesPaths().keySet())) {
					List<String> filtered_jpackage_args = new ArrayList<>();
					for(Iterator<String> jpackage_argsIterator = jpackage_args.iterator();jpackage_argsIterator.hasNext();) {
						String currentArg = jpackage_argsIterator.next();
						if(currentArg.equals("--license-file")) {
							jpackage_argsIterator.remove();
							jpackage_argsIterator.next();
							jpackage_argsIterator.remove();
						}
						else {
							filtered_jpackage_args.add(currentArg);
						}
					}
					
					// We must generate app-image and invoke CreateImageArchivesTask later in the process
					if(this.verbose) {
						this.getLog().info(" - jpackage " + filtered_jpackage_args.stream().collect(Collectors.joining(" ")));			
					}
					if(this.jpackage.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), filtered_jpackage_args.stream().toArray(String[]::new)) == 0) {
						Files.move(packagePath.getParent().resolve(mainLauncher.getName().orElse(this.projectModule.getArtifact().getArtifactId())), packagePath);
					}
					else {
						throw new TaskExecutionException("Error creating project application, activate '-Dinverno.verbose=true' to display full log");
					}
				}
				
				for(Entry<String, Path> e : this.projectModule.getImageArchivesPaths().entrySet()) {
					if(JPACKAGE_TYPES.contains(e.getKey())) {
						jpackage_args.set(jpackage_args.size() - 1, e.getKey());
						if(this.verbose) {
							this.getLog().info(" - jpackage " + jpackage_args.stream().collect(Collectors.joining(" ")));			
						}
						if(this.jpackage.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), jpackage_args.stream().toArray(String[]::new)) == 0) {
							Optional<Path> jpackagePath = Files.list(packagePath.getParent())
								.filter(path -> path.getFileName().toString().endsWith(e.getKey()))
								.findFirst();
							
							if(!jpackagePath.isPresent()) {
								throw new TaskExecutionException("Generated application of type " + e.getKey() + " could not be found in " + packagePath.getParent());
							}
							Files.move(jpackagePath.get(), e.getValue());
						}
						else {
							throw new TaskExecutionException("Error creating project application, activate '-Dinverno.verbose=true' to display full log");
						}
					}
				}
			} catch (IOException e) {
				throw new TaskExecutionException("Error creating project application, activate '-Dinverno.verbose=true' to display full log", e);
			}
		}
		else {
			if(this.verbose) {
				this.getLog().info("[ Project application is up to date ]");
			}
		}
		return null;
	}
	
	public Optional<String> getCopyright() {
		return copyright;
	}

	public void setCopyright(Optional<String> copyright) {
		this.copyright = copyright;
	}

	public Optional<String> getDescription() {
		return description;
	}

	public void setDescription(Optional<String> description) {
		this.description = description;
	}

	public Optional<String> getVendor() {
		return vendor;
	}

	public void setVendor(Optional<String> vendor) {
		this.vendor = vendor;
	}

	public Optional<Path> getLicensePath() {
		return licensePath;
	}

	public void setLicensePath(Optional<Path> licensePath) {
		this.licensePath = licensePath;
	}

	public Optional<Path> getResourcePath() {
		return resourcePath;
	}

	public void setResourcePath(Optional<Path> resourcePath) {
		this.resourcePath = resourcePath;
	}

	public Optional<String> getInstallDirectory() {
		return installDirectory;
	}

	public void setInstallDirectory(Optional<String> installDirectory) {
		this.installDirectory = installDirectory;
	}

	public Optional<LinuxConfiguration> getLinuxConfiguration() {
		return linuxConfiguration;
	}

	public void setLinuxConfiguration(Optional<LinuxConfiguration> linuxConfiguration) {
		this.linuxConfiguration = linuxConfiguration;
	}

	public Optional<MacOSConfiguration> getMacOSConfiguration() {
		return macOSConfiguration;
	}

	public void setMacOSConfiguration(Optional<MacOSConfiguration> macOSConfiguration) {
		this.macOSConfiguration = macOSConfiguration;
	}

	public Optional<WindowsConfiguration> getWindowsConfiguration() {
		return windowsConfiguration;
	}

	public void setWindowsConfiguration(Optional<WindowsConfiguration> windowsConfiguration) {
		this.windowsConfiguration = windowsConfiguration;
	}

	public boolean isAutomaticLaunchers() {
		return automaticLaunchers;
	}

	public void setAutomaticLaunchers(boolean automaticLaunchers) {
		this.automaticLaunchers = automaticLaunchers;
	}

	public List<Launcher> getLaunchers() {
		return launchers;
	}

	public void setLaunchers(List<Launcher> launchers) {
		this.launchers = launchers;
	}
	
	public static class LinuxConfiguration {
		
		/**
		 * Name for Linux package, defaults to the application name.
		 */
		@Parameter(property = "inverno.app.linux.packageName", required = false)
		private String packageName;
		
		/**
		 * Maintainer for .deb bundle.
		 */
		@Parameter(property = "inverno.app.linux.debMaintainer", required = false)
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

		public Optional<String> getPackageName() {
			return Optional.ofNullable(this.packageName).filter(StringUtils::isNotEmpty);
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public Optional<String> getDebMaintainer() {
			return Optional.ofNullable(this.debMaintainer).filter(StringUtils::isNotEmpty);
		}

		public void setDebMaintainer(String debMaintainer) {
			this.debMaintainer = debMaintainer;
		}

		public Optional<String> getMenuGroup() {
			return Optional.ofNullable(this.menuGroup).filter(StringUtils::isNotEmpty);
		}

		public void setMenuGroup(String menuGroup) {
			this.menuGroup = menuGroup;
		}

		public Optional<String> getPackageDeps() {
			return Optional.ofNullable(this.packageDeps).filter(StringUtils::isNotEmpty);
		}

		public void setPackageDeps(String packageDeps) {
			this.packageDeps = packageDeps;
		}

		public Optional<String> getRpmLicenseType() {
			return Optional.ofNullable(this.rpmLicenseType).filter(StringUtils::isNotEmpty);
		}

		public void setRpmLicenseType(String rpmLicenseType) {
			this.rpmLicenseType = rpmLicenseType;
		}

		public Optional<String> getAppRelease() {
			return Optional.ofNullable(this.appRelease).filter(StringUtils::isNotEmpty);
		}

		public void setAppRelease(String appRelease) {
			this.appRelease = appRelease;
		}

		public Optional<String> getAppCategory() {
			return Optional.ofNullable(this.appCategory).filter(StringUtils::isNotEmpty);
		}

		public void setAppCategory(String appCategory) {
			this.appCategory = appCategory;
		}

		public boolean isShortcut() {
			return shortcut;
		}

		public void setShortcut(boolean shortcut) {
			this.shortcut = shortcut;
		}
	}
	
	public static class MacOSConfiguration {
		
		/**
		 * An identifier that uniquely identifies the application for macOSX.
		 * Defaults to the the main class name.
		 * May only use alphanumeric (A-Z,a-z,0-9), hyphen (-), and period (.) characters.
		 */
		@Parameter(property = "inverno.app.macos.packageIdentifier", required = false)
		private String packageIdentifier;
		
		/**
		 * Name of the application as it appears in the Menu Bar. This can be different
		 * from the application name. This name must be less than 16 characters long and
		 * be suitable for displaying in the menu bar and the application Info window.
		 * Defaults to the application name.
		 */
		@Parameter(property = "inverno.app.macos.packageName", required = false)
		private String packageName;
		
		/**
		 * When signing the application bundle, this value is prefixed to all components that need to be signed that don't have an existing bundle identifier.
		 */
		@Parameter(property = "inverno.app.macos.bundleSigningPrefix", required = false)
		private String bundleSigningPrefix;
		
		/**
		 * Request that the bundle be signed.
		 */
		@Parameter(property = "inverno.app.macos.sign", required = false)
		private boolean sign;
		
		/**
		 * Path of the keychain to search for the signing identity (absolute path or relative to the current directory).
		 * If not specified, the standard keychains are used.
		 */
		@Parameter(property = "inverno.app.macos.signingKeychain", required = false)
		private File signingKeychain;

		/**
		 * Team name portion in Apple signing identities' names.
		 * For example "Developer ID Application: <team name>"
		 */
		@Parameter(property = "inverno.app.macos.signingKeychain", required = false)
		private String signingKeyUserName;

		public Optional<String> getPackageIdentifier() {
			return Optional.ofNullable(this.packageIdentifier).filter(StringUtils::isNotEmpty);
		}

		public void setPackageIdentifier(String packageIdentifier) {
			this.packageIdentifier = packageIdentifier;
		}

		public Optional<String> getPackageName() {
			return Optional.ofNullable(this.packageName).filter(StringUtils::isNotEmpty);
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public Optional<String> getBundleSigningPrefix() {
			return Optional.ofNullable(this.bundleSigningPrefix).filter(StringUtils::isNotEmpty);
		}

		public void setBundleSigningPrefix(String bundleSigningPrefix) {
			this.bundleSigningPrefix = bundleSigningPrefix;
		}

		public boolean isSign() {
			return sign;
		}

		public void setSign(boolean sign) {
			this.sign = sign;
		}

		public Optional<Path> getSigningKeychain() {
			return Optional.ofNullable(this.signingKeychain).map(file -> file.toPath().toAbsolutePath());
		}

		public void setSigningKeychain(File signingKeychain) {
			this.signingKeychain = signingKeychain;
		}

		public Optional<String> getSigningKeyUserName() {
			return Optional.ofNullable(this.signingKeyUserName).filter(StringUtils::isNotEmpty);
		}

		public void setSigningKeyUserName(String signingKeyUserName) {
			this.signingKeyUserName = signingKeyUserName;
		}
	}
	
	public static class WindowsConfiguration {

		/**
		 * Creates a console launcher for the application, should be specified for application which requires console interactions
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
		 * Request to perform an install on a per-user basis.
		 */
		@Parameter(property = "inverno.app.windows.perUserInstall", required = false)
		private boolean perUserInstall;

		/**
		 * Creates a desktop shortcut for the application.
		 */
		@Parameter(property = "inverno.app.windows.shortcut", required = false)
		private boolean shortcut;
		
		/**
		 * UUID associated with upgrades for this package.
		 */
		@Parameter(property = "inverno.app.windows.upgradeUUID", required = false)
		private String upgradeUUID;

		public boolean isConsole() {
			return console;
		}

		public void setConsole(boolean console) {
			this.console = console;
		}

		public boolean isDirChooser() {
			return dirChooser;
		}

		public void setDirChooser(boolean dirChooser) {
			this.dirChooser = dirChooser;
		}

		public boolean isMenu() {
			return menu;
		}

		public void setMenu(boolean menu) {
			this.menu = menu;
		}

		public Optional<String> getMenuGroup() {
			return Optional.ofNullable(this.menuGroup).filter(StringUtils::isNotEmpty);
		}

		public void setMenuGroup(String menuGroup) {
			this.menuGroup = menuGroup;
		}

		public boolean isPerUserInstall() {
			return perUserInstall;
		}

		public void setPerUserInstall(boolean perUserInstall) {
			this.perUserInstall = perUserInstall;
		}

		public boolean isShortcut() {
			return shortcut;
		}

		public void setShortcut(boolean shortcut) {
			this.shortcut = shortcut;
		}

		public Optional<String> getUpgradeUUID() {
			return Optional.ofNullable(this.upgradeUUID).filter(StringUtils::isNotEmpty);
		}

		public void setUpgradeUUID(String upgradeUUID) {
			this.upgradeUUID = upgradeUUID;
		}
	}
	
	public static class Launcher {
		
		/**
		 * The name of the application launcher.
		 */
		@Parameter(required = false)
		private String name;
		
		/**
		 * The module containing the main class of the application launcher. If not
		 * specified the project's module is selected.
		 */
		@Parameter(required = false)
		private String module;
		
		/**
		 * The main class of the application launcher. If not specified the specified
		 * module must provide a main class.
		 */
		@Parameter(required = false)
		private String mainClass;
		
		/**
		 *  The VM options to use when executing the application launcher. 
		 */
		@Parameter(defaultValue = "-Dorg.apache.logging.log4j.simplelog.level=INFO -Dorg.apache.logging.log4j.level=INFO", required = false)
		private String vmOptions = "-Dorg.apache.logging.log4j.simplelog.level=INFO -Dorg.apache.logging.log4j.level=INFO";
		
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

		public Optional<String> getName() {
			return Optional.ofNullable(this.name).filter(StringUtils::isNotEmpty);
		}

		public void setName(String name) {
			this.name = name;
		}

		public Optional<String> getModule() {
			return Optional.ofNullable(this.module).filter(StringUtils::isNotEmpty);
		}

		public void setModule(String module) {
			this.module = module;
		}

		public Optional<String> getMainClass() {
			return Optional.ofNullable(this.mainClass).filter(StringUtils::isNotEmpty);
		}

		public void setMainClass(String mainClass) {
			this.mainClass = mainClass;
		}

		public Optional<String> getVmOptions() {
			return Optional.ofNullable(this.vmOptions).filter(StringUtils::isNotEmpty);
		}

		public void setVmOptions(String vmOptions) {
			this.vmOptions = vmOptions;
		}

		public Optional<String> getArguments() {
			return Optional.ofNullable(this.arguments).filter(StringUtils::isNotEmpty);
		}

		public void setArguments(String arguments) {
			this.arguments = arguments;
		}

		public Optional<Path> getIconPath() {
			return Optional.ofNullable(this.iconFile).map(file -> file.toPath().toAbsolutePath());
		}

		public void setIconFile(File iconFile) {
			this.iconFile = iconFile;
		}

		public Optional<String> getAppVersion() {
			return Optional.ofNullable(this.appVersion).filter(StringUtils::isNotEmpty);
		}

		public void setAppVersion(String appVersion) {
			this.appVersion = appVersion;
		}
		
		public boolean isAddUnnamedModules() {
			return addUnnamedModules;
		}

		public void setAddUnnamedModules(boolean addUnnamedModules) {
			this.addUnnamedModules = addUnnamedModules;
		}
	}
	
}
