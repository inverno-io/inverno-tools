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
package io.inverno.tool.maven.internal.task;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;

import io.inverno.tool.maven.internal.ProjectModule;
import io.inverno.tool.maven.internal.Task;
import io.inverno.tool.maven.internal.TaskExecutionException;
import io.inverno.tool.maven.internal.DependencyModule;
import io.inverno.tool.maven.internal.ProgressBar.Step;

/**
 * <p>
 * Executes the project module application.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class ExecuteProjectTask extends Task<Process> {

	private final ProjectModule projectModule;
	private final String javaCommand;
	
	private Optional<String> mainClass = Optional.empty();
	private Optional<String> arguments = Optional.empty();
	private Optional<String> vmOptions = Optional.empty();
	private Optional<Path> workingPath = Optional.empty();
	
	private boolean addUnnamedModules;
	
	public ExecuteProjectTask(AbstractMojo mojo, ProjectModule projectModule) {
		super(mojo);
		this.projectModule = projectModule;
		this.javaCommand = Paths.get(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
	}
	
	@Override
	public void setStep(Step step) {
		if(step != null) {
			step.setDescription("Running project: " + this.projectModule + "...");
		}
		super.setStep(step);
	}

	@Override
	protected Process execute() throws TaskExecutionException {
		if(this.verbose) {
			this.getLog().info("[ Running project: " + this.projectModule + "... ]");
		}
		
		List<String> java_command = new LinkedList<>();
		java_command.add(0, this.javaCommand);
		
		String dependenciesModulePath = this.projectModule.getModuleDependencies().stream().map(dependencyModule -> dependencyModule.getJmodPath().toString()).collect(Collectors.joining(System.getProperty("path.separator")));
		String projectModulePath = this.projectModule.getClassesPath().toString();
		
		String jlink_modulePath = String.join(System.getProperty("path.separator"), dependenciesModulePath, projectModulePath);
		
		if(this.vmOptions.map(this::sanitizeArguments).isPresent()) {
			try {
				java_command.addAll(this.translateArguments(this.vmOptions.get()));
			} 
			catch (IllegalArgumentException e) {
				throw new TaskExecutionException("Invalid vm options", e);
			}
		}
		
		if(this.addUnnamedModules) {
			String unnamedModules = this.projectModule.getModuleDependencies().stream().filter(dependency -> !dependency.isNamed()).map(DependencyModule::getModuleName).collect(Collectors.joining(","));
			if(StringUtils.isNotEmpty(unnamedModules)) {
				java_command.add("--add-modules");
				java_command.add(unnamedModules);
			}
		}
		
		java_command.add("--module-path");
		java_command.add(jlink_modulePath);
		
		String moduleMainClass = this.mainClass.or(() -> {
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
			return Optional.empty();
		}).orElseThrow(() -> new TaskExecutionException("Main project class is missing"));
		
		java_command.add("--module");
		java_command.add(this.projectModule.getModuleName() + "/" + moduleMainClass);
		
		if(this.arguments.map(this::sanitizeArguments).isPresent()) {
			try {
				java_command.addAll(this.translateArguments(this.arguments.get()));
			} 
			catch (IllegalArgumentException e) {
				throw new TaskExecutionException("Invalid arguments", e);
			}
		}
		
		if(this.verbose) {
			this.getLog().info(" - " + java_command.stream().collect(Collectors.joining(" ")));
		}
		
		ProcessBuilder pb = new ProcessBuilder(java_command);
		this.workingPath.map(Path::toFile).ifPresent(pb::directory);
		pb.inheritIO();
		
		try {
			return pb.start();
		} 
		catch (IOException e) {
			throw new TaskExecutionException("Error running project, activate '-Dinverno.verbose=true' to display full log", e);
		}
	}

	public Optional<String> getMainClass() {
		return mainClass;
	}

	public void setMainClass(Optional<String> mainClass) {
		this.mainClass = mainClass;
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

	public Optional<Path> getWorkingPath() {
		return workingPath;
	}

	public void setWorkingPath(Optional<Path> workingPath) {
		this.workingPath = workingPath;
	}

	public boolean isAddUnnamedModules() {
		return addUnnamedModules;
	}

	public void setAddUnnamedModules(boolean addUnnamedModules) {
		this.addUnnamedModules = addUnnamedModules;
	}
}
