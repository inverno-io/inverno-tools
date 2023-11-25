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
package io.inverno.tool.buildtools.internal;

import io.inverno.tool.buildtools.Dependency;
import io.inverno.tool.buildtools.ModuleInfo;
import io.inverno.tool.buildtools.TestProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ModularizeDependenciesTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private TestProject project;
	
	@BeforeEach
	public void init() {
		this.project = new TestProject();
		this.project.compile();
	}
	
	@AfterEach
	public void cleanup() {
		this.project.clean();
	}
	
	@Test
	public void testExecute() throws Exception {
		Map<String, Dependency> modularizedDependencies = this.project.modularizeDependencies()
			.execute().stream()
			.collect(Collectors.toMap(Dependency::getName, Function.identity()));
		
		String dependencyGroup = TestProject.GROUP;
		String dependencyName = "module-dep";
		Assertions.assertTrue(Files.exists(TestProject.WORKING_PATH.resolve(TestProject.getDependencyModulePath(dependencyName))));
		
		Assertions.assertEquals(3, modularizedDependencies.size());
		
		dependencyGroup = TestProject.GROUP;
		dependencyName = "automatic-module-dep";
		Dependency dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertFalse(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		Path moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"module " + TestProject.getDependencyModuleName(dependencyName) + " { " +
			"exports io.inverno.test.automaticmoduledep;" +
			"}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
		
		dependencyGroup = TestProject.GROUP;
		dependencyName = "unnamed-dep";
		dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"module " + TestProject.getDependencyModuleName(dependencyName) + " { " +
			"exports io.inverno.test.unnamedmoduledep;" +
			"}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
		
		dependencyGroup = "org.webjars";
		dependencyName = "webjar-dep";
		dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"module " + TestProject.getDependencyModuleName(dependencyName) + " {}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
	}
	
	@Test
	public void testExecuteWithModuleOverridesPath() throws Exception {
		Map<String, Dependency> modularizedDependencies = this.project.modularizeDependencies()
			.moduleOverridesPath(Path.of("src/test/resources/modules-override"))
			.execute().stream()
			.collect(Collectors.toMap(Dependency::getName, Function.identity()));
		
		String dependencyGroup = TestProject.GROUP;
		String dependencyName = "module-dep";
		Assertions.assertTrue(Files.exists(TestProject.WORKING_PATH.resolve(TestProject.getDependencyModulePath(dependencyName))));
		
		Assertions.assertEquals(3, modularizedDependencies.size());
		
		dependencyGroup = TestProject.GROUP;
		dependencyName = "automatic-module-dep";
		Dependency dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertFalse(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		Path moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"open module " + TestProject.getDependencyModuleName(dependencyName) + " { " +
			"exports io.inverno.test.automaticmoduledep; " +
			"uses io.inverno.test.automaticmoduledep.AutomaticMessageProvider;" +
			"}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
		
		dependencyGroup = TestProject.GROUP;
		dependencyName = "unnamed-dep";
		dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"module " + TestProject.getDependencyModuleName(dependencyName) + " { " +
			"exports io.inverno.test.unnamedmoduledep;" +
			"}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
		
		dependencyGroup = "org.webjars";
		dependencyName = "webjar-dep";
		dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"module " + TestProject.getDependencyModuleName(dependencyName) + " {}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
	}
	
	@Test
	public void testExecuteWithModuleOverrides() throws Exception {
		Map<String, Dependency> modularizedDependencies = this.project.modularizeDependencies()
			.moduleOverrides(List.of(
				new ModuleInfo(
					TestProject.getDependencyModuleName("automatic-module-dep"), 
					true, 
					null, 
					null, 
					null,
					null,
					List.of(new ModuleInfo.UsesDirective("io.inverno.test.automaticmoduledep.AutomaticMessageProvider")), 
					null
				)
			))
			.execute().stream()
			.collect(Collectors.toMap(Dependency::getName, Function.identity()));
		
		String dependencyGroup = TestProject.GROUP;
		String dependencyName = "module-dep";
		Assertions.assertTrue(Files.exists(TestProject.WORKING_PATH.resolve(TestProject.getDependencyModulePath(dependencyName))));
		
		Assertions.assertEquals(3, modularizedDependencies.size());
		
		dependencyGroup = TestProject.GROUP;
		dependencyName = "automatic-module-dep";
		Dependency dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertFalse(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		Path moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"open module " + TestProject.getDependencyModuleName(dependencyName) + "{" +
			"exports io.inverno.test.automaticmoduledep;" +
			"uses io.inverno.test.automaticmoduledep.AutomaticMessageProvider;" +
			"}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
		
		dependencyGroup = TestProject.GROUP;
		dependencyName = "unnamed-dep";
		dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"module " + TestProject.getDependencyModuleName(dependencyName) + " { " +
			"exports io.inverno.test.unnamedmoduledep;" +
			"}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
		
		dependencyGroup = "org.webjars";
		dependencyName = "webjar-dep";
		dependency = modularizedDependencies.get(dependencyName);
		
		Assertions.assertEquals(dependencyGroup, dependency.getGroup());
		Assertions.assertEquals(dependencyName, dependency.getName());
		Assertions.assertEquals(TestProject.getDependencyVersion(dependencyName), dependency.getVersion());
		
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModuleUnnamedPath(dependencyName)));
		moduleInfoSourcePath = TestProject.getDependencyModuleExplodedPath(dependencyName).resolve("module-info.java");
		Assertions.assertTrue(Files.exists(moduleInfoSourcePath));
		
		Assertions.assertEquals(
			"module " + TestProject.getDependencyModuleName(dependencyName) + " {}",
			Files.readString(moduleInfoSourcePath).replaceAll("[\s\t]+", " ").replaceAll("[\r\n]", "")
		);
		Assertions.assertTrue(Files.exists(TestProject.getDependencyModulePath(dependencyName)));
	}
}
