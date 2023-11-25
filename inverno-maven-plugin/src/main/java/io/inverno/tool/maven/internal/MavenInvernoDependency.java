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
import java.nio.file.Path;
import org.apache.maven.artifact.Artifact;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class MavenInvernoDependency implements Dependency {

	private final Artifact artifact;
	
	public MavenInvernoDependency(Artifact artifact) {
		this.artifact = artifact;
	}
	
	@Override
	public Path getJarPath() {
		return this.artifact.getFile().toPath().toAbsolutePath();
	}

	@Override
	public String getGroup() {
		return this.artifact.getGroupId();
	}

	@Override
	public String getName() {
		return this.artifact.getVersion();
	}

	@Override
	public String getVersion() {
		return this.artifact.getVersion();
	}

	@Override
	public String toString() {
		return this.getGroup() + ":" + this.getName() + ":" + this.getVersion();
	}
}
