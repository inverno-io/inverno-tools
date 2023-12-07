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

import io.inverno.tool.buildtools.BuildRuntimeTask;
import io.inverno.tool.buildtools.Image;
import io.inverno.tool.buildtools.TestProject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class BuildRuntimeTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final Path PROCESS_OUTPUT_PATH = TestProject.TARGET_PATH.resolve("process.out");
	
	private TestProject project;
	
	@BeforeEach
	public void init() {
		this.project = new TestProject();
		this.project.compile();
	}
	
	@AfterEach
	public void cleanup() throws IOException {
		this.project.clean();
		Files.deleteIfExists(PROCESS_OUTPUT_PATH);
	}
	
	@Test
	public void testExecute() throws Exception {
		Image runtimeImage = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.execute();
		
		Path runtimePath = runtimeImage.getPath().get();
		
		Properties releaseProperties = new Properties();
		try(InputStream releaseInput = Files.newInputStream(runtimePath.resolve("release"))) {
			releaseProperties.load(releaseInput);
			Assertions.assertEquals("\"java.base io.inverno.test.automaticModuleDep io.inverno.test.moduleDep io.inverno.test.project io.inverno.test.unnamed.dep org.webjars.webjar.dep\"", releaseProperties.getProperty("MODULES"));
		}
		Assertions.assertFalse(Files.exists(runtimePath.resolve("bin")));
	}
	
	@Test
	public void testExecuteWithNativeCommands() throws Exception {
		Image runtimeImage = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.stripNativeCommands(false)
			.execute();
		
		Path runtimePath = runtimeImage.getPath().get();
		
		Properties releaseProperties = new Properties();
		try(InputStream releaseInput = Files.newInputStream(runtimePath.resolve("release"))) {
			releaseProperties.load(releaseInput);
			Assertions.assertEquals("\"java.base io.inverno.test.automaticModuleDep io.inverno.test.moduleDep io.inverno.test.project io.inverno.test.unnamed.dep org.webjars.webjar.dep\"", releaseProperties.getProperty("MODULES"));
		}
		Assertions.assertTrue(Files.exists(runtimePath.resolve("bin")));
	}
	
	@Test
	public void testExecuteWithConfLegalManInJmod() throws Exception {
		Image runtimeImage = this.project
			.modularizeDependencies()
			.buildJmod()
			.resolveMainClass(true)
			.configurationPath(Path.of("src/test/resources/conf").toAbsolutePath())
			.legalPath(Path.of("src/test/resources/legal").toAbsolutePath())
			.manPath(Path.of("src/test/resources/man").toAbsolutePath())
			.buildRuntime()
			.execute();
		
		Path runtimePath = runtimeImage.getPath().get();
		
		Properties releaseProperties = new Properties();
		try(InputStream releaseInput = Files.newInputStream(runtimePath.resolve("release"))) {
			releaseProperties.load(releaseInput);
			Assertions.assertEquals("\"java.base io.inverno.test.automaticModuleDep io.inverno.test.moduleDep io.inverno.test.project io.inverno.test.unnamed.dep org.webjars.webjar.dep\"", releaseProperties.getProperty("MODULES"));
		}
		Assertions.assertFalse(Files.exists(runtimePath.resolve("bin")));
		Assertions.assertTrue(Files.exists(runtimePath.resolve("conf/configuration.properties")));
		Assertions.assertTrue(Files.exists(runtimePath.resolve("man/project.1")));
		Assertions.assertTrue(Files.exists(runtimePath.resolve("legal/" + this.project.getModuleName()+ "/LICENSE")));
	}
	
	@Test
	public void testExecuteWithLauncher() throws Exception {
		Image runtimeImage = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.launchers(List.of(
				BuildRuntimeTask.Launcher.of("project", this.project.getModuleName(), "io.inverno.test.project.Main")
			))
			.execute();
		
		Path runtimePath = runtimeImage.getPath().get();
		
		Properties releaseProperties = new Properties();
		try(InputStream releaseInput = Files.newInputStream(runtimePath.resolve("release"))) {
			releaseProperties.load(releaseInput);
			Assertions.assertEquals("\"java.base io.inverno.test.automaticModuleDep io.inverno.test.moduleDep io.inverno.test.project io.inverno.test.unnamed.dep org.webjars.webjar.dep\"", releaseProperties.getProperty("MODULES"));
		}
		Assertions.assertTrue(Files.exists(runtimePath.resolve("bin/project")));
		
		ProcessBuilder pb = new ProcessBuilder(runtimePath.resolve("bin/project").toAbsolutePath().toString())
			.redirectOutput(PROCESS_OUTPUT_PATH.toFile());
		
		Process process = pb.start();
		process.waitFor();
		Assertions.assertEquals("execute module dep, execute automatic module dep, webjar module dep, execute unnamed module dep\n", Files.readString(PROCESS_OUTPUT_PATH));
	}
}
