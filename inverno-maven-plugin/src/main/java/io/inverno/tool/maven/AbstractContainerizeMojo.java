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

import io.inverno.tool.buildtools.ContainerizeTask;
import java.util.Map;
import java.util.Set;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Base containerize mojo.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public abstract class AbstractContainerizeMojo extends PackageApplicationMojo {
	
	/**
	 * The base container image.
	 */
	@Parameter(property = "inverno.container.from", defaultValue = "debian:buster-slim", required = true)
	protected String from;
	
	/**
	 * The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.
	 */
	@Parameter(property = "inverno.container.executable", defaultValue = "${project.artifactId}", required = true)
	protected String executable;
	
	/**
	 * The format of the container image.
	 */
	@Parameter(property = "inverno.container.imageFormat", defaultValue = "Docker", required = false)
	protected ContainerizeTask.Format imageFormat;
	
	/**
	 * The repository part of the target image reference defined as: <code>${registry}/${repository}/${name}:${project.version}</code>
	 */
	@Parameter(property = "inverno.container.repository", required = false)
	protected String repository;
	
	/**
	 * The registry part of the target image reference defined as: <code>${registry}/${repository}/${name}:${project.version}</code>
	 */
	@Parameter(property = "inverno.container.registry", required = false)
	protected String registry;
	
	/**
	 * The labels to apply to the container image.
	 */
	@Parameter(required = false)
	protected Map<String, String> labels;
	
	/**
	 * The ports exposed by the container at runtime defined as: {@code port_number [ "/" udp/tcp ] }.
	 */
	@Parameter(required = false)
	protected Set<String> ports;
	
	/**
	 * The container's mount points.
	 */
	@Parameter(required = false)
	protected Set<String> volumes;
	
	/**
	 * The user and group used to run the container defined as: {@code user / uid [ ":" group / gid ]}.
	 */
	@Parameter(required = false)
	protected String user;
	
	/**
	 * The container's environment variables.
	 */
	@Parameter(required = false)
	protected Map<String, String> environment;
	
	/**
	 * <p>
	 * Configures the containerize task.
	 * </p>
	 * 
	 * @param containerizeTask the containerize task
	 * 
	 * @return the containerize task
	 */
	protected ContainerizeTask configureTask(ContainerizeTask containerizeTask) {
		return containerizeTask
			.target(ContainerizeTask.Target.TAR)
			.from(this.from)
			.executable(this.executable)
			.format(this.imageFormat)
			.repository(this.repository)
			.registry(this.registry)
			.labels(this.labels)
			.ports(this.ports)
			.volumes(this.volumes)
			.user(this.user)
			.environment(this.environment);
	}
}
