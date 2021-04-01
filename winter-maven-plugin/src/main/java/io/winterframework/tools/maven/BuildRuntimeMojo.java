package io.winterframework.tools.maven;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.StringUtils;

import io.winterframework.tools.maven.internal.DependencyModule;
import io.winterframework.tools.maven.internal.ProjectModule;
import io.winterframework.tools.maven.internal.task.CompileModuleDescriptorsTask;
import io.winterframework.tools.maven.internal.task.CreateProjectJmodTask;
import io.winterframework.tools.maven.internal.task.CreateProjectRuntimeTask;
import io.winterframework.tools.maven.internal.task.ModularizeDependenciesTask;
import io.winterframework.tools.maven.internal.task.CreateImageArchivesTask;
import io.winterframework.tools.maven.internal.task.PackageModularizedDependenciesTask;
import io.winterframework.tools.maven.internal.task.ResolveDependenciesTask;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "build-runtime", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildRuntimeMojo extends AbstractImageMojo {

	@Parameter(required = false)
	private List<BuildRuntimeMojo.Launcher> launchers;
	
	public void doExecute() throws MojoExecutionException {
		try {
			this.initializePaths();
			
			Set<DependencyModule> dependencies = this.getResolveDependenciesTask().call();
			
			ModuleReference projectModuleReference = ModuleFinder.of(Paths.get(this.project.getBuild().getOutputDirectory())).findAll().stream().findFirst().get();
			ProjectModule projectModule = new ProjectModule(this.project, projectModuleReference.descriptor(), dependencies, this.workingPath, this.jmodsPath, ProjectModule.Classifier.RUNTIME_IMAGE, this.formats);

			this.getLog().info("Building project runtime: " + projectModule.getRuntimeImagePath() + "...");
			
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
			this.displayProgress(1f/6);
			if(dependencies.stream().anyMatch(dependency -> dependency.isMarked())) {
				this.getModularizeDependenciesTask(projectModule).call();
				this.displayProgress(2f/6);
				this.getCompileModuleDescriptorsTask(projectModule).call();
				this.displayProgress(3f/6);
				this.getPackageModularizedDependenciesTask(projectModule).call();
				marked = true;
			}
			else {
				if(this.verbose) {
					this.getLog().info("[ Project dependencies are up to date ]");
				}
			}
			this.displayProgress(4f/6);
			
			if(marked || !Files.exists(projectModule.getRuntimeImagePath()) || projectModule.getImageArchivesPaths().values().stream().anyMatch(path -> !Files.exists(path))) {
				this.getCreateProjectRuntimeTask(projectModule).call();
				this.displayProgress(5f/6);
				this.getImageArchivesTask(projectModule).call();
			}
			this.displayProgress(1);
			
			if(this.attach) {
				for(Entry<String, Path> e : projectModule.getImageArchivesPaths().entrySet()) {
					this.projectHelper.attachArtifact(this.project, e.getKey(), projectModule.getClassifier().getClassifier(), e.getValue().toFile());
				}
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error building project runtime", e);
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
		task.setLaunchers(this.launchers);
		task.setStripDebug(this.stripDebug);
		task.setStripNativeCommands(this.stripNativeCommands);
		task.setVm(Optional.ofNullable(this.compress).filter(StringUtils::isNotEmpty));
		
		return task;
	}
	
	private CreateImageArchivesTask getImageArchivesTask(ProjectModule projectModule) {
		CreateImageArchivesTask task = new CreateImageArchivesTask(this, this.archiverManager, projectModule, projectModule.getRuntimeImagePath());
		
		task.setVerbose(this.verbose);
		
		task.setPrefix(this.project.getBuild().getFinalName() + File.separator);
		
		return task;
	}
	
	public static class Launcher {
		
		@Parameter(required = true)
		private String name;

		@Parameter(required = false)
		private String module;
		
		@Parameter(required = false)
		private String mainClass;

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
	}
}
