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
package io.inverno.tool.buildtools;

import java.nio.file.Path;

/**
 * <p>
 * The {@link Artifact} representing a {@link Project} dependency.
 * </p>
 * 
 * <p>
 * A proper implementation must provide dependency information like group, name and version as the path to the JAR archive that will be used when building project runtime and packaging project 
 * application.
 * </p>
 * 
 * <p>
 * A project dependency is basically a JAR containing a Java module with a proper module descriptor, an automatic module with a {@code Automatic-Module-Name} entry defined in its {@code MANIFEST.MF} 
 * or an unnamed module. The two latter are modularized by the {@link ModularizeDependenciesTask} task in order to run the project, build project runtime or package project application.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface Dependency extends Artifact {

	/**
	 * <p>
	 * Returns the path to the dependency JAR.
	 * </p>
	 * 
	 * @return the path to the dependency JAR
	 */
	Path getJarPath();
}
