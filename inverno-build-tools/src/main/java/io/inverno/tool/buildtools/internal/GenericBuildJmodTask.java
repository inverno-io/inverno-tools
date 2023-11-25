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

import io.inverno.tool.buildtools.BuildJmodTask;
import io.inverno.tool.buildtools.BuildRuntimeTask;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

/**
 * <p>
 * Generic {@link BuildJmodTask} implementation.
 * </p>
 * 
 * <p>
 * This implementation relies on JDK's {@code jmod} tool to generate the JMOD archive from the project module and previously modularized project dependencyies.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericBuildJmodTask extends AbstractTask<Path, BuildJmodTask> implements BuildJmodTask {
	
	private static final Logger LOGGER = LogManager.getLogger(GenericBuildJmodTask.class);
	
	private static final PrintStream OUT = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.INFO)
			.setAutoFlush(true)
			.buildPrintStream();
	
	private static final PrintStream ERR = IoBuilder.forLogger(LOGGER)
			.setLevel(Level.ERROR)
			.setAutoFlush(true)
			.buildPrintStream();

	private String mainClass;
	private boolean resolveMainClass;
	private Path configurationPath;
	private Path legalPath;
	private Path manPath;
	
	/**
	 * <p>
	 * Creates a generic build JMOD task.
	 * </p>
	 * 
	 * @param parentTask the parent task
	 */
	public GenericBuildJmodTask(AbstractTask<?, ?> parentTask) {
		super(parentTask);
	}

	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		return "Project jmod created";
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		return 50;
	}
	
	@Override
	public BuildJmodTask mainClass(String mainClass) {
		this.mainClass= mainClass;
		return this;
	}

	@Override
	public BuildJmodTask resolveMainClass(boolean resolveMainClass) {
		this.resolveMainClass = resolveMainClass;
		return this;
	}

	@Override
	public BuildJmodTask configurationPath(Path configurationPath) {
		this.configurationPath = configurationPath;
		return this;
	}

	@Override
	public BuildJmodTask legalPath(Path legalPath) {
		this.legalPath = legalPath;
		return this;
	}

	@Override
	public BuildJmodTask manPath(Path manPath) {
		this.manPath = manPath;
		return this;
	}

	@Override
	protected Path doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Creating project jmod...");
		}
		if(project.isMarked()) {
			LOGGER.info("[ Creating project jmod {}... ]", project.getModulePath());
			try {
				Files.deleteIfExists(project.getModulePath());
				
				List<String> jmod_args = new LinkedList<>();
				
				jmod_args.add("create");
				
				jmod_args.add("--class-path");
				jmod_args.add(project.getClassesPath().toString());
				
				jmod_args.add("--module-version");
				jmod_args.add(project.getModuleVersion());
					
				if(this.configurationPath != null && Files.exists(this.configurationPath)) {
					jmod_args.add("--config");
					jmod_args.add(this.configurationPath.toString());
				}
				if(this.legalPath != null && Files.exists(this.legalPath)) {
					jmod_args.add("--legal-notices");
					jmod_args.add(this.legalPath.toString());
				}
				if(this.manPath != null && Files.exists(this.manPath)) {
					jmod_args.add("--man");
					jmod_args.add(this.manPath.toString());
				}
				
				String resolvedMainClass = null;;
				if(StringUtils.isNotBlank(this.mainClass)) {
					resolvedMainClass = this.mainClass;
				}
				else if(this.resolveMainClass) {
					try {
						Set<String> mainClasses = project.getMainClasses();
						if(mainClasses.size() == 1) {
							resolvedMainClass = mainClasses.stream().findFirst().get();
						}
						else if(mainClasses.size() > 1) {
							throw new TaskExecutionException("Project module " + project.getModuleName() + " defines multipled main classes: " + mainClasses.stream().collect(Collectors.joining(", ")) + ". Please specifies one explicitly");
						}
					} 
					catch (ClassNotFoundException | IOException e) {
						LOGGER.warn("Could not find project main class", e);
					}
				}
				if(resolvedMainClass != null) {
					try {
						project.setDefaultMainClass(resolvedMainClass);
					}
					catch (ClassNotFoundException | IOException | IllegalArgumentException e) {
						throw new TaskExecutionException("Could not resolve project main class", e);
					}
					jmod_args.add("--main-class");
					jmod_args.add(resolvedMainClass);
				}
				
				jmod_args.add(project.getModulePath().toString());
				
				LOGGER.info(" - jmod {}", jmod_args.stream().collect(Collectors.joining(" ")));
				if(JavaTools.JMOD.run(OUT, ERR, jmod_args.stream().toArray(String[]::new)) != 0) {
					throw new TaskExecutionException("Error creating project jmod");
				}
			} 
			catch (IOException e) {
				throw new TaskExecutionException("Error creating project jmod", e);
			}
		}
		else {
			LOGGER.info("[ Project jmod is up to date ]");
		}
		return project.getModulePath();
	}
	
	@Override
	public BuildRuntimeTask buildRuntime() {
		return new GenericBuildRuntimeTask(this);
	}
}
