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
package io.inverno.tool.maven;

import java.io.IOException;
import java.nio.file.Path;

import io.inverno.tool.maven.internal.ProgressBar;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Base Inverno mojo.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractInvernoMojo extends AbstractMojo {

	/**
	 * Enables verbose logging.
	 */
	@Parameter(property = "inverno.verbose", defaultValue = "false", required = false)
	protected boolean verbose;
	
	@Parameter( defaultValue = "${project}", readonly = true, required = true )
	protected MavenProject project;
    
	protected Path invernoBuildPath;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if(!this.isSkip()) {
			try {
				this.initializePaths();
			}
			catch (IOException e) {
				throw new MojoExecutionException("Error initializing paths", e);
			}
			this.doExecute();
		}
		else {
			this.getLog().info("Execution is skipped");
		}
	}
	
	protected abstract boolean isSkip();
	
	protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;
	
	protected void initializePaths() throws IOException {
		this.invernoBuildPath = Path.of(this.project.getBuild().getDirectory(), "maven-inverno");
	}
	
	protected ProgressBar createProgressBar() {
		ProgressBar progressBar = new ProgressBar();
		progressBar.setEnabled(!this.verbose);
		
		return progressBar;
	}
}
