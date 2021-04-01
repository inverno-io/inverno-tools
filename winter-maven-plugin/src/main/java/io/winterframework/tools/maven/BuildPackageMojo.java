package io.winterframework.tools.maven;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.StringUtils;

import io.winterframework.tools.maven.internal.DependencyModule;
import io.winterframework.tools.maven.internal.ProjectModule;
import io.winterframework.tools.maven.internal.task.CompileModuleDescriptorsTask;
import io.winterframework.tools.maven.internal.task.CreateImageArchivesTask;
import io.winterframework.tools.maven.internal.task.CreateProjectJmodTask;
import io.winterframework.tools.maven.internal.task.CreateProjectRuntimeTask;
import io.winterframework.tools.maven.internal.task.ModularizeDependenciesTask;
import io.winterframework.tools.maven.internal.task.PackageModularizedDependenciesTask;
import io.winterframework.tools.maven.internal.task.CreateProjectPackageTask;
import io.winterframework.tools.maven.internal.task.ResolveDependenciesTask;

/**
 * 
 * export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
 */
@Mojo(name = "build-package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildPackageMojo extends AbstractImageMojo {

	@Parameter(property = "winter.package.name", defaultValue = "${project.artifactId}", required = true)
	private String name;
	
	@Parameter(property = "winter.package.copyright", required = false)
	private String copyright;
	
	@Parameter(property = "winter.package.description", defaultValue = "${project.description}", required = false)
	private String description;
	
	@Parameter(property = "winter.package.vendor", defaultValue = "${project.organization.name}", required = false)
	private String vendor;
	
	@Parameter(property = "winter.package.iconFile", required = false)
	private File iconFile;
	
	@Parameter(property = "winter.package.licenseFile", defaultValue = "${project.basedir}/LICENSE", required = false)
	private File licenseFile;
	
	@Parameter(property = "winter.package.resourceDirectory", required = false)
	private File resourceDirectory;
	
	@Parameter(property = "winter.package.arguments", required = false)
	private String arguments;
	
	@Parameter(property = "winter.package.vmOptions", required = false)
	private String vmOptions;
	
	@Parameter(property = "winter.package.module", required = false)
	private String module;
	
	@Parameter(property = "winter.package.installDirectory", required = false)
	private String installDirectory;
	
	@Parameter(required = false)
	private LinuxConfiguration linuxConfiguration;
	
	@Parameter(required = false)
	private MacOSConfiguration macOSConfiguration;
	
	@Parameter(required = false)
	private WindowsConfiguration windowsConfiguration;
	
	@Parameter(property = "winter.package.automaticLaunchers", defaultValue = "true", required = false)
	private boolean automaticLaunchers;
	
	@Parameter(required = false)
	private List<BuildPackageMojo.Launcher> launchers;
	
	public void doExecute() throws MojoExecutionException, MojoFailureException {
		if(this.jpackage == null) {
			throw new MojoFailureException("'jdk.jpackage' module is missing, before JDK 16 it must be activated explicitly MAVEN_OPTS=\"--add-modules jdk.incubator.jpackage in MAVEN_OPTS\"");
		}
		try {
			this.initializePaths();
			
			Set<DependencyModule> dependencies = this.getResolveDependenciesTask().call();
			
			ModuleReference projectModuleReference = ModuleFinder.of(Paths.get(this.project.getBuild().getOutputDirectory())).findAll().stream().findFirst().get();
			ProjectModule projectModule = new ProjectModule(this.project, projectModuleReference.descriptor(), dependencies, this.workingPath, this.jmodsPath, ProjectModule.Classifier.PACKAGE, this.formats);
			
			this.getLog().info("Building project package: " + projectModule.getPackageImagePath() + "...");
			
			boolean marked = false;
			this.displayProgress(0);
			if(projectModule.isMarked()) {
				this.getCreateProjectJmodTask(projectModule).call();
				marked = true;
			}
			else {
				if(this.verbose) {
					this.getLog().info("[ Project jmod is up to date ]");
				}
			}
			this.displayProgress(1f/7);
			if(dependencies.stream().anyMatch(dependency -> dependency.isMarked())) {
				this.getModularizeDependenciesTask(projectModule).call();
				this.displayProgress(2f/7);
				this.getCompileModuleDescriptorsTask(projectModule).call();
				this.displayProgress(3f/7);
				this.getPackageModularizedDependenciesTask(projectModule).call();
				marked = true;
			}
			else {
				if(this.verbose) {
					this.getLog().info("[ Project dependencies are up to date ]");
				}
			}
			this.displayProgress(4f/7);
			
			if(marked || !Files.exists(projectModule.getRuntimeImagePath())) {
				this.getCreateProjectRuntimeTask(projectModule).call();
				this.displayProgress(5f/7);
				marked = true;
			}
			
			if(marked || !Files.exists(projectModule.getPackageImagePath()) || projectModule.getImageArchivesPaths().values().stream().anyMatch(path -> !Files.exists(path))) {
				this.getCreateProjectPackageTask(projectModule).call();
				this.displayProgress(6f/7);
				if(!CreateProjectPackageTask.PACKAGE_TYPES.containsAll(this.formats)) {
					this.getImageArchivesTask(projectModule).call();
				}
			}
			this.displayProgress(1);
			
			if(this.attach) {
				for(Entry<String, Path> e : projectModule.getImageArchivesPaths().entrySet()) {
					this.projectHelper.attachArtifact(this.project, e.getKey(), projectModule.getClassifier().getClassifier(), e.getValue().toFile());
				}
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error building project package", e);
		}
	}
	
	private ResolveDependenciesTask getResolveDependenciesTask() {
		ResolveDependenciesTask task = new ResolveDependenciesTask(this, this.project.getArtifacts(), this.projectJModsOverridePath, this.jmodsExplodedPath, this.jmodsPath);
		
		task.setOverWriteIfNewer(this.overWriteIfNewer);
		task.setIncludeScope(this.includeScope);
		task.setExcludeScope(this.excludeScope);
		task.setIncludeTypes(this.includeTypes);
		task.setExcludeTypes(this.excludeTypes);
		task.setIncludeClassifiers(this.includeClassifiers);
		task.setExcludeClassifiers(this.excludeClassifiers);
		task.setExcludeArtifactIds(this.excludeArtifactIds);
		task.setIncludeArtifactIds(this.includeArtifactIds);
		task.setIncludeGroupIds(this.includeGroupIds);
		task.setExcludeGroupIds(this.excludeGroupIds);
		task.setVerbose(this.verbose);
		
		return task;
	}
	
	private CreateProjectJmodTask getCreateProjectJmodTask(ProjectModule projectModule) {
		CreateProjectJmodTask task = new CreateProjectJmodTask(this, this.jmod, projectModule, true);

		task.setVerbose(this.verbose);

		task.setProjectMainClass(this.mainClass);
		this.projectConfPath.ifPresent(path -> task.setProjectConfPath(path));
		this.projectLegalPath.ifPresent(path -> task.setProjectLegalPath(path));
		this.projectManPath.ifPresent(path -> task.setProjectManPath(path));
		
		return task;
	}
	
	private ModularizeDependenciesTask getModularizeDependenciesTask(ProjectModule projectModule) {
		ModularizeDependenciesTask task = new ModularizeDependenciesTask(this, this.jdeps, projectModule, this.jmodsExplodedPath);
		
		task.setVerbose(this.verbose);
		
		return task;
	}
	
	private CompileModuleDescriptorsTask getCompileModuleDescriptorsTask(ProjectModule projectModule) {
		CompileModuleDescriptorsTask task = new CompileModuleDescriptorsTask(this, this.javac, projectModule, this.jmodsExplodedPath);
		
		task.setVerbose(this.verbose);
		
		return task;
	}
	
	private PackageModularizedDependenciesTask getPackageModularizedDependenciesTask(ProjectModule projectModule) {
		PackageModularizedDependenciesTask task = new PackageModularizedDependenciesTask(this, this.jar, projectModule);
		
		task.setVerbose(this.verbose);
		
		return task;
	}
	
	private CreateProjectRuntimeTask getCreateProjectRuntimeTask(ProjectModule projectModule) {
		CreateProjectRuntimeTask task = new CreateProjectRuntimeTask(this, this.jlink, projectModule);
		
		task.setVerbose(this.verbose);
		
		task.setAddModules(Optional.ofNullable(this.addModules).filter(StringUtils::isNotEmpty));
		task.setAddOptions(Optional.ofNullable(this.addOptions).filter(StringUtils::isNotEmpty));
		task.setBindServices(this.bindServices);
		task.setCompress(Optional.ofNullable(this.compress).filter(StringUtils::isNotEmpty));
		task.setIgnoreSigningInformation(this.ignoreSigningInformation);
		task.setStripDebug(this.stripDebug);
		task.setStripNativeCommands(this.stripNativeCommands);
		task.setVm(Optional.ofNullable(this.vm).filter(StringUtils::isNotEmpty));
		
		return task;
	}
	
	private CreateProjectPackageTask getCreateProjectPackageTask(ProjectModule projectModule) {
		CreateProjectPackageTask task = new CreateProjectPackageTask(this, this.jpackage, projectModule, this.launchersPath);
		
		task.setVerbose(this.verbose);
		
		task.setLaunchers(this.launchers);
		task.setArguments(Optional.ofNullable(this.arguments).filter(StringUtils::isNotEmpty));
		task.setCopyright(Optional.ofNullable(this.copyright).filter(StringUtils::isNotEmpty));
		task.setDescription(Optional.ofNullable(this.description).filter(StringUtils::isNotEmpty));
		task.setIconPath(Optional.ofNullable(this.iconFile).map(file -> file.toPath().toAbsolutePath()));
		task.setInstallDirectory(Optional.ofNullable(this.installDirectory).filter(StringUtils::isNotEmpty));
		task.setLaunchers(this.launchers);
		task.setLicensePath(Optional.ofNullable(this.licenseFile).map(file -> file.toPath().toAbsolutePath()));
		task.setMainClass(Optional.ofNullable(this.mainClass).filter(StringUtils::isNotEmpty));
		task.setModule(Optional.ofNullable(this.module).filter(StringUtils::isNotEmpty));
		task.setName(this.name);
		task.setResourcePath(Optional.ofNullable(this.resourceDirectory).map(file -> file.toPath().toAbsolutePath()));
		task.setVendor(Optional.ofNullable(this.vendor).filter(StringUtils::isNotEmpty));
		task.setVmOptions(Optional.ofNullable(this.vmOptions).filter(StringUtils::isNotEmpty));
		task.setLinuxConfiguration(Optional.ofNullable(this.linuxConfiguration));
		task.setMacOSConfiguration(Optional.ofNullable(this.macOSConfiguration));
		task.setWindowsConfiguration(Optional.ofNullable(this.windowsConfiguration));
		
		return task;
	}
	
	private CreateImageArchivesTask getImageArchivesTask(ProjectModule projectModule) {
		CreateImageArchivesTask task = new CreateImageArchivesTask(this, this.archiverManager, projectModule, projectModule.getPackageImagePath());
		
		task.setVerbose(this.verbose);
		task.setPrefix(this.project.getBuild().getFinalName() + File.separator);
		
		return task;
	}
	
	public static class LinuxConfiguration {
		
		/**
		 * Name for Linux package, defaults to the application name.
		 */
		@Parameter(property = "winter.package.linux.packageName", required = false)
		private String packageName;
		
		/**
		 * Maintainer for .deb bundle.
		 */
		@Parameter(property = "winter.package.linux.debMaintainer", required = false)
		private String debMaintainer;
		
		/**
		 * Menu group this application is placed in.
		 */
		@Parameter(property = "winter.package.linux.menuGroup", required = false)
		private String menuGroup;

		/**
		 * Required packages or capabilities for the application.
		 */
		@Parameter(property = "winter.package.linux.packageDeps", required = false)
		private String packageDeps;

		/**
		 * Type of the license ("License: {@literal <value>} of the RPM .spec).
		 */
		@Parameter(property = "winter.package.linux.rpmLicenseType", required = false)
		private String rpmLicenseType;

		/**
		 * Release value of the RPM <name>.spec file or Debian revision value of the DEB control file.
		 */
		@Parameter(property = "winter.package.linux.appRelease", required = false)
		private String appRelease;
		
		/**
		 * Group value of the RPM <name>.spec file or Section value of DEB control file.
		 */
		@Parameter(property = "winter.package.linux.appCategory", required = false)
		private String appCategory;
		
		/**
		 * Creates a shortcut for the application
		 */
		@Parameter(property = "winter.package.linux.shortcut", required = false)
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
		@Parameter(property = "winter.package.macos.packageIdentifier", required = false)
		private String packageIdentifier;
		
		/**
		 * Name of the application as it appears in the Menu Bar. This can be different
		 * from the application name. This name must be less than 16 characters long and
		 * be suitable for displaying in the menu bar and the application Info window.
		 * Defaults to the application name.
		 */
		@Parameter(property = "winter.package.macos.packageName", required = false)
		private String packageName;
		
		/**
		 * When signing the application bundle, this value is prefixed to all components that need to be signed that don't have an existing bundle identifier.
		 */
		@Parameter(property = "winter.package.macos.bundleSigningPrefix", required = false)
		private String bundleSigningPrefix;
		
		/**
		 * Request that the bundle be signed.
		 */
		@Parameter(property = "winter.package.macos.sign", required = false)
		private boolean sign;
		
		/**
		 * Path of the keychain to search for the signing identity (absolute path or relative to the current directory).
		 * If not specified, the standard keychains are used.
		 */
		@Parameter(property = "winter.package.macos.signingKeychain", required = false)
		private File signingKeychain;

		/**
		 * Team name portion in Apple signing identities' names.
		 * For example "Developer ID Application: <team name>"
		 */
		@Parameter(property = "winter.package.macos.signingKeychain", required = false)
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
		@Parameter(property = "winter.package.windows.console", required = false)
		private boolean console;
		
		/**
		 * Adds a dialog to enable the user to choose a directory in which the product is installed.
		 */
		@Parameter(property = "winter.package.windows.dirChooser", required = false)
		private boolean dirChooser;
		
		/**
		 * Adds the application to the system menu.
		 */
		@Parameter(property = "winter.package.windows.menu", required = false)
		private boolean menu;
		
		/**
		 * Start Menu group this application is placed in.
		 */
		@Parameter(property = "winter.package.windows.menuGroup", required = false)
		private String menuGroup;
		
		/**
		 * Request to perform an install on a per-user basis.
		 */
		@Parameter(property = "winter.package.windows.perUserInstall", required = false)
		private boolean perUserInstall;

		/**
		 * Creates a desktop shortcut for the application.
		 */
		@Parameter(property = "winter.package.windows.shortcut", required = false)
		private boolean shortcut;
		
		/**
		 * UUID associated with upgrades for this package.
		 */
		@Parameter(property = "winter.package.windows.upgradeUUID", required = false)
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
		
		@Parameter(required = true)
		private String name;
		
		@Parameter(required = false)
		private String module;
		
		@Parameter(required = false)
		private String mainClass;
		
		@Parameter(required = false)
		private String vmOptions;
		
		@Parameter(required = false)
		private String arguments;
		
		@Parameter(required = false)
		private String appVersion;
		
		@Parameter(required = false)
		private File iconFile;

		public String getName() {
			return name;
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

		public Optional<String> getAppVersion() {
			return Optional.ofNullable(this.appVersion).filter(StringUtils::isNotEmpty);
		}

		public void setAppVersion(String appVersion) {
			this.appVersion = appVersion;
		}

		public Optional<Path> getIconPath() {
			return Optional.ofNullable(this.iconFile).map(file -> file.toPath().toAbsolutePath());
		}

		public void setIconFile(File iconFile) {
			this.iconFile = iconFile;
		}
	}
}
