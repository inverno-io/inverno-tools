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
package io.inverno.tool.buildtools;

import io.inverno.tool.buildtools.internal.JavaTools;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class TestDependency implements Dependency {

	private final String group;
	private final String name;
	private final String version;
	
	private final Path sourcePath;
	private final Path targetPath;
	private final Path classesPath;
	private final Path jarPath;

	public TestDependency(String group, String name, String version, Path sourcePath, Path targetPath) {
		this.group = group;
		this.name = name;
		this.version = version;
		this.sourcePath = sourcePath;
		this.targetPath = targetPath;
		this.classesPath = this.targetPath.resolve(Path.of("dependencies-classes", this.sourcePath.getFileName().toString())).toAbsolutePath();
		this.jarPath = this.targetPath.resolve(this.name + "-" + this.version + ".jar").toAbsolutePath();
	}

	public void compile() {
		try {
			Files.createDirectories(this.classesPath);
		
			Path metaInfPath = this.sourcePath.resolve("META-INF").toAbsolutePath();
			if(Files.exists(metaInfPath)) {
				Files.walk(metaInfPath).forEach(source -> {
					Path destination = this.classesPath.resolve(this.sourcePath.toAbsolutePath().relativize(source.toAbsolutePath()));
					try {
						if(!Files.exists(destination) || Files.getLastModifiedTime(source).toMillis() > Files.getLastModifiedTime(destination).toMillis()) {
							Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
						}
					} 
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
			
			List<String> javac_args = new ArrayList<>();
			
			javac_args.add("-d");
			javac_args.add(this.classesPath.toString());
			
			int javac_args_size = javac_args.size();
			Files.walk(this.sourcePath)
				.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().endsWith(".java"))
				.forEach(p -> javac_args.add(p.toString()));
			
			if(javac_args.size() == javac_args_size) {
				// Nothing to compile
				return;
			}
			
			if(JavaTools.JAVAC.run(System.out, System.err, javac_args.stream().toArray(String[]::new)) != 0) {
				throw new RuntimeException("Error compiling " + this.sourcePath);
			}
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void build() {
		this.compile();
		
		try {
			Files.createDirectories(this.jarPath.getParent());
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
		
		String[] jar_args = {
			"--create",
			"--no-manifest",
			"--file", this.getJarPath().toString(),
			"-C", this.classesPath.toString(),
			"."
		};
		
		if(JavaTools.JAR.run(System.out, System.err, jar_args) != 0) {
			throw new RuntimeException("Error packaging " + this.classesPath);
		}
	}
	
	public void clean() {
		try {
			if(Files.exists(this.targetPath)) {
				Files.walk(this.targetPath)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
			}
			
			Files.deleteIfExists(this.jarPath);
		} 
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Path getJarPath() {
		return this.jarPath;
	}

	@Override
	public String getGroup() {
		return this.group;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getVersion() {
		return this.version;
	}

}
