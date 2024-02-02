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
package io.inverno.tool.buildtools.internal;

import io.inverno.tool.buildtools.ArchiveTask;
import io.inverno.tool.buildtools.ContainerizeTask;
import io.inverno.tool.buildtools.Image;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import io.inverno.tool.buildtools.PackageApplicationTask;

/**
 * <p>
 * Generic {@link PackageApplicationTask} implementation.
 * </p>
 * 
 * <p>
 * This implementation relies on JDK's {@code jpackage} tool.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericPackageApplicationTask extends AbstractTask<Set<Image>, PackageApplicationTask> implements PackageApplicationTask {

	private static final Logger LOGGER = LogManager.getLogger(GenericPackageApplicationTask.class);
	
	private static final int UNITARY_WEIGHT = 450;
	
	private static final PrintStream OUT = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.INFO)
			.setAutoFlush(true)
			.buildPrintStream();
	
	private static final PrintStream ERR = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.ERROR)
			.setAutoFlush(true)
			.buildPrintStream();
	
	private Optional<String> copyright = Optional.empty();
	private Optional<String> vendor = Optional.empty();
	private Optional<URI> aboutURL = Optional.empty();
	private Optional<String> installDirectory = Optional.empty();
	private Optional<Path> licensePath = Optional.empty();
	private Optional<Path> resourcePath = Optional.empty();
	private Set<Path> appContents = Set.of();
	private boolean automaticLaunchers = true;
	private List<? extends Launcher> launchers = List.of();
	private Optional<LinuxConfiguration> linuxConfiguration = Optional.empty();
	private Optional<MacOSConfiguration> macOSConfiguration = Optional.empty();
	private Optional<WindowsConfiguration> windowsConfiguration = Optional.empty();
	private Set<PackageType> types = Set.of();
	
	/**
	 * <p>
	 * Creates a generic package application task.
	 * </p>
	 * 
	 * @param parentTask the parent task
	 */
	public GenericPackageApplicationTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}

	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		if(!this.types.isEmpty()) {
			return "Project application packages created: " + this.types.stream().map(PackageType::getFormat).collect(Collectors.joining(", "));
		}
		else {
			return "Project application image created";
		}
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		return 20 + this.types.size() * UNITARY_WEIGHT;
	}
	
	@Override
	public PackageApplicationTask copyright(String copyright) {
		this.copyright = Optional.ofNullable(copyright);
		return this;
	}

	@Override
	public PackageApplicationTask vendor(String vendor) {
		this.vendor = Optional.ofNullable(vendor);
		return this;
	}
	
	@Override
	public PackageApplicationTask aboutURL(URI aboutURL) {
		this.aboutURL = Optional.ofNullable(aboutURL);
		return this;
	}
	
	@Override
	public PackageApplicationTask installDirectory(String installDirectory) {
		this.installDirectory = Optional.ofNullable(installDirectory);
		return this;
	}

	@Override
	public PackageApplicationTask licensePath(Path licensePath) {
		this.licensePath = Optional.ofNullable(licensePath);
		return this;
	}

	@Override
	public PackageApplicationTask resourcesPath(Path resourcesPath) {
		this.resourcePath = Optional.ofNullable(resourcesPath);
		return this;
	}
	
	@Override
	public PackageApplicationTask appContents(Set<Path> appContents) {
		this.appContents = appContents != null ? appContents : Set.of();
		return this;
	}
	
	@Override
	public PackageApplicationTask automaticLaunchers(boolean automaticLaunchers) {
		this.automaticLaunchers = automaticLaunchers;
		return this;
	}
	
	@Override
	public PackageApplicationTask launchers(List<? extends Launcher> launchers) {
		this.launchers = launchers != null ? launchers : List.of();
		return this;
	}

	@Override
	public PackageApplicationTask linuxConfiguration(LinuxConfiguration linuxConfiguration) {
		this.linuxConfiguration = Optional.ofNullable(linuxConfiguration);
		return this;
	}

	@Override
	public PackageApplicationTask macOSConfiguration(MacOSConfiguration macOSConfiguration) {
		this.macOSConfiguration = Optional.ofNullable(macOSConfiguration);
		return this;
	}

	@Override
	public PackageApplicationTask windowsConfiguration(WindowsConfiguration windowsConfiguration) {
		this.windowsConfiguration = Optional.ofNullable(windowsConfiguration);
		return this;
	}

	@Override
	public PackageApplicationTask types(Set<PackageType> types) {
		this.types = types != null ? types : Set.of();
		return this;
	}
	
	@Override
	protected Set<Image> doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Packaging project application...");
		}
		
		Map<String, Path> applicationImageArchivesPaths = project.getImageArchivesPaths(
			ImageType.APPLICATION, 
			this.types.stream().map(PackageType::getFormat).collect(Collectors.toSet())
		);
		Path runtimeImagePath = project.getImagePath(ImageType.RUNTIME);
		Path applicationImagePath = project.getImagePath(ImageType.APPLICATION);
		if(project.isMarked() || 
			project.getDependencies().stream().anyMatch(dependency -> dependency.isMarked()) || 
			!Files.exists(applicationImagePath) || 
			applicationImageArchivesPaths.entrySet().stream().anyMatch(e -> !Files.exists(e.getValue()))) {
			LOGGER.info("[ Packaging application {}... ]", applicationImagePath);
			
			if(!Files.exists(runtimeImagePath)) {
				throw new TaskExecutionException("Missing project runtime: " + runtimeImagePath);
			}
			
			if(Files.exists(applicationImagePath)) {
				try (Stream<Path> walk = Files.walk(applicationImagePath)) {
					for(Iterator<Path> pathIterator = walk.sorted(Comparator.reverseOrder()).iterator(); pathIterator.hasNext();) {
						Files.delete(pathIterator.next());
					}
				}
				catch (IOException e) {
					throw new TaskExecutionException("Error cleaning project application", e);
				}
			}
			
			List<String> jpackage_args = new LinkedList<>();
			List<String> nonAppImage_jpackage_args = new LinkedList<>();
			
			jpackage_args.add("--runtime-image");
			jpackage_args.add(runtimeImagePath.toString());

			jpackage_args.add("--dest");
			jpackage_args.add(applicationImagePath.getParent().toString());
			
			List<Launcher> appLaunchers = new ArrayList<>(this.launchers);
			final Launcher mainLauncher;
			if(!appLaunchers.isEmpty()) {
				mainLauncher = appLaunchers.remove(0);
			}
			else {
				mainLauncher = new PackageApplicationTask.Launcher() {
					@Override
					public Optional<String> getName() {
						return Optional.empty();
					}
					
					@Override
					public Optional<String> getDescription() {
						return Optional.empty();
					}

					@Override
					public Optional<String> getModule() {
						return Optional.empty();
					}

					@Override
					public Optional<String> getMainClass() {
						return Optional.empty();
					}

					@Override
					public Optional<String> getVmOptions() {
						return Optional.empty();
					}

					@Override
					public Optional<String> getArguments() {
						return Optional.empty();
					}

					@Override
					public Optional<Path> getIconPath() {
						return Optional.empty();
					}

					@Override
					public Optional<String> getAppVersion() {
						return Optional.empty();
					}

					@Override
					public boolean isAddUnnamedModules() {
						return true;
					}
					
					@Override
					public boolean isLauncherAsService() {
						return false;
					}
					
					@Override
					public boolean isWinConsole() {
						return false;
					}
					
					@Override
					public boolean isWinShortcut() {
						return false;
					}
					
					@Override
					public boolean isWinMenu() {
						return false;
					}
					
					@Override
					public Optional<String> getLinuxAppCategory() {
						return Optional.empty();
					}
					
					@Override
					public boolean isLinuxShortcut() {
						return false;
					}
				};
			}
			
			String mainLauncherName = mainLauncher.getName().orElse(project.getName());
			String mainLauncherModule = mainLauncher.getModule().orElse(project.getModuleName());
			String mainLauncherMainClass = mainLauncher.getMainClass().or(() -> {
					if(mainLauncherModule.equals(project.getModuleName())) {
						try {
							return project.getDefaultMainClass().map(defaultMainClass -> {
								LOGGER.info(" - no main class specified for main launcher {}, defaulting to {}", mainLauncherName, defaultMainClass);
								return defaultMainClass;
							});
						} 
						catch (ClassNotFoundException | IOException e) {
							LOGGER.warn("Could not find project main class", e);
						}
					}
					return Optional.empty();
				}).orElseThrow(() -> new TaskExecutionException("Main class is missing for main launcher: " + mainLauncherName));
			
			jpackage_args.add("--name");
			jpackage_args.add(mainLauncherName);
			
			jpackage_args.add("--app-version");
			jpackage_args.add(project.getModuleVersion());
			
			mainLauncher.getIconPath().ifPresent(value -> {
				if(Files.exists(value)) {
					jpackage_args.add("--icon");
					jpackage_args.add(value.toString());
				}
				else {
					LOGGER.warn(" - ignoring icon {} which does not exist", value.toString());
				}
			});
			
			jpackage_args.add("--module");

			jpackage_args.add(mainLauncherModule + "/" + mainLauncherMainClass);
			
			Optional<String> addUnnamedVmOption = mainLauncher.isAddUnnamedModules() ? Optional.ofNullable(project.getDependencies().stream()
					.filter(dependency -> !dependency.isNamed())
					.map(BuildDependency::getModuleName).collect(Collectors.joining(","))
				)
				.filter(StringUtils::isNotEmpty)
				.map(unnamedModules -> "--add-modules " + unnamedModules) : Optional.empty();
			
			if(addUnnamedVmOption.isPresent() || mainLauncher.getVmOptions().isPresent()) {
				jpackage_args.add("--java-options");
				jpackage_args.add(Stream.concat(addUnnamedVmOption.stream(), mainLauncher.getVmOptions().stream()).collect(Collectors.joining(" ")));
			}
			
			mainLauncher.getArguments().map(JavaTools::sanitizeArguments).ifPresent(value -> {
				jpackage_args.add("--arguments");
				jpackage_args.add(value);
			});
			
			this.copyright.ifPresent(value -> {
				jpackage_args.add("--copyright");
				jpackage_args.add(value);
			});
			mainLauncher.getDescription().ifPresent(value -> {
				jpackage_args.add("--description");
				jpackage_args.add(value);
			});
			mainLauncher.getIconPath().ifPresent(value -> {
				jpackage_args.add("--icon");
				jpackage_args.add(value.toAbsolutePath().toString());
			});
			this.vendor.ifPresent(value -> {
				jpackage_args.add("--vendor");
				jpackage_args.add(value);
			});
			this.aboutURL.ifPresent(value -> {
				nonAppImage_jpackage_args.add("--about-url");
				nonAppImage_jpackage_args.add(value.normalize().toString());
			});
			this.installDirectory.ifPresent(value -> {
				jpackage_args.add("--install-dir");
				jpackage_args.add(value);
			});
			this.licensePath.ifPresent(value -> {
				if(Files.exists(value)) {
					nonAppImage_jpackage_args.add("--license-file");
					nonAppImage_jpackage_args.add(value.toString());
				}
				else {
					LOGGER.warn(" - ignoring license file {} which does not exist", value.toString());
				}
			});
			this.resourcePath.ifPresent(value -> {
				if(Files.exists(value)) {
					jpackage_args.add("--resource-dir");
					jpackage_args.add(value.toString());
				}
				else {
					LOGGER.warn(" - ignoring resource directory {} which does not exist", value.toString());
				}
			});
			this.appContents.forEach(appContent -> {
				jpackage_args.add("--app-content");
				jpackage_args.add(appContent.toAbsolutePath().toString());
			});
			if(mainLauncher.isLauncherAsService()) {
				jpackage_args.add("--launcher-as-service");
			}
			
			Path launchersPath = project.getLaunchersPath();			
			if(!appLaunchers.isEmpty()) {
				try {
					Files.createDirectories(launchersPath);
					for(Launcher launcher : appLaunchers) {
						Properties launcherProperties = new Properties();
						
						String launcherName = launcher.getName().orElseThrow(() -> new TaskExecutionException("Missing launcher name"));
						String launcherModule = launcher.getModule().orElse(project.getModuleName());
						String launcherMainClass = launcher.getMainClass().orElseThrow(() -> new TaskExecutionException("Main class is missing for launcher: " + launcherName));
						
						launcherProperties.put("module", launcherModule + "/" + launcherMainClass);
						
						launcher.getDescription().ifPresent(value -> {
							launcherProperties.put("description", value);
						});
						
						launcher.getArguments().map(JavaTools::sanitizeArguments).ifPresent(value -> {
							launcherProperties.put("arguments", value);
						});
						
						if(addUnnamedVmOption.isPresent() || launcher.getVmOptions().isPresent()) {
							jpackage_args.add("--java-options");
							jpackage_args.add(Stream.concat(addUnnamedVmOption.stream(), launcher.getVmOptions().stream()).collect(Collectors.joining(" ")));
						}
						
						launcher.getAppVersion().ifPresent(value -> {
							launcherProperties.put("app-version", value);
						});
						launcher.getIconPath().ifPresent(value -> {
							if(Files.exists(value)) {
								launcherProperties.put("icon", value.toString());
							}
							else {
								LOGGER.warn(" - ignoring icon {} which does not exist", value.toString());
							}
						});
						if(launcher.isLauncherAsService()) {
							launcherProperties.put("launcher-as-service", true);
						}
						if(launcher.isWinConsole()) {
							launcherProperties.put("win-console", true);
						}
						if(launcher.isWinShortcut()) {
							launcherProperties.put("win-shortcut", true);
						}
						if(launcher.isWinMenu()) {
							launcherProperties.put("win-menu", true);
						}
						launcher.getLinuxAppCategory().ifPresent(value -> {
							launcherProperties.put("linux-app-category", value.toString());
						});
						if(launcher.isLinuxShortcut()) {
							launcherProperties.put("linux-shortcut", true);
						}
						
						Path launcherPropertiesPath = launchersPath.resolve(launcherName + ".properties");
						Files.deleteIfExists(launcherPropertiesPath);
						try(OutputStream launcherPropertiesOutput = Files.newOutputStream(launcherPropertiesPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
							launcherProperties.store(launcherPropertiesOutput, null);
						}
						
						jpackage_args.add("--add-launcher");
						jpackage_args.add(launcherName + "=" + launcherPropertiesPath.toString());
					}
				} 
				catch (IOException e) {
					throw new TaskExecutionException("Error creating project application launchers", e);
				}
			}

			if(this.automaticLaunchers) {
				try {
					Files.createDirectories(launchersPath);
					// Add launchers for project module main classes other than the main launcher
					for(String mainClass : project.getMainClasses()) {
						if(!mainLauncherModule.equals(project.getModuleName()) || !mainLauncherMainClass.equals(mainClass)) {
							String launcherName = mainClass;
							int classSimpleNameIndex = launcherName.lastIndexOf('.');
							if(classSimpleNameIndex > 0) {
								launcherName = launcherName.substring(classSimpleNameIndex + 1);							
							}
							launcherName = Character.toLowerCase(launcherName.charAt(0)) + launcherName.substring(1);
							Properties launcherProperties = new Properties();
							
							launcherProperties.put("module", project.getModuleName() + "/" + mainClass);
							
							Path launcherPropertiesPath = launchersPath.resolve(launcherName + ".properties");
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
			
			Set<Image> jpackageImages = new HashSet<>();

			switch(Platform.getSystemPlatform()) {
				case LINUX: this.linuxConfiguration.ifPresentOrElse(
						configuration -> {
							configuration.getPackageName().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--linux-package-name");
								nonAppImage_jpackage_args.add(value);
							});
							configuration.getDebMaintainer().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--linux-deb-maintainer");
								nonAppImage_jpackage_args.add(value);
							});
							configuration.getMenuGroup().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--linux-menu-group");
								nonAppImage_jpackage_args.add(value);
							});
							configuration.getPackageDeps().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--linux-package-deps");
								nonAppImage_jpackage_args.add(value);
							});
							configuration.getRpmLicenseType().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--linux-rpm-license-type");
								nonAppImage_jpackage_args.add(value);
							});
							configuration.getAppRelease().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--linux-app-release");
								nonAppImage_jpackage_args.add(value);
							});
							configuration.getAppCategory().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--linux-app-category");
								nonAppImage_jpackage_args.add(value);
							});
							if(configuration.isShortcut() || mainLauncher.isLinuxShortcut()) {
								nonAppImage_jpackage_args.add("--linux-shortcut");
							}
						},
						() -> {
							if(mainLauncher.isLinuxShortcut()) {
								nonAppImage_jpackage_args.add("--linux-shortcut");
							}
						}
					);
					break;
				case MACOS: this.macOSConfiguration.ifPresent(configuration -> {
						configuration.getPackageIdentifier().ifPresent(value -> {
							nonAppImage_jpackage_args.add("--mac-package-identifier");
							nonAppImage_jpackage_args.add(value);
						});
						configuration.getPackageName().ifPresent(value -> {
							nonAppImage_jpackage_args.add("--mac-package-name");
							nonAppImage_jpackage_args.add(value);
						});
						configuration.getPackageSigningPrefix().ifPresent(value -> {
							if(Runtime.version().feature() >= 17) {
								nonAppImage_jpackage_args.add("--mac-package-signing-prefix");
							}
							else {
								nonAppImage_jpackage_args.add("--mac-bundle-signing-prefix");
							}
							nonAppImage_jpackage_args.add(value);
						});
						if(configuration.isSign()) {
							nonAppImage_jpackage_args.add("--mac-sign");
						}
						configuration.getSigningKeychain().ifPresent(value -> {
							nonAppImage_jpackage_args.add("--mac-signing-keychain");
							nonAppImage_jpackage_args.add(value);
						});
						configuration.getSigningKeyUserName().ifPresent(value -> {
							nonAppImage_jpackage_args.add("--mac-signing-key-user-name");
							nonAppImage_jpackage_args.add(value);
						});
					});
					break;
				case WINDOWS: this.windowsConfiguration.ifPresentOrElse(
						configuration -> {
							if(configuration.isConsole() || mainLauncher.isWinConsole()) {
								jpackage_args.add("--win-console");
							}
							if(configuration.isDirChooser()) {
								nonAppImage_jpackage_args.add("--win-dir-chooser");
							}
							if(configuration.isMenu() || mainLauncher.isWinMenu()) {
								nonAppImage_jpackage_args.add("--win-menu");
							}
							configuration.getMenuGroup().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--win-menu-group");
								nonAppImage_jpackage_args.add(value);
							});
							if(configuration.isPerUserInstall()) {
								nonAppImage_jpackage_args.add("--win-per-user-install");
							}
							if(configuration.isShortcut() || mainLauncher.isWinShortcut()) {
								nonAppImage_jpackage_args.add("--win-shortcut");
							}
							if(configuration.isShortcutPrompt()) {
								nonAppImage_jpackage_args.add("--win-shortcut-prompt");
							}
							configuration.getUpdateURL().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--win-update-url");
								nonAppImage_jpackage_args.add(value.normalize().toString());
							});
							configuration.getUpgradeUUID().ifPresent(value -> {
								nonAppImage_jpackage_args.add("--win-upgrade-uuid");
								nonAppImage_jpackage_args.add(value);
							});
						},
						() -> {
							if(mainLauncher.isWinConsole()) {
								jpackage_args.add("--win-console");
							}
							if(mainLauncher.isWinMenu()) {
								nonAppImage_jpackage_args.add("--win-menu");
							}
							if(mainLauncher.isWinShortcut()) {
								nonAppImage_jpackage_args.add("--win-shortcut");
							}
						}
					);
					break;
				default: LOGGER.warn("Could not apply platform specific configuration because the platform could not be determined");
			}
			
			// Build Application image
			try {
				List<String> image_jpackage_args = new ArrayList<>(jpackage_args);
				image_jpackage_args.add("--type");
				image_jpackage_args.add("app-image");

				// We must generate app-image and invoke CreateImageArchivesTask later in the process
				LOGGER.info(" - jpackage {}", image_jpackage_args.stream().collect(Collectors.joining(" ")));
				if(JavaTools.JPACKAGE.run(OUT, ERR, image_jpackage_args.stream().toArray(String[]::new)) == 0) {
					// jpackage creates the app in the main launcher name folder
					Files.move(applicationImagePath.getParent().resolve(mainLauncherName), applicationImagePath);
					jpackageImages.add(new GenericImage(ImageType.APPLICATION, null, applicationImagePath));
				}
				else {
					throw new TaskExecutionException("Error packaging project application");
				}
			} 
			catch (IOException e) {
				throw new TaskExecutionException("Error packaging project application", e);
			}
			
			jpackage_args.addAll(nonAppImage_jpackage_args);
			
			try {	
				// Package Application
				for(Map.Entry<String, Path> e : applicationImageArchivesPaths.entrySet()) {
					List<String> archive_jpackage_args = new ArrayList<>(jpackage_args);
					archive_jpackage_args.add("--type");
					archive_jpackage_args.add(e.getKey());

					LOGGER.info(" - jpackage {}", archive_jpackage_args.stream().collect(Collectors.joining(" ")));
					if(JavaTools.JPACKAGE.run(OUT, ERR, archive_jpackage_args.stream().toArray(String[]::new)) == 0) {
						Optional<Path> jpackagePath = Files.list(applicationImagePath.getParent())
							.filter(path -> path.getFileName().toString().endsWith(e.getKey()))
							.findFirst();

						if(!jpackagePath.isPresent()) {
							throw new TaskExecutionException("Generated application of type " + e.getKey() + " could not be found in " + applicationImagePath.getParent());
						}
						Files.move(jpackagePath.get(), e.getValue());
						jpackageImages.add(new GenericImage(ImageType.APPLICATION, e.getKey(), e.getValue()));
					}
					else {
						throw new TaskExecutionException("Error packaging project application");
					}
				}
				return jpackageImages;
			} 
			catch (IOException e) {
				throw new TaskExecutionException("Error packaging project application", e);
			}
		}
		else {
			LOGGER.info("[ Project application package is up to date ]");
			
			Set<Image> jpackageImages = new HashSet<>();
			jpackageImages.add(new GenericImage(ImageType.APPLICATION, null, applicationImagePath));
			
			for(Map.Entry<String, Path> e : applicationImageArchivesPaths.entrySet()) {
				jpackageImages.add(new GenericImage(ImageType.APPLICATION, e.getKey(), e.getValue()));
			}
			return jpackageImages;
		}
	}

	@Override
	public ArchiveTask archive() {
		return new GenericArchiveTask(this, ImageType.APPLICATION);
	}

	@Override
	public ContainerizeTask containerize() {
		return new GenericContainerizeTask(this);
	}
}
