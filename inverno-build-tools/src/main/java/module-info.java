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

/**
 * <p>
 * The Inverno Build Tools module provides tools for running, packaging and distributing modular Java applications.
 * </p>
 * 
 * <p>
 * Considering a modular Java project with heterogeneous dependencies, it basically allows to:
 * </p>
 * 
 * <ul>
 * <li>Modularize automatic and unnamed dependencies</li>
 * <li>Run or start/stop the project application in a separated process</li>
 * <li>Create a JMOD archive of the project</li>
 * <li>Build an optimized Java runtime containing the project, its dependencies along with an optimized specific Java runtime.</li>
 * <li>Package the project application in optimized native delivrables</li>
 * <li>Create a container image of the project application as a TAR archive or load it to a local Docker daemon or deploy it to a remote image registry.</li>
 * </ul>
 * 
 * <p>
 * A modular Java project can be fully modularized and packaged in a Debian package, ready to be deployed as follows:
 * </p>
 * 
 * <pre>{@code
 * Project project = ... 
 * 
 * Set<Path> applicationPaths = this.project
 *     .modularizeDependencies()
 *     .buildJmod()
 *     .buildRuntime()
 *     .packageApplication()
 *     .copyright("Copyright 2023 Inverno")
 *     .vendor("Inverno Framework")
 *     .licensePath(Path.of("src/test/resources/legal/LICENSE"))
 *     .types(Set.of(PackageApplicationTask.PackageType.DEB))
 *     .execute();    
 * }</pre>
 * 
 * <p>
 * A modular Java project can be containerized and loaded directly to the local Docker daemon as follows:
 * </p>
 * 
 * <pre>{@code
 * Project project = ...
 * 
 * ContainerizeTask.ContainerImageRef imageReference = this.project
 *     .modularizeDependencies()
 *     .buildJmod()
 *     .buildRuntime()
 *     .packageApplication()
 *     .containerize()
 *     .target(Target.DOCKER)
 *     .execute();
 * }</pre>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
module io.inverno.tool.buildtools {
	requires com.google.cloud.tools.jib;
	requires com.google.cloud.tools.jib.api.buildplan;
	requires org.apache.commons.compress;
	requires org.apache.commons.lang3;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.iostreams;
	
	exports io.inverno.tool.buildtools;
	
	uses java.util.spi.ToolProvider;
}
