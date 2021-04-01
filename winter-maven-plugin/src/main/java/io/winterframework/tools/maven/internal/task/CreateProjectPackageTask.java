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
package io.winterframework.tools.maven.internal.task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashSet;
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

import io.winterframework.tools.maven.BuildPackageMojo;
import io.winterframework.tools.maven.BuildPackageMojo.LinuxConfiguration;
import io.winterframework.tools.maven.BuildPackageMojo.MacOSConfiguration;
import io.winterframework.tools.maven.BuildPackageMojo.WindowsConfiguration;
import io.winterframework.tools.maven.internal.NullPrintStream;
import io.winterframework.tools.maven.internal.ProjectModule;
import io.winterframework.tools.maven.internal.Task;
import io.winterframework.tools.maven.internal.TaskExecutionException;

/**
 * @author jkuhn
 *
 */
public class CreateProjectPackageTask extends Task<Void> {

	public static final Set<String> PACKAGE_TYPES = Set.of("exe", "msi", "rpm", "deb", "pkg", "dmg");
	
	public static enum Platform {
		WINDOWS,
		LINUX,
		MACOS,
		UNKNOWN;
	}
	
	public static final Platform PLATFORM;
	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.indexOf("win") >= 0) {
			PLATFORM = Platform.WINDOWS;
		}
		else if(osName.indexOf("mac") >= 0) {
			PLATFORM = Platform.MACOS;
		}
		else if(osName.indexOf("linux") >= 0) {
			PLATFORM = Platform.LINUX;
		}
		else {
			PLATFORM = Platform.UNKNOWN;
		}
	}
	
	private final ToolProvider jpackage;
	private final ProjectModule projectModule;
	private final Path launchersPath;
	
	private String name;
	
	private Optional<String> copyright = Optional.empty();
	
	private Optional<String> description = Optional.empty();
	
	private Optional<String> vendor = Optional.empty();
	
	private Optional<Path> iconPath = Optional.empty();
	
	private Optional<Path> licensePath = Optional.empty();
	
	private Optional<Path> resourcePath = Optional.empty();
	
	private Optional<String> arguments = Optional.empty();
	
	private Optional<String> vmOptions = Optional.empty();
	
	private Optional<String> module = Optional.empty();
	
	private Optional<String> mainClass = Optional.empty();
	
	private Optional<String> installDirectory = Optional.empty();
	
	private Optional<LinuxConfiguration> linuxConfiguration = Optional.empty();
	
	private Optional<MacOSConfiguration> macOSConfiguration = Optional.empty();
	
	private Optional<WindowsConfiguration> windowsConfiguration = Optional.empty();
	
	private boolean automaticLaunchers;
	
	private List<BuildPackageMojo.Launcher> launchers;

	public CreateProjectPackageTask(AbstractMojo mojo, ToolProvider jpackage, ProjectModule projectModule, Path launchersPath) {
		super(mojo);
		this.jpackage = jpackage;
		this.projectModule = projectModule;
		this.launchersPath = launchersPath;
	}

	@Override
	public Void call() throws TaskExecutionException {
		Path packagePath = this.projectModule.getPackageImagePath();
		if(this.verbose) {
			this.getLog().info("[ Creating project package: " + packagePath + "... ]");
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
				throw new TaskExecutionException("Error cleaning project package", e);
			}
		}
		
		List<String> jpackage_args = new LinkedList<>();
		
		jpackage_args.add("--runtime-image");
		jpackage_args.add(rumtimeImagePath.toString());

		jpackage_args.add("--dest");
		jpackage_args.add(packagePath.getParent().toString());
		
		jpackage_args.add("--app-version");
		jpackage_args.add(this.projectModule.getModuleVersion());
		
		jpackage_args.add("--name");
		jpackage_args.add(this.name);

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
		this.iconPath.ifPresent(value -> {
			if(Files.exists(value)) {
				jpackage_args.add("--icon");
				jpackage_args.add(value.toString());
			}
			else if(this.verbose) {
				this.getLog().warn(" - ignoring icon " + value.toString() + " which does not exist");
			}
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
		
		Set<String> projectBinMainClasses = new HashSet<>();
		jpackage_args.add("--module");

		final String moduleName = this.module.orElse(this.projectModule.getModuleName());
		final String moduleMainClass = this.mainClass.or(() -> {
			if(moduleName.equals(this.projectModule.getModuleName())) {
				try {
					return this.projectModule.getDefaultMainClass().map(defaultMainClass -> {
						if(this.verbose) {
							this.getLog().info(" - no main class specified, defaulting to " + defaultMainClass);
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
		}).orElseThrow(() -> new TaskExecutionException("Main project class is missing"));
		
		jpackage_args.add(moduleName + "/" + moduleMainClass);
		
		this.arguments.ifPresent(value -> {
			jpackage_args.add("--arguments");
			jpackage_args.add(value);
		});
		this.vmOptions.ifPresent(value -> {
			jpackage_args.add("--java-options");
			jpackage_args.add(value);
		});
		
		if(this.launchers != null && !this.launchers.isEmpty()) {
			try {
				Files.createDirectories(this.launchersPath);
				for(BuildPackageMojo.Launcher launcher : this.launchers) {
					Properties launcherProperties = new Properties();
					
					final String launcherModuleName = launcher.getModule().orElse(this.projectModule.getModuleName());
					final String launcherModuleMainClass = launcher.getMainClass().or(() -> {
						if(moduleName.equals(this.projectModule.getModuleName())) {
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
					}).orElseThrow(() -> new TaskExecutionException("Main launcher class is missing: " + launcher.getName()));
					launcherProperties.put("module", launcherModuleName + "/" + launcherModuleMainClass);
					
					launcher.getArguments().ifPresent(value -> {
						launcherProperties.put("arguments", value);
					});
					launcher.getVmOptions().ifPresent(value -> {
						launcherProperties.put("java-options", value);
					});
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
				throw new TaskExecutionException("Error creating project package launchers", e);
			}
		}

		if(this.automaticLaunchers) {
			try {
				// Add launchers for module main classes that haven't been added yet 
				for(String mainClass : this.projectModule.getMainClasses()) {
					if(!projectBinMainClasses.contains(mainClass)) {
						String launcherName = mainClass;
						int classSimpleNameIndex = launcherName.lastIndexOf('.');
						if(classSimpleNameIndex > 0) {
							launcherName = launcherName.substring(classSimpleNameIndex + 1);							
						}
						
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
		
		if(PLATFORM == Platform.LINUX) {
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
		else if(PLATFORM == Platform.MACOS) {
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
		else if(PLATFORM == Platform.WINDOWS) {
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
			if(!PACKAGE_TYPES.containsAll(this.projectModule.getImageArchivesPaths().keySet())) {
				// We must generate app-image and invoke CreateImageArchivesTask later in the process
				if(this.verbose) {
					this.getLog().info(" - jpackage " + jpackage_args.stream().collect(Collectors.joining(" ")));			
				}
				if(this.jpackage.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), jpackage_args.stream().toArray(String[]::new)) == 0) {
					Files.move(packagePath.getParent().resolve(this.name), packagePath);
				}
				else {
					throw new TaskExecutionException("Error creating project package, activate '-Dwinter.image.verbose=true' to display full log");
				}
			}
			
			for(Entry<String, Path> e : this.projectModule.getImageArchivesPaths().entrySet()) {
				if(PACKAGE_TYPES.contains(e.getKey())) {
					jpackage_args.set(jpackage_args.size() - 1, e.getKey());
					if(this.verbose) {
						this.getLog().info(" - jpackage " + jpackage_args.stream().collect(Collectors.joining(" ")));			
					}
					if(this.jpackage.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), jpackage_args.stream().toArray(String[]::new)) == 0) {
						Optional<Path> jpackagePath = Files.list(packagePath.getParent())
							.filter(path -> path.getFileName().toString().endsWith(e.getKey()))
							.findFirst();
						
						if(!jpackagePath.isPresent()) {
							throw new TaskExecutionException("Generated package of type " + e.getKey() + " could not be found in " + packagePath.getParent());
						}
						Files.move(jpackagePath.get(), e.getValue());
					}
					else {
						throw new TaskExecutionException("Error creating project package, activate '-Dwinter.image.verbose=true' to display full log");
					}
				}
			}
		} catch (IOException e) {
			throw new TaskExecutionException("Error creating project package, activate '-Dwinter.image.verbose=true' to display full log", e);
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Optional<Path> getIconPath() {
		return iconPath;
	}

	public void setIconPath(Optional<Path> iconPath) {
		this.iconPath = iconPath;
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

	public Optional<String> getArguments() {
		return arguments;
	}

	public void setArguments(Optional<String> arguments) {
		this.arguments = arguments;
	}

	public Optional<String> getVmOptions() {
		return vmOptions;
	}

	public void setVmOptions(Optional<String> vmOptions) {
		this.vmOptions = vmOptions;
	}

	public Optional<String> getModule() {
		return module;
	}

	public void setModule(Optional<String> module) {
		this.module = module;
	}

	public Optional<String> getMainClass() {
		return mainClass;
	}

	public void setMainClass(Optional<String> mainClass) {
		this.mainClass = mainClass;
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

	public List<BuildPackageMojo.Launcher> getLaunchers() {
		return launchers;
	}

	public void setLaunchers(List<BuildPackageMojo.Launcher> launchers) {
		this.launchers = launchers;
	}

	public ProjectModule getProjectModule() {
		return projectModule;
	}
	
}
