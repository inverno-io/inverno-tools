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

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

import io.inverno.tool.buildtools.ArchiveTask;
import io.inverno.tool.buildtools.BuildJmodTask;
import io.inverno.tool.buildtools.BuildRuntimeTask;
import io.inverno.tool.buildtools.Image;
import io.inverno.tool.maven.internal.MavenInvernoProject;

/**
 * <p>
 * Builds the project runtime image.
 * </p>
 * 
 * <p>
 * A runtime image is a custom Java runtime containing a set of modules and their dependencies.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "build-runtime", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildRuntimeMojo extends AbstractInvernoMojo {
	
	/**
	 * The Maven project helper.
	 */
	@Component
	protected MavenProjectHelper mavenProjectHelper;
	
	/**
	 * Skips the generation of the runtime.
	 */
	@Parameter(property = "inverno.runtime.skip", required = false)
	private boolean skip;
	
	/* MavenProjectDependencyResolver */
	
	/*
	 * I don't see anything else than jar for now, we could consider jmod but jmod is not for runtime so having it define as a project artifacts is less likely
	 */
	/**
	 * Comma separated list of Types to include. Empty String indicates include everything (default).
	 */
//    @Parameter( property = "inverno.runtime.includeTypes", defaultValue = "jar" )
	protected String includeTypes = "jar";

	/**
	 * Comma separated list of Types to exclude. Empty String indicates don't exclude anything (default).
	 */
