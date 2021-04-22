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
package io.winterframework.tool.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;

/**
 * <p>
 * Base project image mojo.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
abstract class AbstractImageMojo extends AbstractWinterMojo {

	@Component
	protected MavenProjectHelper projectHelper;

	/**
	 * Overwrite dependencies that don't exist or are older than the source.
	 */
	@Parameter(property = "winter.image.overWriteIfNewer", defaultValue = "true", required = false)
	protected boolean overWriteIfNewer;

	/*
	 * I don't see anything else than jar for now, we could consider jmod but jmod
	 * is not for runtime so having it define as a project artifacts is less likely
	 */
	/**
	 * Comma Separated list of Types to include. Empty String indicates include
	 * everything (default).
	 */
//    @Parameter( property = "winter.image.includeTypes", defaultValue = "jar" )
	protected String includeTypes = "jar";

	/**
	 * Comma Separated list of Types to exclude. Empty String indicates don't
	 * exclude anything (default).
	 */
//    @Parameter( property = "winter.image.excludeTypes", defaultValue = "" )
	protected String excludeTypes = "";

	/**
	 * Scope to include. An Empty string indicates all scopes (default). The scopes
	 * being interpreted are the scopes as Maven sees them, not as specified in the
	 * pom. In summary:
	 * <ul>
	 * <li><code>runtime</code> scope gives runtime and compile dependencies,</li>
	 * <li><code>compile</code> scope gives compile, provided, and system
	 * dependencies,</li>
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
	 * Comma Separated list of Classifiers to include. Empty String indicates
	 * include everything (default).
	 */
	@Parameter(property = "winter.image.includeClassifiers", defaultValue = "", required = false)
	protected String includeClassifiers;

	/**
	 * Comma Separated list of Classifiers to exclude. Empty String indicates don't
	 * exclude anything (default).
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

	/**
	 * The main class to use to build the image. If not specified, a main class is
	 * automatically selected.
	 */
	@Parameter(property = "winter.image.mainClass", required = false)
	protected String mainClass;

	/**
	 * A directory containing module descriptors to use to modularize unnamed
	 * dependency modules and which override the ones that are otherwise generated.
	 */
	@Parameter(property = "winter.image.jmodsOverrideDirectory", defaultValue = "${project.basedir}/src/jmods/", required = false)
	protected File jmodsOverrideDirectory;

	/**
	 * A directory containing user-editable configuration files that will be copied
	 * to the resulting image.
	 */
	@Parameter(property = "winter.image.configurationDirectory", defaultValue = "${project.basedir}/src/main/conf/", required = false)
	protected File configurationDirectory;

	/**
	 * A directory containing legal notices that will be copied to the resulting
	 * image.
	 */
	@Parameter(property = "winter.image.legalDirectory", defaultValue = "${project.basedir}/src/main/legal/", required = false)
	protected File legalDirectory;

	/**
	 * A directory containing man pages that will be copied to the resulting
	 * image.
	 */
	@Parameter(property = "winter.image.manDirectory", defaultValue = "${project.basedir}/src/main/man/", required = false)
	protected File manDirectory;

	/**
	 * The modules to add to the resulting image.
	 */
	@Parameter(property = "winter.image.addModules", required = false)
	protected String addModules;

	/**
	 * The options to prepend before any other options when invoking the JVM in the
	 * resulting image.
	 */
	@Parameter(property = "winter.image.addOptions", required = false)
	protected String addOptions;

	/**
	 * The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.
	 */
	@Parameter(property = "winter.image.compress", required = false)
	protected String compress;

	/**
	 * Link in service provider modules and their dependencies.
	 */
	@Parameter(property = "winter.image.bindServices", defaultValue = "false", required = false)
	protected boolean bindServices;

	/**
	 * Suppress a fatal error when signed modular JARs are linked in the image.
	 */
	@Parameter(property = "winter.image.ignoreSigningInformation", defaultValue = "false", required = false)
	protected boolean ignoreSigningInformation;

	/**
	 * Strip debug information from the resulting image.
	 */
	@Parameter(property = "winter.image.stripDebug", defaultValue = "true", required = false)
	protected boolean stripDebug;

	/**
	 * Strip native command (eg. java...) from the resulting image.
	 */
	@Parameter(property = "winter.image.stripNativeCommands", defaultValue = "true", required = false)
	protected boolean stripNativeCommands;

	/**
	 * Select the HotSpot VM in the output image defined as: {@code "client" / "server" / "minimal" / "all"}
	 */
	@Parameter(property = "winter.image.vm", required = false)
	protected String vm;

	/**
	 * A list of archive formats to generate (eg. zip, tar.gz...)
	 */
	@Parameter(defaultValue = "zip", required = true)
	protected Set<String> formats;

	/**
	 * Attach the resulting image archives to the project to install them in the
	 * local Maven repository and deploy them to remote repositories.
	 */
	@Parameter(property = "winter.image.attach", defaultValue = "true", required = true)
	protected boolean attach;

	// src
	protected Optional<Path> jmodsOverridePath;
	protected Optional<Path> confPath;
	protected Optional<Path> legalPath;
	protected Optional<Path> manPath;

	// target
	protected Path jmodsExplodedPath;
	protected Path jmodsPath;
	protected Path launchersPath;

	/*
	 * https://docs.oracle.com/en/java/javase/16/docs/specs/man/javac.html
	 */
	protected ToolProvider javac;
	/*
	 * https://docs.oracle.com/en/java/javase/16/docs/specs/man/jar.html
	 */
	protected ToolProvider jar;
	/*
	 * https://docs.oracle.com/en/java/javase/16/docs/specs/man/jdeps.html
	 */
	protected ToolProvider jdeps;
	/*
	 * https://docs.oracle.com/en/java/javase/16/docs/specs/man/jmod.html
	 */
	protected ToolProvider jmod;
	/*
	 * https://docs.oracle.com/en/java/javase/16/docs/specs/man/jlink.html
	 */
	protected ToolProvider jlink;
	/*
	 * https://docs.oracle.com/en/java/javase/16/docs/specs/man/jpackage.html
	 */
	protected ToolProvider jpackage;

	public AbstractImageMojo() {
		ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).forEach(toolProvider -> {
			switch (toolProvider.name()) {
			case "javac":
				this.javac = toolProvider;
				break;
			case "jar":
				this.jar = toolProvider;
				break;
			case "jdeps":
				this.jdeps = toolProvider;
				break;
			case "jmod":
				this.jmod = toolProvider;
				break;
			case "jlink":
				this.jlink = toolProvider;
				break;
			case "jpackage":
				this.jpackage = toolProvider;
				break;
			}
		});
	}

	protected void initializePaths() throws IOException {
		super.initializePaths();

		this.jmodsOverridePath = Optional.ofNullable(this.jmodsOverrideDirectory).map(f -> f.toPath().toAbsolutePath());
		this.confPath = Optional.ofNullable(this.configurationDirectory).map(f -> f.toPath().toAbsolutePath());
		this.legalPath = Optional.ofNullable(this.legalDirectory).map(f -> f.toPath().toAbsolutePath());
		this.manPath = Optional.ofNullable(this.manDirectory).map(f -> f.toPath().toAbsolutePath());

		this.jmodsExplodedPath = this.winterBuildPath.resolve("jmods-exploded").toAbsolutePath();
		Files.createDirectories(this.jmodsExplodedPath);

		this.jmodsPath = this.winterBuildPath.resolve("jmods").toAbsolutePath();
		Files.createDirectories(this.jmodsPath);

		this.launchersPath = this.winterBuildPath.resolve("launchers").toAbsolutePath();
	}
}
