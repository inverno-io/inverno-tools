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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;

import io.inverno.tool.maven.internal.DependencyModule;
import io.inverno.tool.maven.internal.NullPrintStream;
import io.inverno.tool.maven.internal.ProgressBar.Step;
import io.inverno.tool.maven.internal.ProjectModule;
import io.inverno.tool.maven.internal.Task;
import io.inverno.tool.maven.internal.TaskExecutionException;

/**
 * <p>
 * Packages the modularized project dependencies into modular JARs.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class PackageModularizedDependenciesTask extends Task<Void> {

	private final ToolProvider jar;
	private final ProjectModule projectModule;
	
	public PackageModularizedDependenciesTask(AbstractMojo mojo, ToolProvider jar, ProjectModule projectModule) {
		super(mojo);
		this.jar = jar;
		this.projectModule = projectModule;
	}

	@Override
	public void setStep(Step step) {
		if(step != null) {
			step.setDescription("Packaging modularized project dependencies...");
		}
		super.setStep(step);
	}
	
	@Override
	protected Void execute() throws TaskExecutionException {
		if(this.projectModule.getModuleDependencies().stream().anyMatch(dependency -> dependency.isMarked() && dependency.isAutomatic())) {
			if(this.verbose) {
				this.getLog().info("[ Packaging modularized project dependencies... ]");
			}
			
			for(DependencyModule dependency : this.projectModule.getModuleDependencies()) {
				if(dependency.isMarked() && dependency.isAutomatic()) {
					if(this.verbose) {
						this.getLog().info(" - " + dependency);
					}
					this.packageDependency(dependency);
				}
			}
		}
		else {
			if(this.verbose) {
				this.getLog().info("[ Project dependencies are up to date ]");
			}
		}
		return null;
	}
	
	private void packageDependency(DependencyModule dependency) throws TaskExecutionException {
		String[] jar_args = {
			"--create",
			"--no-manifest",
			"--file", dependency.getJmodPath().toString(),
			"--module-version", dependency.getModuleVersion(),
			"-C", dependency.getExplodedJmodPath().toString(),
			"."
		};
		
		if(this.verbose) {
			this.getLog().info("   - jar " + Arrays.stream(jar_args).collect(Collectors.joining(" ")));
		}
		
		if(this.jar.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), jar_args) != 0) {
			throw new TaskExecutionException("Error packaging dependency " + dependency + ", activate '-Dinverno.verbose=true' to display full jar log");
		}
		
		try(FileSystem jarFs = FileSystems.newFileSystem(URI.create("jar:" + dependency.getJmodPath().toUri()), Map.of())) {
			Files.deleteIfExists(jarFs.getPath("module-info.java"));
		}
		catch (IOException e) {
			throw new TaskExecutionException("Error packaging dependency " + dependency, e);
		}
	}
}
