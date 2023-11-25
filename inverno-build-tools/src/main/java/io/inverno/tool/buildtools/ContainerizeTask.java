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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * A task for creating container images of the project application.
 * </p>
 * 
 * <p>
 * This task supports the creation of Docker or OCI images packaged as portable TAR archives or directly deployed to a local Docker daemon or to a remote registry.
 * </p>
 * 
 * <p>
 * This task depends on {@link PackageApplicationTask}.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface ContainerizeTask extends Task<ContainerizeTask.ContainerImageRef, ContainerizeTask> {

	/**
	 * <p>
	 * Describes where to generate the container image.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	enum Target {
		/**
		 * Generates a portable TAR archive.
		 */
		TAR,
		/**
		 * Installs the image to the local Docker daemon.
		 */
		DOCKER,
		/**
		 * Deploys the image to a remote image registry.
		 */
		REGISTRY
	}
	
	/**
	 * <p>
	 * Describes the format of the image to generate.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	enum Format {
		/**
		 * <p>
		 * Generates a Docker image.
		 * </p>
		 * 
		 * <p>
		 * See <a href="https://docs.docker.com/registry/spec/manifest-v2-2/">Docker V2.2</a>. 
		 * </p>
		 */
		Docker,
		/** 
		 * <p>
		 * Generates an OCI image.
		 * </p>
		 * 
		 * <p>
		 * See <a href="https://github.com/opencontainers/image-spec/blob/master/manifest.md">OCI</a>. 
		 * </p>
		 */
		OCI
	}
	
	/**
	 * <p>
	 * Represents the generated container image.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface ContainerImageRef {
		
		/**
		 * <p>
		 * Returns the image registry.
		 * </p>
		 * 
		 * @return the image registry 
		 */
		String getRegistry();
		
		/**
		 * <p>
		 * Returns the image repository.
		 * </p>
		 * 
		 * @return the imae repository
		 */
		String getRepository();
		
		/**
		 * <p>
		 * Returns the image tag.
		 * </p>
		 * 
		 * @return an optional returning the image tag or an empty optional
		 */
		Optional<String> getTag();

		/**
		 * <p>
		 * Returns the image digest.
		 * </p>
		 * 
		 * @return an optional returning the image digest or an empty optional
		 */
		Optional<String> getDigest();
		
		/**
		 * <p>
		 * Returns the canonical name of the image (e.g. {@code registry/repository:tag})
		 * </p>
		 * 
		 * @return the canonical name of the image
		 */
		String getCanonicalName();
		
		/**
		 * <p>
		 * Returns the path to the generated TAR archive.
		 * </p>
		 * 
		 * @return an optional returning the path to the TAR archive or an empty optional
		 */
		Optional<Path> getArchivePath();
	}
	
	/**
	 * <p>
	 * Specifies where to generate the image.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link Target#TAR}.
	 * </p>
	 * 
	 * @param target the image target
	 * 
	 * @return the task
	 */
	ContainerizeTask target(Target target);

	/**
	 * <p>
	 * Specifies the base container image.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code debian:stable-slim}.
	 * </p>
	 * 
	 * @param from the base container image
	 * 
	 * @return the task
	 */
	ContainerizeTask from(String from);

	/**
	 * <p>
	 * Specifies the image executable.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link Project#getName()}.
	 * </p>
	 * 
	 * @param executable the image executable
	 * 
	 * @return the task
	 */
	ContainerizeTask executable(String executable);
	
	/**
	 * <p>
	 * Specifies the image format.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link Format#OCI}.
	 * </p>
	 * 
	 * @param format the image format.
	 * 
	 * @return the task
	 */
	ContainerizeTask format(Format format);

	/**
	 * <p>
	 * Specifies the image repository.
	 * </p>
	 * 
	 * @param repository the image repository.
	 * 
	 * @return the task
	 */
	ContainerizeTask repository(String repository);
	
	/**
	 * <p>
	 * Specifies the image registry where to deploy the image.
	 * </p>
	 * 
	 * @param registry the image registry.
	 * 
	 * @return the task
	 */
	ContainerizeTask registry(String registry);

	/**
	 * <p>
	 * Specifies the username to use to connect to the image registry.
	 * </p>
	 * 
	 * @param registryUsername the registry username
	 * 
	 * @return the task
	 */
	ContainerizeTask registryUsername(String registryUsername);

	/**
	 * <p>
	 * Specifies the password to use to connect to the image registry.
	 * </p>
	 * 
	 * @param registryPassword the registry password
	 * 
	 * @return the task
	 */
	ContainerizeTask registryPassword(String registryPassword);
	
	/**
	 * <p>
	 * Specifies the path to the Docker CLI executable used to install the image in the Docker daemon.
	 * </p>
	 * 
	 * @param dockerExecutable the path to the docker executable
	 * 
	 * @return the task
	 */
	ContainerizeTask dockerExecutable(Path dockerExecutable);
	
	/**
	 * <p>
	 * Specifies the Docker environment variables used when executing the Docker CLI executable.
	 * </p>
	 * 
	 * @param dockerEnvironment the docker environment variables
	 * 
	 * @return the task
	 */
	ContainerizeTask dockerEnvironment(Map<String, String> dockerEnvironment);

	/**
	 * <p>
	 * Specifies the labels to apply to the image.
	 * </p>
	 * 
	 * @param labels the image labels
	 * 
	 * @return the task
	 */
	ContainerizeTask labels(Map<String, String> labels);

	/**
	 * <p>
	 * Specifies the ports exposed by the container at runtime defined as: {@code port_number [ "/" udp/tcp ] }.
	 * </p>
	 * 
	 * @param ports the ports to expose
	 * 
	 * @return the task
	 */
	ContainerizeTask ports(Set<String> ports);

	/**
	 * <p>
	 * Specifies the container's mount points.
	 * </p>
	 * 
	 * @param volumes the image volumes
	 * 
	 * @return the task
	 */
	ContainerizeTask volumes(Set<String> volumes);

	/**
	 * <p>
	 * Specifies the user and group used to run the container defined as: {@code user / uid [ ":" group / gid ]}.
	 * </p>
	 * 
	 * @param user the user and group used to run the container
	 * 
	 * @return the task
	 */
	ContainerizeTask user(String user);

	/**
	 * <p>
	 * Specifies the container's environment variables.
	 * </p>
	 * 
	 * @param environment the container's environment variables
	 * 
	 * @return the task
	 */
	ContainerizeTask environment(Map<String, String> environment);
}
