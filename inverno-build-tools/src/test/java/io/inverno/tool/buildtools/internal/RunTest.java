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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class RunTest {

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
	public void cleanup() throws IOException {
		this.project.clean();
	}
	
	@Test
	public void testExecute() throws Exception {
		Integer exitCode = this.project
			.modularizeDependencies()
			.run()
			.execute();
		
		Assertions.assertEquals(0, exitCode);
	}
	
	@Test
	public void testExecuteWithRedirectOutput() throws Exception {
		Path outputPath = Files.createTempFile("test-", ".out");
		
		Integer exitCode = this.project
			.modularizeDependencies()
			.run()
			.redirectOutput(ProcessBuilder.Redirect.to(outputPath.toFile()))
			.execute();
		
		Assertions.assertEquals(0, exitCode);
		Assertions.assertTrue(Files.exists(outputPath));
		
		Assertions.assertEquals("execute module dep, execute automatic module dep, webjar module dep, execute unnamed module dep" + System.lineSeparator(), Files.readString(outputPath));
	}
	
	@Test
	public void testExecuteWithArgs() throws Exception {
		Path outputPath = Files.createTempFile("test-", ".out");
		
		Integer exitCode = this.project
			.modularizeDependencies()
			.run()
			.redirectOutput(ProcessBuilder.Redirect.to(outputPath.toFile()))
			.arguments("arg1 arg2")
			.execute();
		
		Assertions.assertEquals(0, exitCode);
		Assertions.assertTrue(Files.exists(outputPath));
		
		Assertions.assertEquals("execute module dep, execute automatic module dep, webjar module dep, execute unnamed module dep, arg1, arg2" + System.lineSeparator(), Files.readString(outputPath));
	}
	
	@Test
	public void testExecuteWithVmOptions() throws Exception {
		Path outputPath = Files.createTempFile("test-", ".out");
		
		Integer exitCode = this.project
			.modularizeDependencies()
			.run()
			.redirectOutput(ProcessBuilder.Redirect.to(outputPath.toFile()))
			.vmOptions("-Dinverno.test.property=property")
			.execute();
		
		Assertions.assertEquals(0, exitCode);
		Assertions.assertTrue(Files.exists(outputPath));
		
		Assertions.assertEquals("execute module dep, execute automatic module dep, webjar module dep, execute unnamed module dep, property" + System.lineSeparator(), Files.readString(outputPath));
	}
}
