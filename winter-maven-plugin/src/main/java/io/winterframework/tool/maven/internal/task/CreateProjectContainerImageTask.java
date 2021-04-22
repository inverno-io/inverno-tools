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
package io.winterframework.tool.maven.internal.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.maven.plugin.AbstractMojo;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.FilePermissionsProvider;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.api.buildplan.ModificationTimeProvider;
import com.google.cloud.tools.jib.api.buildplan.OwnershipProvider;
import com.google.cloud.tools.jib.api.buildplan.Port;

import io.winterframework.tool.maven.internal.Platform;
import io.winterframework.tool.maven.internal.ProjectModule;
import io.winterframework.tool.maven.internal.Task;
import io.winterframework.tool.maven.internal.TaskExecutionException;
import io.winterframework.tool.maven.internal.ProgressBar.Step;

/**
 * <p>
 * Creates the project module container image.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class CreateProjectContainerImageTask extends Task<Void> {

	public static enum Target {
		TAR,
		DOCKER,
		REGISTRY
	}
	
	private final ProjectModule projectModule;
	
	private FilePermissionsProvider filePermissionsProvider;
	private ModificationTimeProvider modificationTimeProvider;
	private OwnershipProvider ownershipProvider;
	
	private Target target;
	private String from;
	private String name;
	
	private Optional<String> registry;
	private Optional<String> repository;
	
	private Optional<ImageFormat> imageFormat;
	
	private Optional<String> registryUsername;
	private Optional<String> registryPassword;
	
	private Optional<Path> dockerExecutable;
	private Optional<Map<String, String>> dockerEnvironment;

	private Optional<Map<String, String>> labels;
	private Optional<Set<String>> ports;
	private Optional<Set<String>> volumes;
	private Optional<String> user;
	private Optional<Map<String, String>> environment;
	
	public CreateProjectContainerImageTask(AbstractMojo mojo, ProjectModule projectModule) {
		super(mojo);
		this.projectModule = projectModule;
	}
	
	@Override
	public void setStep(Step step) {
		if(step != null) {
			step.setDescription("Creating project container image...");
		}
		super.setStep(step);
	}

	@Override
	protected Void execute() throws TaskExecutionException {
		if(this.projectModule.isMarked() || 
			this.projectModule.getModuleDependencies().stream().anyMatch(dependency -> dependency.isMarked()) || 
			(this.target == Target.TAR && !Files.exists(this.projectModule.getContainerImageTarPath()))) {
			if(this.verbose) {
				switch(this.target) {
					case TAR: this.getLog().info("[ Creating project container image: " + this.projectModule.getContainerImageTarPath() + "... ]");
						break;
					case DOCKER: this.getLog().info("[ Creating project container image to Docker daemon... ]");
						break;
					default: this.getLog().info("[ Creating project container image... ]");
						break;
				}
			}
			try {
				FileEntriesLayer layer = FileEntriesLayer.builder().addEntryRecursive(
						this.projectModule.getApplicationImagePath(), 
						AbsoluteUnixPath.get("/" + this.name), 
						this.getFilePermissionsProvider(),
						this.getModificationTimeProvider(),
						this.getOwnershipProvider()
					)
					.build();
				
				JibContainerBuilder builder = Jib.from(this.from)
					.setCreationTime(Instant.now())
					.setFormat(this.imageFormat.orElse(ImageFormat.OCI))
					.addFileEntriesLayer(layer)
					.setEntrypoint("/" + this.name + "/bin/" + this.name)
					.addExposedPort(Port.tcp(8080));

				
				this.labels.ifPresent(labels -> {
					builder.setLabels(labels);
				});
				this.ports.ifPresent(ports -> {
					builder.setExposedPorts(ports.stream().map(this::parsePort).toArray(Port[]::new));
				});
				this.volumes.ifPresent(volumes -> {
					builder.setVolumes(volumes.stream().map(AbsoluteUnixPath::get).toArray(AbsoluteUnixPath[]::new));
				});
				
				this.user.ifPresent(user -> {
					builder.setUser(user);
				});
				this.environment.ifPresent(environment -> {
					builder.setEnvironment(environment);
				});
				
				builder.setPlatforms(Set.of(new com.google.cloud.tools.jib.api.buildplan.Platform(Platform.getSystemPlatform().getArch(), Platform.getSystemPlatform().getOs())));
				
				Containerizer containerizer = this.getContainerizer();
				
				builder.containerize(containerizer);
			} 
			catch (InterruptedException | RegistryException | IOException | CacheDirectoryCreationException
					| ExecutionException | InvalidImageReferenceException e) {
				e.printStackTrace();
			}
		}
		else {
			if(this.verbose) {
				this.getLog().info("[ Project container image is up to date ]");
			}
		}
		return null;
	}
	
	private Port parsePort(String port) throws IllegalArgumentException {
		try (Scanner portScanner = new Scanner(port).useDelimiter("/")) {
			int portNumber = portScanner.nextInt();
			String protocol;
			if(portScanner.hasNext()) {
				protocol = portScanner.next("udp|tcp");
			}
			else {
				protocol = "tcp";
			}
			
			if(portNumber < 0 || portNumber > 0xffff) {
				throw new IllegalArgumentException("Invalid port number: " + portNumber);
			}
			
			return Port.parseProtocol(portNumber, protocol);
		}
		catch(NoSuchElementException e) {
			throw new IllegalArgumentException("Invalid port: " + port + ", must be <portNumber>/<portocol>");
		}
	}
	
	protected Containerizer getContainerizer() throws InvalidImageReferenceException, TaskExecutionException {
		// <registry>/<repository>:<tag>
		String imageReference = registry.orElse("") + this.repository.map(value -> value += "/").orElse("") + this.name + ":" + this.projectModule.getModuleVersion();
		
		Containerizer containerizer;
		switch(this.target) {
			case TAR: {
				containerizer = Containerizer.to(TarImage.at(this.projectModule.getContainerImageTarPath()).named(imageReference));
				break;
			}
			case DOCKER: {
				DockerDaemonImage dockerDaemonImage = DockerDaemonImage.named(imageReference);
				this.dockerExecutable.ifPresent(dockerDaemonImage::setDockerExecutable);
				this.dockerEnvironment.ifPresent(dockerDaemonImage::setDockerEnvironment);
				containerizer = Containerizer.to(dockerDaemonImage);
				break;
			}
			case REGISTRY: {
				// TODO what if username and password are null? It it a valid use case (no authent?)
				containerizer = Containerizer.to(
					RegistryImage.named(imageReference).addCredential(
						this.registryUsername.orElse(null),
						this.registryPassword.orElse(null)
					)
				);
				break;
			}
			default:
				throw new IllegalStateException("Unsupported target: " + this.target);
		}
		
		if(this.verbose) {
			containerizer.addEventHandler(LogEvent.class, event -> {
				switch(event.getLevel()) {
					case DEBUG: this.getLog().debug(event.getMessage());
						break;
					case ERROR: this.getLog().error(event.getMessage());
						break;
					case INFO: this.getLog().info(event.getMessage());
						break;
					case WARN: this.getLog().warn(event.getMessage());
						break;
					default: 
						break;
				}
			});
		}
		
		return containerizer;
	}

	protected FilePermissionsProvider getFilePermissionsProvider() {
		if(this.filePermissionsProvider == null) {
			this.filePermissionsProvider = (sourcePath, destinationPath) -> {
				try {
					return FilePermissions.fromPosixFilePermissions(Files.getPosixFilePermissions(sourcePath));
				} 
				catch (IOException e) {
					return Files.isDirectory(sourcePath) ? FilePermissions.DEFAULT_FOLDER_PERMISSIONS : FilePermissions.DEFAULT_FILE_PERMISSIONS;
				}
			};
		}
		return this.filePermissionsProvider;
	}
	
	protected ModificationTimeProvider getModificationTimeProvider() {
		if(this.modificationTimeProvider == null) {
			final Instant modificationTime = Instant.now();
			this.modificationTimeProvider = (sourcePath, destinationPath) -> {
				return modificationTime;
			};
		}
		return this.modificationTimeProvider;
	}
	
	protected OwnershipProvider getOwnershipProvider() {
		if(this.ownershipProvider == null) {
			this.ownershipProvider = (sourcePath, destinationPath) -> "";
		}
		return this.ownershipProvider;
	}
	
	public Target getTarget() {
		return target;
	}
	
	public void setTarget(Target target) {
		this.target = target;
	}
	
	public String getFrom() {
		return from;
	}
	
	public void setFrom(String from) {
		this.from = from;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Optional<String> getRegistry() {
		return registry;
	}

	public void setRegistry(Optional<String> registry) {
		this.registry = registry;
	}

	public Optional<String> getRepository() {
		return repository;
	}

	public void setRepository(Optional<String> repository) {
		this.repository = repository;
	}

	public Optional<ImageFormat> getImageFormat() {
		return imageFormat;
	}

	public void setImageFormat(Optional<ImageFormat> imageFormat) {
		this.imageFormat = imageFormat;
	}

	public Optional<Map<String, String>> getLabels() {
		return labels;
	}

	public void setLabels(Optional<Map<String, String>> labels) {
		this.labels = labels;
	}

	public Optional<Set<String>> getPorts() {
		return ports;
	}

	public void setPorts(Optional<Set<String>> ports) {
		this.ports = ports;
	}

	public Optional<Set<String>> getVolumes() {
		return volumes;
	}

	public void setVolumes(Optional<Set<String>> volumes) {
		this.volumes = volumes;
	}

	public Optional<String> getUser() {
		return user;
	}

	public void setUser(Optional<String> user) {
		this.user = user;
	}

	public Optional<Map<String, String>> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Optional<Map<String, String>> environment) {
		this.environment = environment;
	}

	public Optional<String> getUsername() {
		return registryUsername;
	}

	public void setRegistryUsername(Optional<String> username) {
		this.registryUsername = username;
	}

	public Optional<String> getPassword() {
		return registryPassword;
	}

	public void setRegistryPassword(Optional<String> password) {
		this.registryPassword = password;
	}

	public Optional<Path> getDockerExecutable() {
		return dockerExecutable;
	}

	public void setDockerExecutable(Optional<Path> dockerExecutable) {
		this.dockerExecutable = dockerExecutable;
	}

	public Optional<Map<String, String>> getDockerEnvironment() {
		return dockerEnvironment;
	}

	public void setDockerEnvironment(Optional<Map<String, String>> dockerEnvironment) {
		this.dockerEnvironment = dockerEnvironment;
	}
	
}
