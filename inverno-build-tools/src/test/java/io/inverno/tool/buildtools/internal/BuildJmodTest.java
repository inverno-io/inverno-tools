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

import io.inverno.tool.buildtools.TestProject;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class BuildJmodTest {

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
		Path jmodPath = this.project
			.modularizeDependencies()
			.buildJmod()
			.execute();
		
		Assertions.assertEquals(this.project.getJmodPath(), jmodPath);
		Assertions.assertTrue(Files.exists(jmodPath));

		Assertions.assertEquals(
			"io.inverno.test.project@1.0.0\n" +
			"exports io.inverno.test.project\n" +
			"requires io.inverno.test.automaticModuleDep\n" +
			"requires io.inverno.test.moduleDep\n" +
			"requires java.base mandated", 
			describeJmod(jmodPath)
		);
		
		Assertions.assertEquals(
			"classes/module-info.class\n" +
			"classes/io/inverno/test/project/Main.class",
			listJmod(jmodPath)
		);
	}
	
	@Test
	public void testExecuteWithResolveMainClass() throws Exception {
		Path jmodPath = this.project
			.modularizeDependencies()
			.buildJmod()
			.resolveMainClass(true)
			.execute();
		
		Assertions.assertEquals(this.project.getJmodPath(), jmodPath);
		Assertions.assertTrue(Files.exists(jmodPath));

		Assertions.assertEquals(
			"io.inverno.test.project@1.0.0\n" +
			"exports io.inverno.test.project\n" +
			"requires io.inverno.test.automaticModuleDep\n" +
			"requires io.inverno.test.moduleDep\n" +
			"requires java.base mandated\n" +
			"main-class io.inverno.test.project.Main", 
			describeJmod(jmodPath)	
		);
		
		Assertions.assertEquals(
			"classes/module-info.class\n" +
			"classes/io/inverno/test/project/Main.class",
			listJmod(jmodPath)
		);
	}
	
	@Test
	public void testExecuteWithMainClass() throws Exception {
		Path jmodPath = this.project
			.modularizeDependencies()
			.buildJmod()
			.mainClass("io.inverno.test.project.Main")
			.execute();
		
		Assertions.assertEquals(this.project.getJmodPath(), jmodPath);
		Assertions.assertTrue(Files.exists(jmodPath));

		Assertions.assertEquals(
			"io.inverno.test.project@1.0.0\n" +
			"exports io.inverno.test.project\n" +
			"requires io.inverno.test.automaticModuleDep\n" +
			"requires io.inverno.test.moduleDep\n" +
			"requires java.base mandated\n" +
			"main-class io.inverno.test.project.Main", 
			describeJmod(jmodPath)	
		);
		
		Assertions.assertEquals(
			"classes/module-info.class\n" +
			"classes/io/inverno/test/project/Main.class",
			listJmod(jmodPath)
		);
	}
	
	@Test
	public void testExecuteWithLegalPath() throws Exception {
		Path jmodPath = this.project
			.modularizeDependencies()
			.buildJmod()
			.legalPath(Path.of("src/test/resources/legal").toAbsolutePath())
			.execute();
		
		Assertions.assertEquals(this.project.getJmodPath(), jmodPath);
		Assertions.assertTrue(Files.exists(jmodPath));

		Assertions.assertEquals(
			"io.inverno.test.project@1.0.0\n" +
			"exports io.inverno.test.project\n" +
			"requires io.inverno.test.automaticModuleDep\n" +
			"requires io.inverno.test.moduleDep\n" +
			"requires java.base mandated",
			describeJmod(jmodPath)	
		);
		
		Assertions.assertEquals(
			"classes/module-info.class\n" +
			"classes/io/inverno/test/project/Main.class\n" +
			"legal/LICENSE",
			listJmod(jmodPath)
		);
	}
	
	@Test
	public void testExecuteWithManPath() throws Exception {
		Path jmodPath = this.project
			.modularizeDependencies()
			.buildJmod()
			.manPath(Path.of("src/test/resources/man").toAbsolutePath())
			.execute();
		
		Assertions.assertEquals(this.project.getJmodPath(), jmodPath);
		Assertions.assertTrue(Files.exists(jmodPath));

		Assertions.assertEquals(
			"io.inverno.test.project@1.0.0\n" +
			"exports io.inverno.test.project\n" +
			"requires io.inverno.test.automaticModuleDep\n" +
			"requires io.inverno.test.moduleDep\n" +
			"requires java.base mandated",
			describeJmod(jmodPath)	
		);
		
		Assertions.assertEquals(
			"classes/module-info.class\n" +
			"classes/io/inverno/test/project/Main.class\n" +
			"man/project.1",
			listJmod(jmodPath)
		);
	}
	
	@Test
	public void testExecuteWithConfigurationPath() throws Exception {
		Path jmodPath = this.project
			.modularizeDependencies()
			.buildJmod()
			.configurationPath(Path.of("src/test/resources/conf").toAbsolutePath())
			.execute();
		
		Assertions.assertEquals(this.project.getJmodPath(), jmodPath);
		Assertions.assertTrue(Files.exists(jmodPath));

		Assertions.assertEquals(
			"io.inverno.test.project@1.0.0\n" +
			"exports io.inverno.test.project\n" +
			"requires io.inverno.test.automaticModuleDep\n" +
			"requires io.inverno.test.moduleDep\n" +
			"requires java.base mandated",
			describeJmod(jmodPath)	
		);
		
		Assertions.assertEquals(
			"classes/module-info.class\n" +
			"classes/io/inverno/test/project/Main.class\n" +
			"conf/configuration.properties",
			listJmod(jmodPath)
		);
	}
	
	private static String describeJmod(Path jmodPath) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintWriter outputWriter = new PrintWriter(output);
		if(JavaTools.JMOD.run(outputWriter, outputWriter, "describe", jmodPath.toString()) != 0) {
			Assertions.fail("Error describing jmod: " + new String(output.toByteArray()));
		}
		return new String(output.toByteArray()).trim();
	}
	
	private static String listJmod(Path jmodPath) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintWriter outputWriter = new PrintWriter(output);
		if(JavaTools.JMOD.run(outputWriter, outputWriter, "list", jmodPath.toString()) != 0) {
			Assertions.fail("Error listing jmod: " + new String(output.toByteArray()));
		}
		return new String(output.toByteArray()).trim();
	}
}
