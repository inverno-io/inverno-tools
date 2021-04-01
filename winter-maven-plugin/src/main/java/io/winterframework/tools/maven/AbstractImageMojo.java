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
package io.winterframework.tools.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * @author jkuhn
 *
 */
abstract class AbstractImageMojo extends AbstractMojo {
	
	@Component
	protected ArchiverManager archiverManager;
	
	@Component
	protected MavenProjectHelper projectHelper;
	
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;
    
	@Parameter(property = "winter.image.workingDirectory", defaultValue = "${project.build.directory}/maven-winter", required = true)
	protected File workingDirectory;
	
	@Parameter(property = "winter.image.verbose", defaultValue = "false", required = false)
	protected boolean verbose;
	
	@Parameter(property = "winter.image.overWriteIfNewer", defaultValue = "true", required = false)
	protected boolean overWriteIfNewer;
    
	/*
	 * I don't see anything else than jar for now, we could consider jmod but jmod
	 * is not for runtime so having it define as a project artifacts is less likely
	 */
	/**
     * Comma Separated list of Types to include. Empty String indicates include everything (default).
     */
//    @Parameter( property = "winter.image.includeTypes", defaultValue = "jar" )
    protected String includeTypes = "jar";

    /**
     * Comma Separated list of Types to exclude. Empty String indicates don't exclude anything (default).
     */
//    @Parameter( property = "winter.image.excludeTypes", defaultValue = "" )
    protected String excludeTypes = "";
	
