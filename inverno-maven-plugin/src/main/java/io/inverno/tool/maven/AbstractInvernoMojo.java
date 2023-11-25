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
package io.inverno.tool.maven;

import io.inverno.tool.buildtools.ModularizeDependenciesTask;
import io.inverno.tool.buildtools.Project;
import io.inverno.tool.maven.internal.MavenInvernoProject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Base Inverno mojo.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractInvernoMojo extends AbstractMojo {
	
	@Parameter( defaultValue = "${project}", readonly = true, required = true )
	protected MavenProject mavenProject;
	
	@Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true )
	protected MojoExecution mojoExecution;
	
	private Path logPath = Path.of(this.mavenProject.getBuild().getDirectory(), "maven-inverno.log").toAbsolutePath();
	
	/**
	 * A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
	 */
	@Parameter(property = "inverno.moduleOverridesDirectory", defaultValue = "${project.basedir}/src/modules/", required = false)
	protected File moduleOverridesDirectory;
	
	/**
	 * A list of {@code module-info.java} overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
	 */
	@Parameter(required = false)
	protected List<ModuleInfoParameters> moduleOverrides;
	
	/**
	 * Displays a progress bar.
	 */
	@Parameter(property = "inverno.progressBar", defaultValue = "true", required = false)
	protected boolean progressBar;
	
	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		if(!this.isSkipped()) {
			Logger logger = this.configureLogging();
			
			if(this.progressBar) {
				System.setProperty(Project.PROPERY_DISPLAY_PROGRESS_BAR, Boolean.toString(true));
			}
			
			try {
				this.doExecute(this.configureProject(new MavenInvernoProject.Builder(this.mavenProject)).build());
			}
			catch(Exception e) {
				logger.error("Failed to execute goal " + this.mojoExecution.getMojoDescriptor().getFullGoalName(), e);
				throw new MojoExecutionException("Failed to execute goal " + this.mojoExecution.getMojoDescriptor().getFullGoalName() + ", please consult " + this.logPath.toFile().getAbsolutePath() + " for more details", e);
			}
		}
		else {
			this.getLog().info("Execution is skipped");
		}
	}
	
	/**
	 * <p>
	 * Configures Inverno build tools log manager to append task logs to the logPath (i.e. {@code target/maven-inverno.log}).
	 * </p>
	 * 
	 * @return 
	 */
	private Logger configureLogging() {
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		
		LayoutComponentBuilder layout = builder.newLayout("PatternLayout");
		layout.addAttribute("pattern", "[%level] %msg%n%throwable");
		
		AppenderComponentBuilder outputAppender = builder.newAppender("inverno-maven-plugin-log", "File"); 
		outputAppender.addAttribute("fileName", this.logPath.toFile().getAbsolutePath());
		outputAppender.add(layout);
		
		builder.add(outputAppender);
		
		LoggerComponentBuilder logger = builder.newLogger("io.inverno.tool", Level.INFO);
		logger.add(builder.newAppenderRef("inverno-maven-plugin-log"));
		logger.addAttribute("additivity", false);

		builder.add(logger);
		
		Configurator.initialize(builder.build());
		
		return LogManager.getLogger(this.getClass());
	}
	
	/**
	 * <p>
	 * Configure the Maven project builder.
	 * </p>
	 * 
	 * @param projectBuilder the Maven Inverno project builder
	 * 
	 * @return the Maven Inverno project builder
	 */
	protected MavenInvernoProject.Builder configureProject(MavenInvernoProject.Builder projectBuilder) {
		return projectBuilder;
	}
	
	/**
	 * <p>
	 * Configures the modularize dependencies task.
	 * </p>
	 * 
	 * @param modularizeDependenciesTask the modularize dependencies task
	 * 
	 * @return the modularize dependencies task
	 */
	protected ModularizeDependenciesTask configureTask(ModularizeDependenciesTask modularizeDependenciesTask) {
		return modularizeDependenciesTask
			.moduleOverridesPath(this.moduleOverridesDirectory != null ? this.moduleOverridesDirectory.toPath().toAbsolutePath() : null)
			.moduleOverrides(this.moduleOverrides);
	}
	
	/**
	 * <p>
	 * Determines whether the execution should be skipped.
	 * </p>
	 * 
	 * @return true to skip execution, false otherwise
	 */
	protected abstract boolean isSkipped();
	
	/**
	 * <p>
	 * Executes the build task.
	 * </p>
	 * 
	 * @param project the Maven Inverno build project
	 * 
	 * @throws Exception if there was an error executing the build task
	 */
	protected abstract void doExecute(MavenInvernoProject project) throws Exception;
}
