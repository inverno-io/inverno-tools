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

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
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
import io.inverno.tool.buildtools.ContainerizeTask;
import io.inverno.tool.buildtools.TaskExecutionException;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Generic {@link ContainerizeTask} implementation.
 * </p>
 * 
 * <p>
 * This implementation relies on <a href="https://github.com/GoogleContainerTools/jib">Jib core</a>.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericContainerizeTask extends AbstractTask<ContainerizeTask.ContainerImage, ContainerizeTask> implements ContainerizeTask {
	
	private static final Logger LOGGER = LogManager.getLogger(GenericContainerizeTask.class);

	private static final Target DEFAULT_TARGET = Target.TAR;
	private static final ImageFormat DEFAULT_IMAGE_FORMAT = ImageFormat.OCI;
	private static final String DEFAULT_FROM = "alpine:latest";
	private static final Port DEFAULT_PORT = Port.tcp(8080);
	
	private Optional<Target> target = Optional.empty();
	private Optional<String> from = Optional.empty();
	private Optional<String> executable = Optional.empty();
	private Optional<ImageFormat> imageFormat = Optional.empty();
	private Optional<String> repository = Optional.empty();
	private Optional<String> registry = Optional.empty();
	private Optional<String> registryUsername = Optional.empty();
	private Optional<String> registryPassword = Optional.empty();
	private Optional<Path> dockerExecutable = Optional.empty();
	private Optional<Map<String, String>> dockerEnvironment = Optional.empty();
	private Optional<Map<String, String>> labels = Optional.empty();
	private Optional<Set<String>> ports = Optional.empty();
	private Optional<Set<String>> volumes = Optional.empty();
	private Optional<String> user = Optional.empty();
	private Optional<Map<String, String>> environment = Optional.empty();
	
	/**
	 * <p>
	 * Creates a generic containerize task.
	 * </p>
	 * 
	 * @param parentTask the parent task
	 */
	public GenericContainerizeTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}

	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		switch(this.target.orElse(DEFAULT_TARGET)) {
			case DOCKER: return "Project " + this.imageFormat.orElse(DEFAULT_IMAGE_FORMAT) + " container image deployed to Docker daemon";
			case REGISTRY: return "Project " + this.imageFormat.orElse(DEFAULT_IMAGE_FORMAT) + " container image published to " + this.registry.orElse("");
			case TAR: return "Project " + this.imageFormat.orElse(DEFAULT_IMAGE_FORMAT) + " container image TAR archive created";
			default: return "Project " + this.imageFormat.orElse(DEFAULT_IMAGE_FORMAT) + " container image created";
		}
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		// TODO see how much does each of these takes (this will be difficult for registry but it is most likely to take a bit longer than docker)
		switch(this.target.orElse(DEFAULT_TARGET)) {
			case DOCKER: 
			case REGISTRY: 
			case TAR: 
			default: return 550;
		}
	}

	@Override
	public ContainerizeTask target(Target target) {
		this.target = Optional.ofNullable(target);
		return this;
	}

	@Override
	public ContainerizeTask from(String from) {
		this.from = Optional.ofNullable(from);
		return this;
	}

	@Override
	public ContainerizeTask executable(String executable) {
		this.executable = Optional.ofNullable(executable);
		return this;
	}
	
	@Override
	public ContainerizeTask format(Format format) {
		this.imageFormat = Optional.ofNullable(format).map(Format::name).map(ImageFormat::valueOf);
		return this;
	}

	@Override
	public ContainerizeTask repository(String repository) {
		this.repository = Optional.ofNullable(repository);
		return this;
	}

	@Override
	public ContainerizeTask registry(String registry) {
		this.registry = Optional.ofNullable(registry);
		return this;
	}

	@Override
	public ContainerizeTask registryUsername(String registryUsername) {
		this.registryUsername = Optional.ofNullable(registryUsername);
		return this;
	}

	@Override
	public ContainerizeTask registryPassword(String registryPassword) {
		this.registryPassword = Optional.ofNullable(registryPassword);
		return this;
	}

	@Override
	public ContainerizeTask dockerExecutable(Path dockerExecutable) {
		this.dockerExecutable = Optional.ofNullable(dockerExecutable);
		return this;
	}

	@Override
	public ContainerizeTask dockerEnvironment(Map<String, String> dockerEnvironment) {
		this.dockerEnvironment = Optional.ofNullable(dockerEnvironment);
		return this;
	}

	@Override
	public ContainerizeTask labels(Map<String, String> labels) {
		this.labels = Optional.ofNullable(labels);
		return this;
	}

	@Override
	public ContainerizeTask ports(Set<String> ports) {
		this.ports = Optional.ofNullable(ports);
		return this;
	}

	@Override
	public ContainerizeTask volumes(Set<String> volumes) {
		this.volumes = Optional.ofNullable(volumes);
		return this;
	}

	@Override
	public ContainerizeTask user(String user) {
		this.user = Optional.ofNullable(user);
		return this;
	}

	@Override
	public ContainerizeTask environment(Map<String, String> environment) {
		this.environment = Optional.ofNullable(environment);
		return this;
	}
	
	@Override
	protected ContainerizeTask.ContainerImage doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Creating project container image...");
		}
		Target resolvedTarget = this.target.orElse(DEFAULT_TARGET);
		Path applicationImagePath = project.getImagePath(ImageType.APPLICATION);
		Path containerImagePath = project.getImagePath(ImageType.CONTAINER);
		if(project.isMarked() || 
			project.getDependencies().stream().anyMatch(dependency -> dependency.isMarked()) || 
			resolvedTarget == Target.DOCKER || resolvedTarget == Target.REGISTRY || !Files.exists(containerImagePath)) {
			switch(resolvedTarget) {
				case TAR: LOGGER.info("[ Creating project container image {}... ]", containerImagePath);
					break;
				case DOCKER: LOGGER.info("[ Creating project container image to Docker daemon... ]");
					break;
				case REGISTRY: LOGGER.info("[ Publishing project container image to {}... ]", this.registry.get());
					break;
				default: LOGGER.info("[ Creating project container image... ]");
					break;
			}
			try {
				FileEntriesLayer layer = FileEntriesLayer.builder().addEntryRecursive(
						applicationImagePath, 
						AbsoluteUnixPath.get("/opt/" + project.getName()),
						this.getFilePermissionsProvider(),
						this.getModificationTimeProvider(),
						this.getOwnershipProvider()
					)
					.build();
				
				JibContainerBuilder builder = Jib.from(this.from.orElse(DEFAULT_FROM))
					.setCreationTime(Instant.now())
					.setFormat(this.imageFormat.orElse(DEFAULT_IMAGE_FORMAT))
					.addFileEntriesLayer(layer)
					.setWorkingDirectory(AbsoluteUnixPath.get("/opt/" + project.getName()))
					.setEntrypoint("/opt/" + project.getName() + "/" + (Platform.getSystemPlatform() == Platform.WINDOWS ? this.executable.orElse(project.getName() + ".exe") : "bin/" + this.executable.orElse(project.getName())))
					.addExposedPort(DEFAULT_PORT);

				this.labels.ifPresent(builder::setLabels);
				this.ports.ifPresent(values -> {
					builder.setExposedPorts(values.stream().map(this::parsePort).toArray(Port[]::new));
				});
				this.volumes.ifPresent(values -> {
					builder.setVolumes(values.stream().map(AbsoluteUnixPath::get).toArray(AbsoluteUnixPath[]::new));
				});
				this.user.ifPresent(builder::setUser);
				this.environment.ifPresent(builder::setEnvironment);
				
				builder.setPlatforms(Set.of(new com.google.cloud.tools.jib.api.buildplan.Platform(Platform.getSystemPlatform().getArch(), Platform.getSystemPlatform().getOs())));
				
				Containerizer containerizer = this.getContainerizer(project);
				
				JibContainer container = builder.containerize(containerizer);
				
				return new GenericContainerizeTask.ContainerImage(project, container.getTargetImage());
			} 
			catch (InterruptedException | RegistryException | IOException | CacheDirectoryCreationException	| ExecutionException | InvalidImageReferenceException e) {
				throw new TaskExecutionException("Error creating project container image", e);
			}
		}
		else {
			LOGGER.info("[ Project container image is up to date ]");
			return null;
		}
	}
	
	/**
	 * <p>
	 * Returns a new containerizer for the specified project.
	 * </p>
	 * 
	 * @param project the build project
	 * 
	 * @return a new containerizer
	 * 
	 * @throws InvalidImageReferenceException if the image reference deduced from the build project is invalid.
	 */
	private Containerizer getContainerizer(BuildProject project) throws InvalidImageReferenceException {
		// <registry>/<repository>:<tag>
		// TODO Check that this is ok
		String imageReference = registry.orElse("") + this.repository.map(value -> value += "/").orElse("") + project.getName() + ":" + project.getModuleVersion();
		
		Containerizer containerizer;
		switch(this.target.orElse(DEFAULT_TARGET)) {
			case TAR: {
				containerizer = Containerizer.to(TarImage.at(project.getImagePath(ImageType.CONTAINER)).named(imageReference));
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
		
		containerizer.addEventHandler(LogEvent.class, event -> {
			switch(event.getLevel()) {
				case DEBUG: LOGGER.debug(event.getMessage());
					break;
				case ERROR: LOGGER.error(event.getMessage());
					break;
				case INFO: LOGGER.info(event.getMessage());
					break;
				case WARN: LOGGER.warn(event.getMessage());
					break;
				default: 
					break;
			}
		});
		
		return containerizer;
	}
	
	/**
	 * <p>
	 * Returns the port instance corresponding to the port parameter (e.g. {@code 1234/tcp}).
	 * </p>
	 * 
	 * @param port the port expression to parse
	 * 
	 * @return a port instance
	 * 
	 * @throws IllegalArgumentException if the specified parameter is an invalid port expression
	 */
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
			throw new IllegalArgumentException("Invalid port: " + port + ", must be of the form: port_number [ \"/\" udp/tcp ]");
		}
	}

	/**
	 * <p>
	 * Returns a file permission provider.
	 * </p>
	 * 
	 * @return a new file permission provider
	 */
	protected FilePermissionsProvider getFilePermissionsProvider() {
		FilePermissionsProvider filePermissionsProvider = (sourcePath, destinationPath) -> {
			try {
				return FilePermissions.fromPosixFilePermissions(Files.getPosixFilePermissions(sourcePath));
			} 
			catch (IOException | UnsupportedOperationException e) {
				LOGGER.warn("Unable to determine File permissions, falling back to default", e);
				return Files.isDirectory(sourcePath) ? FilePermissions.DEFAULT_FOLDER_PERMISSIONS : FilePermissions.DEFAULT_FILE_PERMISSIONS;
			}
		};
		return filePermissionsProvider;
	}
	
	/**
	 * <p>
	 * Returns a modification time provider.
	 * </p>
	 * 
	 * @return a new modification time provider
	 */
	protected ModificationTimeProvider getModificationTimeProvider() {
		final Instant modificationTime = Instant.now();
		ModificationTimeProvider modificationTimeProvider = (sourcePath, destinationPath) -> {
			return modificationTime;
		};
		return modificationTimeProvider;
	}
	
	/**
	 * <p>
	 * Returns an ownership provider.
	 * </p>
	 * 
	 * @return a new ownership provider
	 */
	protected OwnershipProvider getOwnershipProvider() {
		OwnershipProvider ownershipProvider = (sourcePath, destinationPath) -> "";
		return ownershipProvider;
	}
	
	/**
	 * <p>
	 * Generic {@link ContainerizeTask.ContainerImageRef} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	private static class ContainerImage implements ContainerizeTask.ContainerImage {

		private final ImageReference imageReference;
		
		private final Optional<Path> imageArchivePath;

		/**
		 * <p>
		 * Creates a generic container image.
		 * </p>
		 * 
		 * @param project        the build project
		 * @param imageReference the underlying image reference
		 */
		public ContainerImage(BuildProject project, ImageReference imageReference) {
			this.imageReference = imageReference;
			this.imageArchivePath = Optional.of(project.getImagePath(ImageType.CONTAINER)).filter(Files::exists);
		}
		
		@Override
		public String getRegistry() {
			return this.imageReference.getRegistry();
		}

		@Override
		public String getRepository() {
			return this.imageReference.getRepository();
		}

		@Override
		public Optional<String> getTag() {
			return this.imageReference.getTag();
		}
		
		@Override
		public Optional<String> getDigest() {
			return this.imageReference.getDigest();
		}

		@Override
		public String getCanonicalName() {
			return this.imageReference.toString();
		}

		@Override
		public Optional<Path> getPath() {
			return this.imageArchivePath;
		}

		@Override
		public String toString() {
			return this.imageReference.toString();
		}

		@Override
		public String getClassifier() {
			return ImageType.CONTAINER.getNativeClassifier();
		}

		@Override
		public Optional<String> getFormat() {
			return this.imageArchivePath.map(ign -> "tar");
		}
	}
}
