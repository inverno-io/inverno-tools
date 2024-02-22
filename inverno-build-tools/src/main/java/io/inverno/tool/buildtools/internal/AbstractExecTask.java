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

import io.inverno.tool.buildtools.ExecTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Base {@link ExecTask} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <U> the type returned by the task execution
 * @param <V> the type of the task
 */
public abstract class AbstractExecTask<U, V extends ExecTask<U, V>> extends AbstractTask<U, V> implements ExecTask<U, V> {

	private static final Logger LOGGER = LogManager.getLogger(AbstractExecTask.class);
	
	public static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
	
	protected Optional<String> mainClass = Optional.empty();
	protected Optional<String> arguments = Optional.empty();
	protected Optional<String> vmOptions = Optional.empty();
	protected Optional<Path> workingPath = Optional.empty();
	protected boolean addUnnamedModules = true;
	protected Optional<ProcessBuilder.Redirect> redirectInput = Optional.empty();
	protected Optional<ProcessBuilder.Redirect> redirectOutput = Optional.empty();
	protected Optional<ProcessBuilder.Redirect> redirectError = Optional.empty();

	/**
	 * <p>
	 * Create an exec task.
	 * </p>
	 * 
	 * @param parentTask the parent task.
	 */
	public AbstractExecTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public V mainClass(String mainClass) {
		this.mainClass = Optional.ofNullable(mainClass);
		return (V)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V arguments(String arguments) {
		this.arguments = Optional.ofNullable(arguments);
		return (V)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V vmOptions(String vmOptions) {
		this.vmOptions = Optional.ofNullable(vmOptions);
		return (V)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V workingPath(Path workingPath) {
		this.workingPath = Optional.ofNullable(workingPath);
		return (V)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V addUnnamedModules(boolean addUnnamedModules) {
		this.addUnnamedModules = addUnnamedModules;
		return (V)this;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public V redirectInput(ProcessBuilder.Redirect redirectInput) {
		this.redirectInput = Optional.ofNullable(redirectInput);
		return (V)this;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public V redirectOutput(ProcessBuilder.Redirect redirectOutput) {
		this.redirectOutput = Optional.ofNullable(redirectOutput);
		return (V)this;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public V redirectError(ProcessBuilder.Redirect redirectError) {
		this.redirectError = Optional.ofNullable(redirectError);
		return (V)this;
	}
	
	/**
	 * <p>
	 * Starts the project application and returns the corresponding process.
	 * </p>
	 * 
	 * @param project the project to start
	 * 
	 * @return a process
	 * 
	 * @throws TaskExecutionException if there was an error starting the application
	 */
	protected Process startProject(BuildProject project) throws TaskExecutionException {
		List<String> java_command = new LinkedList<>();
		java_command.add(JAVA);
		
		String dependenciesJmodPath = project.getDependencies().stream().map(dependencyModule -> dependencyModule.getModulePath().toString()).collect(Collectors.joining(System.getProperty("path.separator")));
		String projectClassesPath = project.getClassesPath().toString();
		
		String jlink_modulePath = String.join(System.getProperty("path.separator"), dependenciesJmodPath, projectClassesPath);
		
		if(this.vmOptions.map(JavaTools::sanitizeArguments).isPresent()) {
			try {
				java_command.addAll(JavaTools.translateArguments(this.vmOptions.get()));
			} 
			catch (IllegalArgumentException e) {
				throw new TaskExecutionException("Invalid vm options", e);
			}
		}
		
		if(this.addUnnamedModules) {
			String unnamedModules = project.getDependencies().stream()
				.filter(dependency -> !dependency.isNamed())
				.map(BuildDependency::getModuleName)
				.collect(Collectors.joining(","));
			if(StringUtils.isNotBlank(unnamedModules)) {
				java_command.add("--add-modules");
				java_command.add(unnamedModules);
			}
		}
		
		java_command.add("--module-path");
		java_command.add(jlink_modulePath);
		
		String moduleMainClass = this.mainClass.or(() -> {
			try {
				return project.getDefaultMainClass().map(defaultMainClass -> {
					LOGGER.info(" - no main class specified, defaulting to {}", defaultMainClass);
					return defaultMainClass;
				});
			} 
			catch (ClassNotFoundException | IOException e) {
				LOGGER.error("Could not find project main class", e);
			}
			return Optional.empty();
		}).orElseThrow(() -> new TaskExecutionException("Main project class is missing"));
		
		java_command.add("--module");
		java_command.add(project.getModuleName() + "/" + moduleMainClass);
		
		if(this.arguments.map(JavaTools::sanitizeArguments).isPresent()) {
			try {
				java_command.addAll(JavaTools.translateArguments(this.arguments.get()));
			} 
			catch (IllegalArgumentException e) {
				throw new TaskExecutionException("Invalid arguments", e);
			}
		}
		
		LOGGER.info(" - {}", java_command.stream().collect(Collectors.joining(" ")));
		
		ProcessBuilder pb = new ProcessBuilder(java_command);
		this.workingPath.filter(Files::exists).map(Path::toFile).ifPresent(pb::directory);

		pb.redirectInput(this.redirectInput.orElse(ProcessBuilder.Redirect.INHERIT));
		pb.redirectOutput(this.redirectOutput.orElse(ProcessBuilder.Redirect.INHERIT));
		pb.redirectError(this.redirectError.orElse(ProcessBuilder.Redirect.INHERIT));
		
		try {
			return pb.start();
		} 
		catch (IOException e) {
			throw new TaskExecutionException("Error running project", e);
		}
	}
}
