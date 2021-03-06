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
package io.inverno.tool.maven.internal.task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;

import io.inverno.tool.maven.internal.DependencyModule;
import io.inverno.tool.maven.internal.ProjectModule;
import io.inverno.tool.maven.internal.Task;
import io.inverno.tool.maven.internal.TaskExecutionException;
import io.inverno.tool.maven.internal.ProgressBar.Step;

/**
 * <p>
 * Compiles generated or user-defined module descriptors for non-modular project
 * dependencies.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class CompileModuleDescriptorsTask extends Task<Void> {

	private final ToolProvider javac;
	private final ProjectModule projectModule;
	private final Path jmodsExplodedPath;
	
	public CompileModuleDescriptorsTask(AbstractMojo mojo, ToolProvider javac, ProjectModule projectModule, Path jmodsExplodedPath) {
		super(mojo);
		this.javac = javac;
		this.projectModule = projectModule;
		this.jmodsExplodedPath = jmodsExplodedPath;
	}
	
	@Override
	public void setStep(Step step) {
		if(step != null) {
			step.setDescription("Compiling modularized project dependencies...");
		}
		super.setStep(step);
	}

	@Override
	protected Void execute() throws TaskExecutionException {
		if(this.projectModule.getModuleDependencies().stream().anyMatch(DependencyModule::isMarked)) {
			if(this.verbose) {
				this.getLog().info("[ Compiling modularized project dependencies... ]");
			}

			String javac_modulePath = this.projectModule.getModuleDependencies().stream().filter(dependency -> !dependency.isAutomatic()).map(d -> d.getSourcePath().toString()).collect(Collectors.joining(System.getProperty("path.separator")));

			List<String> javac_args = new ArrayList<>();
			
			if(this.verbose) {
				javac_args.add("-verbose");
			}
			else {
				javac_args.add("-nowarn");
			}
			javac_args.add("--module-source-path");
			javac_args.add(this.jmodsExplodedPath.toString());
			javac_args.add("-d");
			javac_args.add(this.jmodsExplodedPath.toString());
			javac_args.add("--module-path");
			javac_args.add(javac_modulePath);
			
			int size = javac_args.size();
			javac_args.addAll(this.projectModule.getModuleDependencies().stream().filter(dependency -> dependency.isAutomatic() && dependency.isMarked()).map(d -> d.getExplodedJmodPath().resolve("module-info.java").toString()).collect(Collectors.toList()));
			if(javac_args.size() == size) {
				// Nothing to compile
				if(this.verbose) {
					this.getLog().info("Nothing to compile");
				}
				return null;
			}
			
			if(this.verbose) {
				this.getLog().info(" - javac " + javac_args.stream().collect(Collectors.joining(" ")));
			}
			if(this.javac.run(this.getOutStream(), this.getOutStream(), javac_args.stream().toArray(String[]::new)) != 0) {
				throw new TaskExecutionException("Error compiling generated module descriptors, activate '-Dinverno.verbose=true' to display full log");
			}
		}
		else {
			if(this.verbose) {
				this.getLog().info("[ Project dependencies are up to date ]");
			}
		}
		return null;
	}
}
