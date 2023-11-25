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
package io.inverno.tool.buildtools;

import io.inverno.tool.buildtools.internal.GenericModularizeDependenciesTask;
import io.inverno.tool.buildtools.internal.GenericStopTask;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * The {@link Artifact} representing the project to build.
 * </p>
 * 
 * <p>
 * A proper implementation must provide project information like group, name and version as well as the paths to project resources such as project's classes, working folder uded for intermediary 
 * build resources and a target folder where project's deliverables are generated.
 * </p>
 * 
 * <p>
 * Build tasks execution is hierarchical, some tasks depends on the others, consistent sequences of build tasks are fluently created from the {@code Project}. For instance, the following shows how to 
 * package an application:
 * </p>
 * 
 * <pre>{@code
 * Project project = ...
 * 
 * project
 *     .modularizeDependencies()           // Modularize project's dependencies
 *     .buildJmod()                        // Build the project's module JMOD
 *     .buildRuntime()                     // Create the project's runtime
 *     .packageApplication()               // Create the project's application package (image + .deb)
 *         .types(Set.of(PackageType.DEB)) 
 *     .archive()                          // Archive the project's application package (.zip)
 *         .formats(Set.of("zip"))
 *     .execute();
 * }</pre>
 * 
 * <p>
 * Note that build tasks does not operate on project sources but on compiled sources and packaged dependencies (JAR) which results from external build tools execution (e.g. Maven, Gradle...) for 
 * which specific {@link Project} implementations shall be provided.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public abstract class Project implements Artifact {
	
	/**
	 * System property to set to display or not the progress bar while executing the build tasks.
	 */
	public static final String PROPERY_DISPLAY_PROGRESS_BAR = "inverno.build.progress_bar";
	
	/**
	 * The target path where build delivrables are generated.
	 */
	private final Path targetPath;
	
	/**
	 * The working path where intermediary build states are generated.
	 */
	private final Path workingPath;

	/**
	 * <p>
	 * Creates a project.
	 * </p>
	 *
	 * @param targetPath  the target path where project deliverables are generated
	 * @param workingPath the working path used to generate intermediate build states, default to {@code targetPath} if null
	 */
	public Project(Path targetPath, Path workingPath) {
		this.targetPath = Objects.requireNonNull(targetPath.toAbsolutePath());
		this.workingPath = workingPath != null ? workingPath.toAbsolutePath() : this.targetPath;
	}
	
	/**
	 * <p>
	 * Returns the path to the classes directory which contains project module {@code .class} files.
	 * </p>
	 *
	 * @return the path to the project module classes
	 */
	public abstract Path getClassesPath();
	
	/**
	 * <p>
	 * Returns project module dependencies.
	 * </p>
	 * 
	 * @return a set of dependency modules
	 */
	public abstract Set<? extends Dependency> getDependencies();
	
	/**
	 * <p>
	 * Returns the target path where project deliverables are generated.
	 * </p>
	 * 
	 * @return the target path
	 */
	public Path getTargetPath() {
		return targetPath;
	}

	/**
	 * <p>
	 * the working path where intermediate build states are generated.
	 * </p>
	 * 
	 * @return the working path
	 */
	public Path getWorkingPath() {
		return workingPath;
	}
	
	/**
	 * <p>
	 * Returns the final name to use when generating project deliverables.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link #getName() }-{@link #getVersion() }
	 * </p>
	 * 
	 * @return the final name of the project
	 */
	public String getFinalName() {
		return this.getName() + "-" + this.getVersion();
	}

	/**
	 * <p>
	 * Creates a modularize dependencies task.
	 * </p>
	 * 
	 * @return a modularized dependencies task
	 */
	public final ModularizeDependenciesTask modularizeDependencies() {
		return new GenericModularizeDependenciesTask(this);
	}
	
	/**
	 * <p>
	 * Creates and configure a modularize dependencies task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured modularized dependencies task
	 */
	public final ModularizeDependenciesTask modularizeDependencies(Consumer<ModularizeDependenciesTask> configurer) {
		ModularizeDependenciesTask modularizeDependencies = this.modularizeDependencies();
		configurer.accept(modularizeDependencies);
		return modularizeDependencies;
	}
	
	/**
	 * <p>
	 * Creates a stop task.
	 * </p>
	 * 
	 * @return a stop task
	 */
	public final StopTask stop() {
		return new GenericStopTask(this);
	}
	
	/**
	 * <p>
	 * Creates and configures a stop task.
	 * </p>
	 * 
	 * @param configurer a configurer
	 * 
	 * @return a configured stop task
	 */
	public final StopTask stop(Consumer<StopTask> configurer) {
		StopTask stop = this.stop();
		configurer.accept(stop);
		return stop;
	}
}