	/**
     * Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as
     * Maven sees them, not as specified in the pom. In summary:
     * <ul>
     * <li><code>runtime</code> scope gives runtime and compile dependencies,</li>
     * <li><code>compile</code> scope gives compile, provided, and system dependencies,</li>
     * <li><code>test</code> (default) scope gives all dependencies,</li>
     * <li><code>provided</code> scope just gives provided dependencies,</li>
     * <li><code>system</code> scope just gives system dependencies.</li>
     * </ul>
     */
    @Parameter(property = "winter.image.includeScope", defaultValue = "", required = false)
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default).
     */
    @Parameter(property = "winter.image.excludeScope", defaultValue = "", required = false)
    protected String excludeScope;
    
    /**
     * Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
     */
    @Parameter(property = "winter.image.includeClassifiers", defaultValue = "", required = false)
    protected String includeClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
     */
    @Parameter(property = "excludeClassifiers", defaultValue = "", required = false)
    protected String excludeClassifiers;
	
	/**
	 * Comma separated list of Artifact names to exclude.
	 */
	@Parameter(property = "winter.image.excludeArtifactIds", defaultValue = "", required = false)
	protected String excludeArtifactIds;

	/**
	 * Comma separated list of Artifact names to include. Empty String indicates
	 * include everything (default).
	 */
	@Parameter(property = "winter.image.includeArtifactIds", defaultValue = "", required = false)
	protected String includeArtifactIds;

	/**
	 * Comma separated list of GroupId Names to exclude.
	 */
	@Parameter(property = "winter.image.excludeGroupIds", defaultValue = "", required = false)
	protected String excludeGroupIds;

	/**
	 * Comma separated list of GroupIds to include. Empty String indicates include
	 * everything (default).
	 */
	@Parameter(property = "winter.image.includeGroupIds", defaultValue = "", required = false)
	protected String includeGroupIds;
	
	@Parameter(property = "winter.image.mainClass", required = false)
	protected String mainClass;

	@Parameter(property = "winter.image.jmodsOverrideDirectory", defaultValue = "${project.basedir}/src/jmods/", required = false)
	protected File jmodsOverrideDirectory;
	
	@Parameter(property = "winter.image.configurationDirectory", defaultValue = "${project.basedir}/src/main/conf/", required = false)
	protected File configurationDirectory;
	
	@Parameter(property = "winter.image.legalDirectory", defaultValue = "${project.basedir}/src/main/legal/", required = false)
	protected File legalDirectory;
	
	@Parameter(property = "winter.image.manDirectory", defaultValue = "${project.basedir}/src/main/man/", required = false)
	protected File manDirectory;
	
	@Parameter(property = "winter.image.addModules", required = false)
	protected String addModules;
	
	@Parameter(property = "winter.image.addOptions", required = false)
	protected String addOptions;
	
	@Parameter(property = "winter.image.compress", required = false)
	protected String compress;
	
	@Parameter(property = "winter.image.bindServices", defaultValue = "false", required = false)
	protected boolean bindServices;
	
	@Parameter(property = "winter.image.ignoreSigningInformation", defaultValue = "false", required = false)
	protected boolean ignoreSigningInformation;
	
	@Parameter(property = "winter.image.stripDebug", defaultValue = "true", required = false)
	protected boolean stripDebug;
	
	@Parameter(property = "winter.image.stripNativeCommands", defaultValue = "true", required = false)
	protected boolean stripNativeCommands;
	
	@Parameter(property = "winter.image.vm", required = false)
	protected String vm;
	
	@Parameter(defaultValue = "zip", required = true)
	protected Set<String> formats;

	@Parameter(property = "winter.image.attach", defaultValue = "true", required = true)
	protected boolean attach;
	
	// src
	protected Optional<Path> projectJModsOverridePath;
	protected Optional<Path> projectConfPath;
	protected Optional<Path> projectLegalPath;
	protected Optional<Path> projectManPath;
	
	// target
	protected Path workingPath;
	protected Path jmodsExplodedPath;
	protected Path jmodsPath;
	protected Path launchersPath;
	
 	/*
 	 *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/javac.html
 	 */
	protected ToolProvider javac;
 	/*
 	 *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/jar.html
 	 */
	protected ToolProvider jar;
 	/*
     *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/jdeps.html
     */
	protected ToolProvider jdeps;
 	/*
 	 *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/jmod.html
 	 */
	protected ToolProvider jmod;
 	/*
 	 *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/jlink.html
 	 */
	protected ToolProvider jlink;
 	/*
 	 *  https://docs.oracle.com/en/java/javase/16/docs/specs/man/jpackage.html
 	 */
	protected ToolProvider jpackage;
    
	public AbstractImageMojo() {
		ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).forEach(toolProvider -> {
			switch(toolProvider.name()) {
				case "javac": this.javac = toolProvider;
					break;
				case "jar": this.jar = toolProvider;
					break;
				case "jdeps": this.jdeps = toolProvider;
					break;
				case "jmod": this.jmod = toolProvider;
					break;
				case "jlink": this.jlink = toolProvider;
					break;
				case "jpackage": this.jpackage = toolProvider;
					break;
			}
		});
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			this.initializePaths();
		}
		catch (IOException e) {
			throw new MojoFailureException("Error initializing paths", e);
		}
		this.doExecute();
	}
	
	protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;
	
	protected void initializePaths() throws IOException {
		this.workingPath = this.workingDirectory.toPath().toAbsolutePath();
		
		this.projectJModsOverridePath = Optional.ofNullable(this.jmodsOverrideDirectory).map(f -> f.toPath().toAbsolutePath());
		this.projectConfPath = Optional.ofNullable(this.configurationDirectory).map(f -> f.toPath().toAbsolutePath());
		this.projectLegalPath = Optional.ofNullable(this.legalDirectory).map(f -> f.toPath().toAbsolutePath());
		this.projectManPath = Optional.ofNullable(this.manDirectory).map(f -> f.toPath().toAbsolutePath());
		
		this.jmodsExplodedPath = this.workingPath.resolve("jmods-exploded").toAbsolutePath();
		Files.createDirectories(this.jmodsExplodedPath);
		
		this.jmodsPath = this.workingPath.resolve("jmods").toAbsolutePath();
		Files.createDirectories(this.jmodsPath);
		
		this.launchersPath = this.workingPath.resolve("launchers").toAbsolutePath();
	} 
	
	protected void displayProgress(float percentage) {
		if(!this.verbose) {
			StringBuilder progressBar = new StringBuilder();
			progressBar.append(" [");
			if(percentage < 1) {
				for(int i=0;i<100;i++) {
					if((float)i/100 < percentage) {
						progressBar.append("\u2550");
					}
					else {
						progressBar.append(" ");
					}
				}
				progressBar.append("]\r");
				System.out.print(progressBar.toString());
			}
			else {
				for(int i=0;i<45;i++) {
					progressBar.append("\u2550");
				}
				progressBar.append(" COMPLETE ");
				for(int i=0;i<45;i++) {
					progressBar.append("\u2550");
				}
				progressBar.append("]");
				System.out.println(progressBar.toString());
			}
		}
	}
}
