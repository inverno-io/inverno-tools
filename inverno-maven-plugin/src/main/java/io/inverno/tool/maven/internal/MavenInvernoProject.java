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
package io.inverno.tool.maven.internal;

import io.inverno.tool.buildtools.Dependency;
import io.inverno.tool.buildtools.Project;
import io.inverno.tool.buildtools.TaskExecutionException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class MavenInvernoProject extends Project {
	
	private static final Logger LOGGER = LogManager.getLogger(MavenInvernoProject.class);

	private final MavenProject mavenProject;
	
	private final Set<MavenInvernoDependency> dependencies;
	
	private MavenInvernoProject(MavenProject mavenProject, Set<MavenInvernoDependency> dependencies) {
		super(Path.of(mavenProject.getBuild().getDirectory()).toAbsolutePath(), Path.of(mavenProject.getBuild().getDirectory(), "maven-inverno").toAbsolutePath());
		
		this.mavenProject = mavenProject;
		this.dependencies = dependencies;
	}
	
	public MavenProject getMavenProject() {
		return mavenProject;
	}
	
	@Override
	public Path getClassesPath() {
		return Path.of(this.mavenProject.getBuild().getOutputDirectory()).toAbsolutePath();
	}

	@Override
	public Set<? extends Dependency> getDependencies() {
		return this.dependencies;
	}

	@Override
	public String getGroup() {
		return this.mavenProject.getGroupId();
	}

	@Override
	public String getName() {
		return this.mavenProject.getArtifactId();
	}

	@Override
	public String getVersion() {
		return this.mavenProject.getVersion();
	}
	
	public Path getPidfile() {
		return this.getWorkingPath().resolve(this.getName() + ".pid").toAbsolutePath();
	}

	@Override
	public String toString() {
		return this.getGroup() + ":" + this.getName() + ":" + this.getVersion();
	}
	
	public static class Builder {
	
		private final MavenProject mavenProject;

		private String includeScope;
		private String excludeScope;

		private String includeTypes;
		private String excludeTypes;

		private String includeClassifiers;
		private String excludeClassifiers;

		private String includeArtifactIds;
		private String excludeArtifactIds;

		private String includeGroupIds;
		private String excludeGroupIds;

		public Builder(MavenProject mavenProject) {
			this.mavenProject = mavenProject;
		}
		
		public MavenInvernoProject.Builder includeScope(String includeScope) {
			this.includeScope = includeScope;
			return this;
		}

		public MavenInvernoProject.Builder excludeScope(String excludeScope) {
			this.excludeScope = excludeScope;
			return this;
		}

		public MavenInvernoProject.Builder includeTypes(String includeTypes) {
			this.includeTypes = includeTypes;
			return this;
		}

		public MavenInvernoProject.Builder excludeTypes(String excludeTypes) {
			this.excludeTypes = excludeTypes;
			return this;
		}

		public MavenInvernoProject.Builder includeClassifiers(String includeClassifiers) {
			this.includeClassifiers = includeClassifiers;
			return this;
		}

		public MavenInvernoProject.Builder excludeClassifiers(String excludeClassifiers) {
			this.excludeClassifiers = excludeClassifiers;
			return this;
		}
		
		public MavenInvernoProject.Builder includeArtifactIds(String includeArtifactIds) {
			this.includeArtifactIds = includeArtifactIds;
			return this;
		}

		public MavenInvernoProject.Builder excludeArtifactIds(String excludeArtifactIds) {
			this.excludeArtifactIds = excludeArtifactIds;
			return this;
		}

		public MavenInvernoProject.Builder includeGroupIds(String includeGroupIds) {
			this.includeGroupIds = includeGroupIds;
			return this;
		}

		public MavenInvernoProject.Builder excludeGroupIds(String excludeGroupIds) {
			this.excludeGroupIds = excludeGroupIds;
			return this;
		}

		public MavenInvernoProject build() throws TaskExecutionException {
			LOGGER.info("[ Resolving dependencies for {}... ]", this.mavenProject);
			try {
				// add filters in well known order, least specific to most specific
				FilterArtifacts filter = new FilterArtifacts();
				filter.addFilter(new ScopeFilter(cleanToBeTokenizedString(this.includeScope), cleanToBeTokenizedString(this.excludeScope)));
				filter.addFilter(new TypeFilter(cleanToBeTokenizedString(this.includeTypes), cleanToBeTokenizedString(this.excludeTypes)));
				filter.addFilter(new ClassifierFilter(cleanToBeTokenizedString(this.includeClassifiers), cleanToBeTokenizedString(this.excludeClassifiers)));
				filter.addFilter(new GroupIdFilter(cleanToBeTokenizedString(this.includeGroupIds), cleanToBeTokenizedString(this.excludeGroupIds)));
				filter.addFilter(new ArtifactIdFilter(cleanToBeTokenizedString(this.includeArtifactIds), cleanToBeTokenizedString(this.excludeArtifactIds)));

				// Filter artifacts
				Set<Artifact> filteredArtifacts = filter.filter(this.mavenProject.getArtifacts());

				return new MavenInvernoProject(
					this.mavenProject, 
					filteredArtifacts.stream()
						.map(MavenInvernoDependency::new)
						.peek(dependency -> LOGGER.info(" - " + dependency))
						.collect(Collectors.toSet())
				);
			}
			catch (ArtifactFilterException e) {
				throw new TaskExecutionException("Error resolving project dependencies", e);
			}
		}

		private static String cleanToBeTokenizedString(String str) {
			String ret = "";
			if (!StringUtils.isEmpty(str)) {
				// remove initial and ending spaces, plus all spaces next to commas
				ret = str.trim().replaceAll("[\\s]*,[\\s]*", ",");
			}
			return ret;
		}
		
	}
}
