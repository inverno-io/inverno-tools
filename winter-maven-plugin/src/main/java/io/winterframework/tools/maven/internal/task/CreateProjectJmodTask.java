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
package io.winterframework.tools.maven.internal.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;

import io.winterframework.tools.maven.internal.NullPrintStream;
import io.winterframework.tools.maven.internal.ProjectModule;
import io.winterframework.tools.maven.internal.Task;
import io.winterframework.tools.maven.internal.TaskExecutionException;

/**
 * @author jkuhn
 *
 */
public class CreateProjectJmodTask extends Task<Void> {

	private final ToolProvider jmod;
	private final ProjectModule projectModule;
	private final boolean resolveMainClass;
	
	private String projectMainClass;
	private Path projectConfPath;
	private Path projectLegalPath;
	private Path projectManPath;
	
	
	public CreateProjectJmodTask(AbstractMojo mojo, ToolProvider jmod, ProjectModule projectModule, boolean resolveMainClass) {
		super(mojo);
		this.jmod = jmod;
		this.projectModule = projectModule;
		this.resolveMainClass = resolveMainClass;
	}
	
	@Override
	public Void call() throws TaskExecutionException {
		if(this.verbose) {
			this.getLog().info("[ Creating project jmod: " + this.projectModule.getJmodPath() + "... ]");
		}
		
		try {
			if(this.projectModule.isMarked()) {
				Files.deleteIfExists(this.projectModule.getJmodPath());
				
				List<String> jmod_args = new LinkedList<>();
				
				jmod_args.add("create");
				
				jmod_args.add("--class-path");
				jmod_args.add(this.projectModule.getClassesPath().toString());
				
				jmod_args.add("--module-version");
				jmod_args.add(this.projectModule.getModuleVersion());
					
				if(this.projectConfPath != null && Files.exists(this.projectConfPath)) {
					jmod_args.add("--config");
					jmod_args.add(this.projectConfPath.toString());
				}
				if(this.projectLegalPath != null && Files.exists(this.projectLegalPath)) {
					jmod_args.add("--legal-notices");
					jmod_args.add(this.projectConfPath.toString());
				}
				if(this.projectManPath != null && Files.exists(this.projectManPath)) {
					jmod_args.add("--man");
					jmod_args.add(this.projectManPath.toString());
				}
				
				if(StringUtils.isNotEmpty(this.projectMainClass)) {
					jmod_args.add("--main-class");
					jmod_args.add(this.projectMainClass);
				}
				else if(this.resolveMainClass) {
					try {
						Set<String> mainClasses = this.projectModule.getMainClasses();
						if(mainClasses.size() == 1) {
							jmod_args.add("--main-class");
							jmod_args.add(mainClasses.stream().findFirst().get());
						}
						else if(mainClasses.size() > 1) {
							throw new TaskExecutionException("Project module " + this.projectModule.getModuleName() + " defines multipled main classes: " + mainClasses.stream().collect(Collectors.joining(", ")) + ". Please specifies one with -Dwinter.image.mainClass=...");
						}
					} 
					catch (ClassNotFoundException | IOException e) {
						if(this.verbose) {
							this.getLog().warn("Could not find project main class", e);
						}
					}
				}
				jmod_args.add(this.projectModule.getJmodPath().toString());
				
				if(this.verbose) {
					this.getLog().info(" - jmod " + jmod_args.stream().collect(Collectors.joining(" ")));
				}
				if(this.jmod.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), jmod_args.stream().toArray(String[]::new)) != 0) {
					throw new TaskExecutionException("Error creating project jmod, activate '-Dwinter.image.verbose=true' to display full log");
				}
			}
			else {
				if(this.verbose) {
					this.getLog().info(" - project jmod is up to date");
				}
			}
		} 
		catch (IOException e) {
			throw new TaskExecutionException("Error creating project jmod", e);
		}
		return null;
	}

	public String getProjectMainClass() {
		return projectMainClass;
	}

	public void setProjectMainClass(String projectMainClass) {
		this.projectMainClass = projectMainClass;
	}

	public Path getProjectConfPath() {
		return projectConfPath;
	}

	public void setProjectConfPath(Path projectConfPath) {
		this.projectConfPath = projectConfPath;
	}

	public Path getProjectLegalPath() {
		return projectLegalPath;
	}

	public void setProjectLegalPath(Path projectLegalPath) {
		this.projectLegalPath = projectLegalPath;
	}

	public Path getProjectManPath() {
		return projectManPath;
	}

	public void setProjectManPath(Path projectManPath) {
		this.projectManPath = projectManPath;
	}
}
