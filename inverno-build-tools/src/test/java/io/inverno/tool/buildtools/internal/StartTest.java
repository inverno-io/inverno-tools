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

import io.inverno.tool.buildtools.TaskExecutionException;
import io.inverno.tool.buildtools.TestProject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Optional;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class StartTest {

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
		Path outputPath = Files.createTempFile("test-", ".out");
		Path inputPath = Files.createTempFile("test-", ".in");
		Path pidFile = this.project.getPidfile();
		
		Long pid = this.project
			.modularizeDependencies()
			.start()
			.vmOptions("-Dinverno.test.pidfile=" + pidFile.toString() + " -Dinverno.test.block=true")
			.redirectInput(ProcessBuilder.Redirect.from(inputPath.toFile()))
			.redirectOutput(ProcessBuilder.Redirect.to(outputPath.toFile()))
			.execute();

		Optional<ProcessHandle> processHandle = ProcessHandle.of(pid);
		Assertions.assertTrue(processHandle.isPresent());
		Assertions.assertTrue(processHandle.get().isAlive());
		
		Assertions.assertTrue(Files.exists(pidFile));
		Assertions.assertEquals(Long.parseLong(Files.readString(pidFile)), pid);
		
		// Should terminate app
		Files.write(inputPath, new byte[] {123}, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		
		Awaitility.await()
			.atMost(Duration.ofSeconds(2))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				Assertions.assertFalse(ProcessHandle.of(pid).isPresent());
			});
		
		Assertions.assertTrue(Files.exists(outputPath));
		Assertions.assertEquals("execute module dep, execute automatic module dep, webjar module dep, execute unnamed module dep" + System.lineSeparator(), Files.readString(outputPath));
	}
	
	@Test
	public void testExecuteWithArgs() throws Exception {
		Path outputPath = Files.createTempFile("test-", ".out");
		Path inputPath = Files.createTempFile("test-", ".in");
		Path pidFile = this.project.getPidfile();
		
		Long pid = this.project
			.modularizeDependencies()
			.start()
			.vmOptions("-Dinverno.test.pidfile=" + pidFile.toString() + " -Dinverno.test.block=true")
			.redirectInput(ProcessBuilder.Redirect.from(inputPath.toFile()))
			.redirectOutput(ProcessBuilder.Redirect.to(outputPath.toFile()))
			.arguments("arg1 arg2")
			.execute();

		Optional<ProcessHandle> processHandle = ProcessHandle.of(pid);
		Assertions.assertTrue(processHandle.isPresent());
		Assertions.assertTrue(processHandle.get().isAlive());
		
		Assertions.assertTrue(Files.exists(pidFile));
		Assertions.assertEquals(Long.parseLong(Files.readString(pidFile)), pid);
		
		// Should terminate app
		Files.write(inputPath, new byte[] {123}, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		
		Awaitility.await()
			.atMost(Duration.ofSeconds(2))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				Assertions.assertFalse(ProcessHandle.of(pid).isPresent());
			});
		
		Assertions.assertTrue(Files.exists(outputPath));
		Assertions.assertEquals("execute module dep, execute automatic module dep, webjar module dep, execute unnamed module dep, arg1, arg2" + System.lineSeparator(), Files.readString(outputPath));
	}
	
	@Test
	public void testExecuteTimeout() throws Exception {
		Awaitility.await()
			.atLeast(Duration.ofMillis(1000l))
			// before starting the app we have modularization of dependencies which takes some time as well 
			.atMost(Duration.ofMillis(2000l))
			.untilAsserted(() -> {
				Assertions.assertEquals(
					"Application startup timeout exceeded",
					Assertions.assertThrows(
						TaskExecutionException.class,
						() -> {
							this.project
								.modularizeDependencies()
								.start()
								.vmOptions("-Dinverno.test.block=true")
								.timeout(1000l)
								.execute();
						}
					).getMessage()
				);
			});
	}
}
