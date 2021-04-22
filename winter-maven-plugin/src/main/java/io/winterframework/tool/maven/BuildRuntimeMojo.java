package io.winterframework.tool.maven;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
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

import io.winterframework.tool.maven.internal.DependencyModule;
import io.winterframework.tool.maven.internal.ProgressBar;
import io.winterframework.tool.maven.internal.ProjectModule;
import io.winterframework.tool.maven.internal.task.CompileModuleDescriptorsTask;
import io.winterframework.tool.maven.internal.task.CreateImageArchivesTask;
import io.winterframework.tool.maven.internal.task.CreateProjectJmodTask;
import io.winterframework.tool.maven.internal.task.CreateProjectRuntimeTask;
import io.winterframework.tool.maven.internal.task.ModularizeDependenciesTask;
import io.winterframework.tool.maven.internal.task.PackageModularizedDependenciesTask;
import io.winterframework.tool.maven.internal.task.ResolveDependenciesTask;

/**
 * <p>
 * Builds the project runtime image.
 * </p>
 * 
 * <p>
 * A runtime image is a custom Java runtime containing a set of modules and
 * their dependencies.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "build-runtime", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildRuntimeMojo extends AbstractImageMojo {

	protected ProjectModule projectModule;
	
	/**
	 * Skips the generation of the runtime.
	 */
	@Parameter(property = "winter.runtime.skip", required = false)
	private boolean skip;
	
	/**
	 * A list of launchers to include in the resulting runtime.
	 */
	@Parameter(required = false)
	private List<CreateProjectRuntimeTask.Launcher> launchers;
	
	@Override
	protected boolean isSkip() {
		return this.skip;
	}
	
	protected void doExecute() throws MojoExecutionException, MojoFailureException {
		try {
			Set<DependencyModule> dependencies = this.getResolveDependenciesTask().call();
			
			ModuleReference projectModuleReference = ModuleFinder.of(Paths.get(this.project.getBuild().getOutputDirectory())).findAll().stream().findFirst().get();
			this.projectModule = new ProjectModule(this.project, projectModuleReference.descriptor(), dependencies, this.winterBuildPath, this.jmodsPath, ProjectModule.Classifier.RUNTIME, this.formats);

			this.getLog().info("Building runtime image: " + this.projectModule.getRuntimeImagePath() + "...");
			ProgressBar progressBar = this.createProgressBar();

			CreateProjectJmodTask createProjectJmodTask = this.getCreateProjectJmodTask(progressBar.addStep(1, 100));
			ModularizeDependenciesTask modularizeDependenciesTask = this.getModularizeDependenciesTask(progressBar.addStep(25, 100));
			CompileModuleDescriptorsTask compileModuleDescriptorsTask = this.getCompileModuleDescriptorsTask(progressBar.addStep(2, 100));
			PackageModularizedDependenciesTask packageModularizedDependenciesTask = this.getPackageModularizedDependenciesTask(progressBar.addStep(6, 100));
			CreateProjectRuntimeTask createProjectRuntimeTask = this.getCreateProjectRuntimeTask(progressBar.addStep(44, 100));
			CreateImageArchivesTask createImageArchivesTask = this.getCreateImageArchivesTask(progressBar.addStep(22, 100));

			progressBar.display();
			
			createProjectJmodTask.call();
			modularizeDependenciesTask.call();
			compileModuleDescriptorsTask.call();
			packageModularizedDependenciesTask.call();
			createProjectRuntimeTask.call();
			createImageArchivesTask.call();

			if(this.attach) {
				for(Entry<String, Path> e : this.projectModule.getImageArchivesPaths().entrySet()) {
					this.projectHelper.attachArtifact(this.project, e.getKey(), this.projectModule.getClassifier().getClassifier(), e.getValue().toFile());
				}
			}
			progressBar.complete();
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error building runtime image", e);
		}
	}
	
	protected ResolveDependenciesTask getResolveDependenciesTask() {
		ResolveDependenciesTask task = new ResolveDependenciesTask(this, this.project.getArtifacts(), this.jmodsOverridePath, this.jmodsExplodedPath, this.jmodsPath);
		
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
	
	protected CreateProjectJmodTask getCreateProjectJmodTask(ProgressBar.Step step) {
		CreateProjectJmodTask task = new CreateProjectJmodTask(this, this.jmod, this.projectModule, true);

		task.setVerbose(this.verbose);
		task.setStep(step);

		task.setProjectMainClass(this.mainClass);
		this.confPath.ifPresent(path -> task.setProjectConfPath(path));
		this.legalPath.ifPresent(path -> task.setProjectLegalPath(path));
		this.manPath.ifPresent(path -> task.setProjectManPath(path));
		
		return task;
	}
	
	protected ModularizeDependenciesTask getModularizeDependenciesTask(ProgressBar.Step step) {
		ModularizeDependenciesTask task = new ModularizeDependenciesTask(this, this.jdeps, this.projectModule, this.jmodsExplodedPath);
		
		task.setVerbose(this.verbose);
		task.setStep(step);
		
		return task;
	}
	
	protected CompileModuleDescriptorsTask getCompileModuleDescriptorsTask(ProgressBar.Step step) {
		CompileModuleDescriptorsTask task = new CompileModuleDescriptorsTask(this, this.javac, this.projectModule, this.jmodsExplodedPath);
		
		task.setVerbose(this.verbose);
		task.setStep(step);
		
		return task;
	}
	
	protected PackageModularizedDependenciesTask getPackageModularizedDependenciesTask(ProgressBar.Step step) {
		PackageModularizedDependenciesTask task = new PackageModularizedDependenciesTask(this, this.jar, this.projectModule);
		
		task.setVerbose(this.verbose);
		task.setStep(step);
		
		return task;
	}
	
	protected CreateProjectRuntimeTask getCreateProjectRuntimeTask(ProgressBar.Step step) {
		CreateProjectRuntimeTask task = new CreateProjectRuntimeTask(this, this.jlink, this.projectModule);
		
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
		
		task.setStep(step);
		
		return task;
	}
	
	protected CreateImageArchivesTask getCreateImageArchivesTask(ProgressBar.Step step) {
		CreateImageArchivesTask task = new CreateImageArchivesTask(this, this.projectModule, projectModule.getRuntimeImagePath());
		
		task.setVerbose(this.verbose);
		
		task.setPrefix(this.project.getBuild().getFinalName() + File.separator);
		
		task.setStep(step);
		
		return task;
	}
}
