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
package io.winterframework.tool.maven;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.winterframework.tool.maven.internal.ProjectModule;

/**
 * <p>
 * Starts the project application without blocking the Maven build.
 * </p>
 * 
 * <p>
 * This goal is used together with the {@code stop} goal in the
 * {@code pre-integration-test} and {@code post-integration-test} phases to run
 * integration tests.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresProject = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class StartMojo extends AbstractExecMojo {

	/**
	 * The amount of time in milliseconds to wait for the application to start.
	 */
	@Parameter(property = "winter.start.timeout", defaultValue = "60000", required = false)
	protected long timeout;
	
	@Override
	protected void handleProcess(ProjectModule projectModule, Process proc) throws MojoExecutionException, MojoFailureException {
		if(proc.isAlive()) {
			// We must wait for the pidfile to appear
			
			Path pidfile = projectModule.getPidfile();
			try(WatchService watchService = FileSystems.getDefault().newWatchService()) {
				pidfile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
				int tries = (int) (this.timeout/1000);
				WatchKey watchKey = null;
				for(int i = 0;i<tries;i++) {
					watchKey = watchService.poll(1000, TimeUnit.MILLISECONDS);
					if(watchKey == null) {
						if(!proc.isAlive()) {
							throw new MojoExecutionException("Application exited: exit(" + proc.exitValue() + ")");
						}
					}
					else if(watchKey.pollEvents().stream().map(WatchEvent::context).anyMatch(path -> path.equals(pidfile.getFileName()))) {
						watchKey.cancel();
						return;
					}
					else {
						watchKey.cancel();
					}
				}
				
				if(watchKey == null) {
					// proc is alive at this stage
					this.getLog().error("Application startup timeout exceeded, trying to stop the process gracefully...");
					try {
						this.destroyProcess(proc);
					}
					finally {
						new MojoExecutionException("Application startup timeout exceeded");
					}
				}
			} 
			catch (IOException | InterruptedException e) {
				try {
					if(proc.isAlive()) {
						this.getLog().error("Fatal error, trying to stop the process gracefully...");
						this.destroyProcess(proc);
					}
				}
				finally {
					new MojoExecutionException("Fatal error", e);
				}
			}
		}
		else {
			throw new MojoExecutionException("Application exited: exit(" + proc.exitValue() + ")");
		}
	}
	
	private void destroyProcess(Process proc) {
		ProcessHandle ph = proc.toHandle();
		ph.destroy();
		try {
			ph.onExit().get(this.timeout, TimeUnit.MILLISECONDS);
		} 
		catch (InterruptedException | ExecutionException e) {
			this.getLog().error(e);
		}
		catch (TimeoutException e) {
			ph.destroyForcibly();
		}
	}
}
