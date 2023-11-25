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

import io.inverno.tool.buildtools.internal.ImageType;
import io.inverno.tool.buildtools.internal.JavaTools;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class TestProject extends Project {
	
	public static final String GROUP = "io.inverno.test";

	public static final Path TARGET_PATH = Path.of("target/inverno-target").toAbsolutePath();
	public static final Path DEPENDENCIES_TARGET_PATH = Path.of("target/inverno-target/dependencies").toAbsolutePath();
	public static final Path WORKING_PATH = Path.of("target/inverno-build").toAbsolutePath();
	
	private static final Map<String, String> MODULE_NAMES_BY_DEPENDENCY_NAME = Map.of(
			"module-dep", "io.inverno.test.moduleDep",
			"automatic-module-dep", "io.inverno.test.automaticModuleDep",
			"unnamed-dep", "io.inverno.test.unnamed.dep",
			"webjar-dep", "org.webjars.webjar.dep"
		);
	
	private static final Map<String, String> VERSIONS_BY_DEPENDENCY_NAME = Map.of(
			"module-dep", "1.0.1",
			"automatic-module-dep", "1.0.2",
			"unnamed-dep", "1.0.3",
			"webjar-dep", "1.0.4"
		);
	
	private final Path sourcePath;
	private final Set<TestDependency> dependencies;
	
	public TestProject() {
		super(TARGET_PATH, WORKING_PATH);
		this.sourcePath = Path.of("src/test/resources/project").toAbsolutePath();
		this.dependencies = Set.of(
			new TestDependency(GROUP, "module-dep", getDependencyVersion("module-dep"), Path.of("src/test/resources/dependencies/module-dep"), DEPENDENCIES_TARGET_PATH),
			new TestDependency(GROUP, "automatic-module-dep", getDependencyVersion("automatic-module-dep"), Path.of("src/test/resources/dependencies/automatic-module-dep"), DEPENDENCIES_TARGET_PATH),
			new TestDependency(GROUP, "unnamed-dep", getDependencyVersion("unnamed-dep"), Path.of("src/test/resources/dependencies/unnamed-dep"), DEPENDENCIES_TARGET_PATH),
			new TestDependency("org.webjars", "webjar-dep", getDependencyVersion("webjar-dep"), Path.of("src/test/resources/dependencies/webjar-dep"), DEPENDENCIES_TARGET_PATH)	
		);
	}
	
	public static String getDependencyVersion(String dependencyName) {
		return VERSIONS_BY_DEPENDENCY_NAME.get(dependencyName);
	}
	
	public static String getDependencyModuleName(String dependencyName) {
		return MODULE_NAMES_BY_DEPENDENCY_NAME.get(dependencyName);
	}
	
	public static Path getDependencyModulePath(String dependencyName) {
		return TestProject.WORKING_PATH.resolve(Path.of("modules", TestProject.getDependencyModuleName(dependencyName) + "-" + TestProject.getDependencyVersion(dependencyName) + ".jar"));
	}
	
	public static Path getDependencyModuleExplodedPath(String dependencyName) {
		return TestProject.WORKING_PATH.resolve(Path.of("modules-exploded", TestProject.getDependencyModuleName(dependencyName)));
	}
	
	public static Path getDependencyModuleUnnamedPath(String dependencyName) {
		return TestProject.WORKING_PATH.resolve(Path.of("modules-unnamed", TestProject.getDependencyModuleName(dependencyName) + "-" + getDependencyVersion(dependencyName) + ".jar"));
	}
	
	public void compile() {
		try {
			Files.createDirectories(this.getClassesPath());
		
			List<String> javac_args = new ArrayList<>();
			
			String modulesPath = this.getDependencies().stream()
				.peek(TestDependency::build)
				.map(dependency -> dependency.getJarPath().toString())
				.collect(Collectors.joining(System.getProperty("path.separator")));
	
			javac_args.add("--module-path");
			javac_args.add(modulesPath);
			
			javac_args.add("-d");
			javac_args.add(this.getClassesPath().toString());
			
			Files.walk(this.sourcePath)
				.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().endsWith(".java"))
				.forEach(p -> javac_args.add(p.toString()));
			
			if(JavaTools.JAVAC.run(System.out, System.err, javac_args.stream().toArray(String[]::new)) != 0) {
				throw new RuntimeException("Error compiling " + this.sourcePath);
			}
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void clean() {
		try {
			this.getDependencies().forEach(TestDependency::clean);

			if(Files.exists(TARGET_PATH)) {
				Files.walk(TARGET_PATH)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
			}
			
			if(Files.exists(WORKING_PATH)) {
				Files.walk(WORKING_PATH)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
			}
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Path getClassesPath() {
		return this.getTargetPath().resolve("project-classes");
	}

	@Override
	public Set<TestDependency> getDependencies() {
		return this.dependencies;
	}

	@Override
	public String getGroup() {
		return GROUP;
	}

	@Override
	public String getName() {
		return "project";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}
	
	public String getModuleName() {
		return this.getGroup() + "." + this.getName();
	}
	
	public Path getJmodPath() {
		return TestProject.WORKING_PATH.resolve(Path.of("modules", this.getModuleName() + "-" + this.getVersion() + ".jmod"));
	}
	
	public Path getRuntimePath() {
		return TestProject.TARGET_PATH.resolve(this.getFinalName() + "-" + ImageType.RUNTIME.getNativeQualifier());
	}
	
	public Path getArchivePath(String format) {
		return TestProject.TARGET_PATH.resolve(this.getFinalName() + "-" + ImageType.RUNTIME.getNativeQualifier() + "." + format);
	}
	
	public Path getApplicationPath(String format) {
		if(format == null) {
			return TestProject.TARGET_PATH.resolve(Path.of(this.getFinalName() + "-" + ImageType.APPLICATION.getNativeQualifier()));
		}
		else {
			return TestProject.TARGET_PATH.resolve(Path.of(this.getFinalName() + "-" + ImageType.APPLICATION.getNativeQualifier() + "." + format));
		}
	}
	
	public Path getContainerImagePath() {
		return TestProject.TARGET_PATH.resolve(this.getFinalName() + "-" + ImageType.CONTAINER.getNativeQualifier() + ".tar");
	}
	
	public Path getPidfile() {
		// This must match BuildProject#getPidfile()
		return TestProject.WORKING_PATH.resolve(this.getName() + ".pid");
	}
}
