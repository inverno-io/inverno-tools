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

import io.inverno.tool.buildtools.ArchiveTask;
import io.inverno.tool.buildtools.BuildRuntimeTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import io.inverno.tool.buildtools.PackageApplicationTask;

/**
 * <p>
 * Generic {@link BuildRuntimeTask} implementation.
 * </p>
 * 
 * <p>
 * This implementation relies on JDK's {@code jlink} tool to generate the runtime image from the project module JMOD and previously modularized project dependencies. 
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericBuildRuntimeTask extends AbstractTask<Path, BuildRuntimeTask> implements BuildRuntimeTask {

	private static final Logger LOGGER = LogManager.getLogger(GenericBuildRuntimeTask.class);
	
	private static final PrintStream OUT = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.INFO)
			.setAutoFlush(true)
			.buildPrintStream();
	
	private static final PrintStream ERR = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.ERROR)
			.setAutoFlush(true)
			.buildPrintStream();
	
	private Optional<String> addModules = Optional.empty();
	private Optional<String> addOptions = Optional.empty();
	private Optional<String> compress = Optional.empty();
	private boolean bindServices;
	private boolean ignoreSigningInformation;
	private boolean stripDebug = true;
	private boolean stripNativeCommands = true;
	private Optional<String> vm = Optional.empty();
	private List<? extends Launcher> launchers = List.of();
	private boolean addUnnamedModules = true;
	
	/**
	 * <p>
	 * Creates a generic build runtime task.
	 * </p>
	 * 
	 * @param parentTask the parent task
	 */
	public GenericBuildRuntimeTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}

	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		return "Project runtime image created";
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		return 350;
	}

	@Override
	public BuildRuntimeTask addModules(String addModules) {
		this.addModules = Optional.ofNullable(addModules);
		return this;
	}
	
	@Override
	public BuildRuntimeTask addOptions(String addOptions) {
		this.addOptions = Optional.ofNullable(addOptions);
		return this;
	}

	@Override
	public BuildRuntimeTask compress(String compress) {
		this.compress = Optional.ofNullable(compress);
		return this;
	}

	@Override
	public BuildRuntimeTask bindServices(boolean bindServices) {
		this.bindServices = bindServices;
		return this;
	}

	@Override
	public BuildRuntimeTask ignoreSigningInformation(boolean ignoreSigningInformation) {
		this.ignoreSigningInformation = ignoreSigningInformation;
		return this;
	}

	@Override
	public BuildRuntimeTask stripDebug(boolean stripDebug) {
		this.stripDebug = stripDebug;
		return this;
	}

	@Override
	public BuildRuntimeTask stripNativeCommands(boolean stripNativeCommands) {
		this.stripNativeCommands = stripNativeCommands;
		return this;
	}

	@Override
	public BuildRuntimeTask vm(String vm) {
		this.vm = Optional.ofNullable(vm);
		return this;
	}
	
	@Override
	public BuildRuntimeTask addUnnamedModules(boolean addUnnamedModules) {
		this.addUnnamedModules = addUnnamedModules;
		return this;
	}

	@Override
	public BuildRuntimeTask launchers(List<? extends Launcher> launchers) {
		this.launchers = launchers != null ? launchers : List.of();
		return this;
	}
	
	@Override
	protected Path doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Creating project runtime...");
		}

		Path runtimeImagePath = project.getImagePath(ImageType.RUNTIME);
		if(project.isMarked() || project.getDependencies().stream().anyMatch(dependency -> dependency.isMarked()) || !Files.exists(runtimeImagePath)) {
			LOGGER.info("[ Creating project runtime: {}... ]", runtimeImagePath);
			if(Files.exists(runtimeImagePath)) {
				try (Stream<Path> walk = Files.walk(runtimeImagePath)) {
					for(Iterator<Path> pathIterator = walk.sorted(Comparator.reverseOrder()).iterator(); pathIterator.hasNext();) {
						Files.delete(pathIterator.next());
					}
				}
				catch (IOException e) {
					throw new TaskExecutionException("Error cleaning project runtime", e);
				}
			}
			
			List<String> jlink_args = new LinkedList<>();

			jlink_args.add("--verbose");
			this.launchers.stream()
				.map(launcher -> {
					StringBuilder launcherString = new StringBuilder();
					launcherString.append(launcher.getName()).append("=").append(launcher.getModule().orElse(project.getModuleName()));
					launcher.getMainClass().ifPresent(mainClass -> launcherString.append("/").append(mainClass));
					return launcherString.toString();
				})
				.forEach(moduleMain -> {
					jlink_args.add("--launcher");
					jlink_args.add(moduleMain);
				});
			
			this.compress.ifPresent(value -> {
				jlink_args.add("--compress=" + value);
			});
			if(this.bindServices) {
				jlink_args.add("--bind-services");
			}
			if(this.ignoreSigningInformation) {
				jlink_args.add("--ignore-signing-information");
			}
			
			if(this.stripDebug) {
				jlink_args.add("--strip-debug");
			}
			if(this.stripNativeCommands && this.launchers.isEmpty()) {
				jlink_args.add("--strip-native-commands");
			}
			this.vm.ifPresent(value -> {
				jlink_args.add("--vm");
				jlink_args.add(value);
			});
			
			String javaModulePath = Path.of(System.getProperty("java.home"), "jmods").toAbsolutePath().toString();
			String dependenciesModulePath = project.getDependencies().stream().map(dependencyModule -> dependencyModule.getModulePath().toString()).collect(Collectors.joining(System.getProperty("path.separator")));
			String projectJmodPath = project.getModulePath().toString();
			
			String jlink_modulePath = String.join(System.getProperty("path.separator"), javaModulePath, dependenciesModulePath, projectJmodPath);
			
			jlink_args.add("--module-path");
			jlink_args.add(jlink_modulePath);
			
			Set<String> modules = new HashSet<>();
			this.addModules.ifPresent(value -> {
				for(String addModule : value.split(",")) {
					modules.add(StringUtils.strip(addModule));				
				}
			});
			project.getDependencies().stream()
				.forEach(d -> modules.add(d.getModuleName()));
			// We must add current project as well...
			modules.add(project.getModuleName());
			
			jlink_args.add("--add-modules");
			jlink_args.add(modules.stream().collect(Collectors.joining(",")));
			
			jlink_args.add("--limit-modules");
			jlink_args.add(modules.stream().collect(Collectors.joining(",")));
			
			jlink_args.add("--output");
			jlink_args.add(runtimeImagePath.toString());
			
			List<String> options = new ArrayList<>();
			this.addOptions.ifPresent(options::add);
			if(this.addUnnamedModules) {
				options.add(" --add-modules=" + project.getDependencies().stream()
					.filter(d -> !d.isNamed())
					.map(d -> d.getModuleName())
					.collect(Collectors.joining(","))
				);
			}
			if(!options.isEmpty()) {
				jlink_args.add("--add-options");
				jlink_args.add(options.stream().collect(Collectors.joining(" ")));
			}
			
			LOGGER.info(" - jlink {}", jlink_args.stream().collect(Collectors.joining(" ")));
			
			if(JavaTools.JLINK.run(OUT, ERR, jlink_args.stream().toArray(String[]::new)) != 0) {
				throw new TaskExecutionException("Error creating project runtime");
			}
		}
		else {
			LOGGER.info("[ Project runtime is up to date ]");
		}
		return runtimeImagePath;
	}

	@Override
	public ArchiveTask archive() {
		return new GenericArchiveTask(this, ImageType.RUNTIME);
	}

	@Override
	public PackageApplicationTask packageApplication() {
		return new GenericPackageApplicationTask(this);
	}
	
	/**
	 * <p>
	 * Generic {@link BuildRuntimeTask.Launcher} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class GenericLauncher implements BuildRuntimeTask.Launcher {

		private final String name;
		private final Optional<String> module;
		private final Optional<String> mainClass;
		
		/**
		 * <p>
		 * Creates a generic runtime launcher.
		 * </p>
		 * 
		 * @param name
		 * @param module
		 * @param mainClass 
		 */
		public GenericLauncher(String name, String module, String mainClass) {
			super();
			this.name = name;
			this.module = Optional.ofNullable(module);
			this.mainClass = Optional.ofNullable(mainClass);
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Optional<String> getModule() {
			return this.module;
		}

		@Override
		public Optional<String> getMainClass() {
			return this.mainClass;
		}
	}
}
