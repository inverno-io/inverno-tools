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
import java.util.Arrays;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;

import io.winterframework.tools.maven.internal.DependencyModule;
import io.winterframework.tools.maven.internal.NullPrintStream;
import io.winterframework.tools.maven.internal.ProjectModule;
import io.winterframework.tools.maven.internal.Task;
import io.winterframework.tools.maven.internal.TaskExecutionException;

/**
 * @author jkuhn
 *
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
	public Void call() throws TaskExecutionException {
		if(this.verbose) {
			this.getLog().info("[ Packaging modularized project dependencies... ]");
		}
		
		for(DependencyModule dependency : this.projectModule.getModuleDependencies()) {
			if(dependency.isMarked() && dependency.getModuleDescriptor().isAutomatic()) {
				if(this.verbose) {
					this.getLog().info(" - " + dependency);
				}
				this.packageDependency(dependency);
			}
		}
		return null;
	}
	
	private void packageDependency(DependencyModule dependency) throws TaskExecutionException {
		try {
			Files.delete(dependency.getExplodedJmodPath().resolve("module-info.java"));
			
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
				throw new TaskExecutionException("Error packaging dependency " + dependency + ", activate '-Dwinter.image.verbose=true' to display full jar log");
			}
		} 
		catch (IOException e) {
			throw new TaskExecutionException("Error packaging dependency " + dependency, e);
		}
	}
}
