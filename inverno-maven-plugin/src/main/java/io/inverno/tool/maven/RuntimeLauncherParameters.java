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

import io.inverno.tool.buildtools.BuildRuntimeTask;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Parameters for the creation of an runtime launcher.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class RuntimeLauncherParameters  implements BuildRuntimeTask.Launcher {
		
	/**
	 * The name of the runtime launcher.
	 */
	@Parameter(required = true)
	private String name;

	/**
	 * The module containing the main class of the runtime launcher. If not specified the project's module is selected.
	 */
	@Parameter(required = false)
	private String module;

	/**
	 * The main class of the runtime launcher. If not specified the specified module must provide a main class.
	 */
	@Parameter(required = false)
	private String mainClass;

	/**
	 * <p>
	 * Sets the name of the runtime launcher.
	 * </p>
	 * 
	 * @param name the launcher name
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * <p>
	 * Sets the module containing the main class of the runtime launcher.
	 * </p>
	 * 
	 * @param module the module
	 */
	public void setModule(String module) {
		this.module = module;
	}

	@Override
	public Optional<String> getModule() {
		return Optional.ofNullable(this.module).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the canonical name of the main class executed with the runtime launcher.
	 * <p>
	 * 
	 * @param mainClass the main class
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	@Override
	public Optional<String> getMainClass() {
		return Optional.ofNullable(this.mainClass).filter(StringUtils::isNotEmpty);
	}
}