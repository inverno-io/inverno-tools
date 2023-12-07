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

import io.inverno.tool.buildtools.Image;
import io.inverno.tool.buildtools.TestProject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ArchiveTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final Set<String> FORMATS = Set.of("zip", "tar", "txz", "tar.gz");
	
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
		Set<Image> archiveImages = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.archive()
			.formats(FORMATS)
			.execute();
		
		Assertions.assertEquals(
			FORMATS.stream().map(this.project::getArchivePath).collect(Collectors.toSet()), 
			archiveImages.stream().map(image -> image.getPath().get()).collect(Collectors.toSet())
		);
		
		Path zipArchivePath = archiveImages.stream().map(image -> image.getPath().get()).filter(p -> p.getFileName().toString().endsWith(".zip")).findFirst().get();

		Set<String> archiveFileNames = new HashSet<>();
		try (ZipFile zipFile = new ZipFile(zipArchivePath.toFile())) {
		    Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		    while (zipEntries.hasMoreElements()) {
		    	archiveFileNames.add(zipEntries.nextElement().getName());
		    }
		}
		
		String prefix = this.project.getFinalName();
		int runtimePathIndex = this.project.getRuntimePath().toString().length() + 1;
		
		Set<String> expectedArchiveFileNames = new HashSet<>();
		try (Stream<Path> stream = Files.walk(this.project.getRuntimePath())) {
			expectedArchiveFileNames = stream
				.map(path -> {
					if(path.equals(this.project.getRuntimePath())) {
						return prefix + "/";
					}
					else if(Files.isDirectory(path)) {
						return prefix + "/" + path.toString().substring(runtimePathIndex) + "/";
					}
					return prefix + "/" + path.toString().substring(runtimePathIndex);
				})
				.collect(Collectors.toSet());
	    }
		Assertions.assertEquals(expectedArchiveFileNames, archiveFileNames);
	}
	
	@Test
	public void testExecuteWithPrefix() throws Exception {
		String prefix = "somePrefix";
		Set<Image> archiveImages = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.archive()
			.prefix(prefix)
			.formats(Set.of("zip"))
			.execute();
		
		Assertions.assertEquals(
			Set.of(this.project.getArchivePath("zip")),
			archiveImages.stream().map(image -> image.getPath().get()).collect(Collectors.toSet())
		);
		
		Path zipArchivePath = archiveImages.stream().map(image -> image.getPath().get()).iterator().next();

		Set<String> archiveFileNames = new HashSet<>();
		try (ZipFile zipFile = new ZipFile(zipArchivePath.toFile())) {
		    Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		    while (zipEntries.hasMoreElements()) {
		    	archiveFileNames.add(zipEntries.nextElement().getName());
		    }
		}
		
		int runtimePathIndex = this.project.getRuntimePath().toString().length() + 1;
		
		Set<String> expectedArchiveFileNames = new HashSet<>();
		try (Stream<Path> stream = Files.walk(this.project.getRuntimePath())) {
			expectedArchiveFileNames = stream
				.map(path -> {
					if(path.equals(this.project.getRuntimePath())) {
						return prefix + "/";
					}
					else if(Files.isDirectory(path)) {
						return prefix + "/" + path.toString().substring(runtimePathIndex) + "/";
					}
					return prefix + "/" + path.toString().substring(runtimePathIndex);
				})
				.collect(Collectors.toSet());
	    }
		Assertions.assertEquals(expectedArchiveFileNames, archiveFileNames);
	}
}