//    @Parameter( property = "inverno.runtime.excludeTypes", defaultValue = "" )
	protected String excludeTypes = "";

	/**
	 * Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
	 * 
	 * <ul>
	 * <li><code>runtime</code> scope gives runtime and compile dependencies,</li>
	 * <li><code>compile</code> scope gives compile, provided, and system dependencies,</li>
	 * <li><code>test</code> (default) scope gives all dependencies,</li>
	 * <li><code>provided</code> scope just gives provided dependencies,</li>
	 * <li><code>system</code> scope just gives system dependencies.</li>
	 * </ul>
	 */
	@Parameter(property = "inverno.runtime.includeScope", defaultValue = "", required = false)
	protected String includeScope;

	/**
	 * Scope to exclude. An Empty string indicates no scopes (default).
	 */
	@Parameter(property = "inverno.runtime.excludeScope", defaultValue = "", required = false)
	protected String excludeScope;

	/**
	 * Comma separated list of Classifiers to include. Empty String indicates include everything (default).
	 */
	@Parameter(property = "inverno.runtime.includeClassifiers", defaultValue = "", required = false)
	protected String includeClassifiers;

	/**
	 * Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
	 */
	@Parameter(property = "inverno.runtime.excludeClassifiers", defaultValue = "", required = false)
	protected String excludeClassifiers;

	/**
	 * Comma separated list of Artifact names to include. Empty String indicates include everything (default).
	 */
	@Parameter(property = "inverno.runtime.includeArtifactIds", defaultValue = "", required = false)
	protected String includeArtifactIds;
	
	/**
	 * Comma separated list of Artifact names to exclude.
	 */
	@Parameter(property = "inverno.runtime.excludeArtifactIds", defaultValue = "", required = false)
	protected String excludeArtifactIds;

	/**
	 * Comma separated list of GroupIds to include. Empty String indicates include everything (default).
	 */
	@Parameter(property = "inverno.runtime.includeGroupIds", defaultValue = "", required = false)
	protected String includeGroupIds;
	
	/**
	 * Comma separated list of GroupId Names to exclude.
	 */
	@Parameter(property = "inverno.runtime.excludeGroupIds", defaultValue = "", required = false)
	protected String excludeGroupIds;

	/* CreateJmodTask */
	
	/**
	 * The main class in the project module to use when building the project JMOD package.
	 */
	@Parameter(property = "inverno.runtime.mainClass", required = false)
	protected String projectMainClass;
	
	/**
	 * Resolves the project main class when not specified explicitly.
	 */
	@Parameter(property = "inverno.runtime.resolveMainClass", defaultValue = "false", required = false)
	protected boolean resolveProjectMainClass;

	/**
	 * A directory containing user-editable configuration files that will be copied to the resulting runtime.
	 */
	@Parameter(property = "inverno.runtime.configurationDirectory", defaultValue = "${project.basedir}/src/main/conf/", required = false)
	protected File configurationDirectory;

	/**
	 * A directory containing legal notices that will be copied to the resulting runtime.
	 */
	@Parameter(property = "inverno.runtime.legalDirectory", defaultValue = "${project.basedir}/src/main/legal/", required = false)
	protected File legalDirectory;

	/**
	 * A directory containing man pages that will be copied to the resulting runtime.
	 */
	@Parameter(property = "inverno.runtime.manDirectory", defaultValue = "${project.basedir}/src/main/man/", required = false)
	protected File manDirectory;
	
	/* CreateRuntimeTask */
	
	/**
	 * The modules to add to the resulting runtime.
	 */
	@Parameter(property = "inverno.runtime.addModules", required = false)
	protected String addModules;

	/**
	 * The options to prepend before any other options when invoking the JVM in the resulting runtime.
	 */
	@Parameter(property = "inverno.runtime.addOptions", required = false)
	protected String addOptions;

	/**
	 * The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.
	 */
	@Parameter(property = "inverno.runtime.compress", required = false)
	protected String compress;

	/**
	 * Links in service provider modules and their dependencies.
	 */
	@Parameter(property = "inverno.runtime.bindServices", defaultValue = "false", required = false)
	protected boolean bindServices;

	/**
	 * Suppresses a fatal error when signed modular JARs are linked in the runtime.
	 */
	@Parameter(property = "inverno.runtime.ignoreSigningInformation", defaultValue = "false", required = false)
	protected boolean ignoreSigningInformation;

	/**
	 * Strips debug information from the resulting runtime.
	 */
	@Parameter(property = "inverno.runtime.stripDebug", defaultValue = "true", required = false)
	protected boolean stripDebug = true;

	/**
	 * Strips native command (e.g. java...) from the resulting runtime.
	 */
	@Parameter(property = "inverno.runtime.stripNativeCommands", defaultValue = "true", required = false)
	protected boolean stripNativeCommands = true;

	/**
	 * Selects the HotSpot VM in the output image defined as: {@code "client" / "server" / "minimal" / "all"}.
	 */
	@Parameter(property = "inverno.runtime.vm", required = false)
	protected String vm;
	
	/**
	 * Adds unnamed modules when generating the runtime.
	 */
	@Parameter(property = "inverno.runtime.addUnnamedModules", defaultValue = "true", required = false)
	protected boolean addUnnamedModules;
	
	/**
	 * A list of launchers to include in the resulting runtime.
	 */
	@Parameter(required = false)
	protected List<RuntimeLauncherParameters> launchers;

	/* ArchiveTask */
	
	/**
	 * A list of archive formats to generate (e.g. zip, tar.gz...)
	 */
	@Parameter(property = "inverno.runtime.archiveFormats", required = true)
	protected Set<String> archiveFormats;
	
	/**
	 * The path to the runtime image within the archive.
	 */
	@Parameter(property = "inverno.runtime.archivePrefix", defaultValue = "${project.build.finalName}", required = false)
	protected String archivePrefix;
	
	/* Maven specific */

	/**
	 * Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
	 */
	@Parameter(property = "inverno.attach", defaultValue = "true", required = true)
	protected boolean attach = true;

	@Override
	protected boolean isSkipped() {
		return this.skip;
	}

	@Override
	protected void doExecute(MavenInvernoProject project) throws Exception {
		Set<Image> runtimeImages = new HashSet<>();
		runtimeImages.addAll(project
			.modularizeDependencies(this::configureTask)
			.buildJmod(this::configureTask)
			.buildRuntime(this::configureTask)
			.doOnComplete(runtimeImages::add)
			.archive(this::configureTask)
			.execute()
		);
		
		this.attachImages(runtimeImages);
	}
	
	/**
	 * <p>
	 * Attaches the specified images as project artifacts.
	 * </p>
	 * 
	 * @param images the images to attach
	 */
	protected void attachImages(Set<Image> images) {
		if(this.attach) {
			for(Image image : images) {
				image.getPath()
					.filter(path -> Files.exists(path) && Files.isRegularFile(path))
					.ifPresent(path -> {
						this.mavenProjectHelper.attachArtifact(this.mavenProject, image.getFormat().get(), image.getClassifier(), path.toFile());
					});
			}
		}
	}
	
	@Override
	protected MavenInvernoProject.Builder configureProject(MavenInvernoProject.Builder projectBuilder) {
		return projectBuilder
			.includeScope(this.includeScope)
			.excludeScope(this.excludeScope)
			.includeTypes(this.includeTypes)
			.excludeTypes(this.excludeTypes)
			.includeClassifiers(this.includeClassifiers)
			.excludeClassifiers(this.excludeClassifiers)
			.includeArtifactIds(this.includeArtifactIds)
			.excludeArtifactIds(this.excludeArtifactIds)
			.includeGroupIds(this.includeGroupIds)
			.excludeGroupIds(this.excludeGroupIds);
	}
	
	/**
	 * <p>
	 * Configures the build jmod task.
	 * </p>
	 * 
	 * @param buildJmodTask the build jmod task
	 * 
	 * @return the build jmod task
	 */
	protected BuildJmodTask configureTask(BuildJmodTask buildJmodTask) {
		return buildJmodTask
			.mainClass(this.projectMainClass)
			.resolveMainClass(this.resolveProjectMainClass)
			.configurationPath(this.configurationDirectory != null ? this.configurationDirectory.toPath().toAbsolutePath() : null)
			.legalPath(this.legalDirectory != null ? this.legalDirectory.toPath().toAbsolutePath() : null)
			.manPath(this.manDirectory != null ? this.manDirectory.toPath().toAbsolutePath() : null);
	}
	
	/**
	 * <p>
	 * Configures the build runtime task.
	 * </p>
	 * 
	 * @param buildRuntimeTask the build runtime task
	 * 
	 * @return the build runtime task
	 */
	protected BuildRuntimeTask configureTask(BuildRuntimeTask buildRuntimeTask) {
		return buildRuntimeTask
			.addModules(this.addModules)
			.addOptions(this.addOptions)
			.compress(this.compress)
			.bindServices(this.bindServices)
			.ignoreSigningInformation(this.ignoreSigningInformation)
			.stripDebug(this.stripDebug)
			.stripNativeCommands(this.stripNativeCommands)
			.vm(this.vm)
			.addUnnamedModules(this.addUnnamedModules)
			.launchers(this.launchers);
	}
	
	/**
	 * <p>
	 * Configures the archive task.
	 * </p>
	 * 
	 * @param archiveTask the archive task
	 * 
	 * @return the archive task
	 */
	protected ArchiveTask configureTask(ArchiveTask archiveTask) {
		return archiveTask
			.prefix(this.archivePrefix)
			.formats(this.archiveFormats);
	}
}
