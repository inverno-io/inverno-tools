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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.tool.buildtools.ContainerizeTask.Format;
import io.inverno.tool.buildtools.ContainerizeTask;
import io.inverno.tool.buildtools.TestProject;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ContainerizeTest {

	static {
		System.setProperty("log4j2.simplelogLevel", "INFO");
		System.setProperty("log4j2.simplelogLogFile", "system.out");
	}
	
	private TestProject project;
	
	@BeforeEach
	public void init() {
		this.project = new TestProject();
		this.project.compile();
	}
	
	@AfterEach
	public void cleanup() throws Exception {
		this.project.clean();
	}
	
	@Test
	public void testExecute() throws Exception {
		ContainerizeTask.ContainerImage image = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.containerize()
			.from(Platform.getSystemPlatform() == Platform.WINDOWS ? "mcr.microsoft.com/windows/nanoserver:ltsc2022" : "debian:stable-slim")
			.execute();
		
		Assertions.assertEquals(this.project.getName() + ":" + this.project.getVersion(), image.getCanonicalName());
		Assertions.assertTrue(image.getPath().isPresent());
		Assertions.assertEquals(this.project.getContainerImagePath(), image.getPath().get());
		Assertions.assertTrue(Files.exists(this.project.getContainerImagePath()));
		
		try(ArchiveInputStream tarInputStream = new TarArchiveInputStream(Files.newInputStream(this.project.getContainerImagePath()))) {
			ObjectMapper mapper = new ObjectMapper();
			ArchiveEntry tarEntry = null;
			while((tarEntry = tarInputStream.getNextEntry()) != null) {
				switch(tarEntry.getName()) {
					case "index.json": {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						IOUtils.copy(tarInputStream, bout);
						JsonNode manifests = mapper.readTree(bout.toByteArray()).get("manifests");
						Assertions.assertEquals(this.project.getName() + ":" + this.project.getVersion(), manifests.get(0).get("annotations").get("org.opencontainers.image.ref.name").textValue());
						break;
					}
				}
			}
		}
	}
	
	@Test
	public void testExecuteWithDockerFormat() throws Exception {
		ContainerizeTask.ContainerImage image = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.containerize()
			.from(Platform.getSystemPlatform() == Platform.WINDOWS ? "mcr.microsoft.com/windows/nanoserver:ltsc2022" : "debian:stable-slim")
			.format(Format.Docker)
			.execute();
		
		Assertions.assertEquals(this.project.getName() + ":" + this.project.getVersion(), image.getCanonicalName());
		Assertions.assertTrue(image.getPath().isPresent());
		Assertions.assertEquals(this.project.getContainerImagePath(), image.getPath().get());
		Assertions.assertTrue(Files.exists(this.project.getContainerImagePath()));
		
		try(ArchiveInputStream tarInputStream = new TarArchiveInputStream(Files.newInputStream(this.project.getContainerImagePath()))) {
			ObjectMapper mapper = new ObjectMapper();
			ArchiveEntry tarEntry = null;
			while((tarEntry = tarInputStream.getNextEntry()) != null) {
				switch(tarEntry.getName()) {
					case "config.json": {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						IOUtils.copy(tarInputStream, bout);
						JsonNode config = mapper.readTree(bout.toByteArray()).get("config");
						Assertions.assertEquals("/opt/" + this.project.getName() + "/" + (Platform.getSystemPlatform() == Platform.WINDOWS ? this.project.getName() + ".exe" : "bin/" + this.project.getName()), config.get("Entrypoint").get(0).textValue());
						Assertions.assertEquals("{\"8080/tcp\":{}}", config.get("ExposedPorts").toString());
						Assertions.assertEquals("/opt/" + this.project.getName(), config.get("WorkingDir").textValue());
						break;
					}
					case "manifest.json": {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						IOUtils.copy(tarInputStream, bout);
						JsonNode manifest = mapper.readTree(bout.toByteArray()).get(0);
						Assertions.assertEquals(this.project.getName() + ":" + this.project.getVersion(), manifest.get("RepoTags").get(0).textValue());
					}
				}
			}
		}
	}
}
