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

import io.inverno.tool.buildtools.ExecTask;
import java.io.File;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Base project execution mojo.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractExecMojo extends AbstractInvernoMojo {

	/**
	 * The main class to use to run the application. If not specified, one of the main class in the project module is automatically selected.
	 */
	@Parameter(property = "inverno.exec.mainClass", required = false)
	protected String mainClass;
	
	/**
	 * The arguments to pass to the application.
	 */
	@Parameter
	protected String arguments;
	
	/**
	 * The VM options to use when executing the application.
	 */
	@Parameter(property = "inverno.exec.vmOptions", defaultValue = "-Dorg.apache.logging.log4j.simplelog.level=INFO -Dorg.apache.logging.log4j.level=INFO", required = false)
	protected String vmOptions;
	
	/**
	 * The working directory of the application.
	 */
	@Parameter(property = "inverno.exec.workingDirectory", defaultValue = "${project.build.directory}/maven-inverno/working", required = false)
	protected File workingDirectory;
	
	/**
	 * Adds the unnamed modules when executing the application.
	 */
	@Parameter(property = "inverno.exec.addUnnamedModules", defaultValue = "true", required = false)
	protected boolean addUnnamedModules = true;
	
	/**
	 * <p>
	 * Configures the exec task.
	 * </p>
	 * 
	 * @param <U> the type returned by the task execution
	 * @param <V> the type of the task
	 * 
	 * @param execTask the exec task
	 * 
	 * @return the exec task
	 */
	protected <U, V extends ExecTask<U, V>> ExecTask<U, V> configureTask(ExecTask<U, V> execTask) {
		return execTask
			.mainClass(this.mainClass)
			.arguments(this.arguments)
			.vmOptions(this.vmOptions)
			.workingPath(this.workingDirectory != null ? this.workingDirectory.toPath().toAbsolutePath() : null)
			.addUnnamedModules(this.addUnnamedModules);
	}
}
