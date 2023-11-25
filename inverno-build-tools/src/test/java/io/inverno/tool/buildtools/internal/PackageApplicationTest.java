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

import io.inverno.tool.buildtools.PackageApplicationTask;
import io.inverno.tool.buildtools.TestProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;


/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PackageApplicationTest {

	static {
		System.setProperty("log4j2.simplelogLevel", "INFO");
		System.setProperty("log4j2.simplelogLogFile", "system.out");
	}
	
	private static final Path PROCESS_OUTPUT_PATH = TestProject.TARGET_PATH.resolve("process.out");
	
	private TestProject project;
	
	@BeforeEach
	public void init() {
		this.project = new TestProject();
		this.project.compile();
	}
	
	@AfterEach
	public void cleanup() throws Exception {
		this.project.clean();
		Files.deleteIfExists(PROCESS_OUTPUT_PATH);
	}
	
	@Test
	public void testExecute() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.execute();
		
		Path applicationPath = this.project.getApplicationPath(null);
		
		Assertions.assertEquals(
			Set.of(applicationPath),
			applicationPaths
		);
		
		Assertions.assertTrue(Files.exists(applicationPath));
		
		ProcessBuilder pb = new ProcessBuilder(applicationPath.resolve("bin/project").toAbsolutePath().toString())
			.redirectOutput(PROCESS_OUTPUT_PATH.toFile());
		
		Process process = pb.start();
		process.waitFor();
		Assertions.assertEquals("execute module dep, execute automatic module dep, webjar module dep, execute unnamed module dep\n", Files.readString(PROCESS_OUTPUT_PATH));
	}
	
	@Test
	public void testExecuteWithGenericOptions() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.copyright("Copyright 2023 Jeremy KUHN")
			.vendor("Inverno Framework")
			//.aboutURL(URI.create("https://inverno.io")) // requires JDK>15
			.licensePath(Path.of("src/test/resources/legal/LICENSE"))
			.execute();
		
		Assertions.assertEquals(
			Set.of(this.project.getApplicationPath(null)),
			applicationPaths
		);
		
		applicationPaths.forEach(applicationPath -> Assertions.assertTrue(Files.exists(applicationPath)));
		
		// TODO it seems these options don't do anything at least when generating the app-image
	}
	
	@Test
	public void testExecuteWithZipArchiveFormat() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.archive()
			.formats(Set.of("zip"))
			.execute();
		
		Assertions.assertEquals(
			Set.of(this.project.getApplicationPath("zip")),
			applicationPaths
		);
		
		applicationPaths.forEach(applicationPath -> Assertions.assertTrue(Files.exists(applicationPath)));
	}
	
	@Test
	public void testExecuteWithLauncher() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.automaticLaunchers(false)
			.launchers(List.of(new PackageApplicationTask.Launcher() {
					
					@Override
					public boolean isAddUnnamedModules() {
						return true;
					}
					
					@Override
					public Optional<String> getVmOptions() {
						return Optional.empty();
					}
					
					@Override
					public Optional<String> getName() {
						return Optional.of("test-project");
					}
					
					@Override
					public Optional<String> getDescription() {
						return Optional.of("This is a test application");
					};
					
					@Override
					public Optional<String> getModule() {
						return Optional.of("io.inverno.test.project");
					}
					
					@Override
					public Optional<String> getMainClass() {
						return Optional.of("io.inverno.test.project.Main");
					}
					
					@Override
					public Optional<Path> getIconPath() {
						return Optional.of(Path.of("src/test/resources/inverno_favicon.png"));
					}
					
					@Override
					public Optional<String> getArguments() {
						return Optional.of("args");
					}
					
					@Override
					public Optional<String> getAppVersion() {
						return Optional.of("1.2.3");
					}

					@Override
					public boolean isLauncherAsService() {
						return false;
					}

					@Override
					public boolean isWinConsole() {
						return false;
					}

					@Override
					public boolean isWinShortcut() {
						return false;
					}

					@Override
					public boolean isWinMenu() {
						return false;
					}

					@Override
					public Optional<String> getLinuxAppCategory() {
						return null;
					}

					@Override
					public boolean isLinuxShortcut() {
						return false;
					}
				}
			))
			.execute();
		
		Path applicationPath = this.project.getApplicationPath(null);
		
		Assertions.assertEquals(
			Set.of(
				applicationPath
			),
			applicationPaths
		);
		
		Assertions.assertTrue(Files.exists(applicationPath));
		Assertions.assertTrue(Files.exists(applicationPath.resolve("bin/test-project")));
		Assertions.assertTrue(Files.exists(applicationPath.resolve("lib/test-project.png")));

		Assertions.assertArrayEquals(
			Files.readAllBytes(Path.of("src/test/resources/inverno_favicon.png")),
			Files.readAllBytes(applicationPath.resolve("lib/test-project.png"))
		);
		
		Properties projectProperties = new Properties();
		projectProperties.load(Files.newInputStream(applicationPath.resolve("lib/app/test-project.cfg")));
		
		Assertions.assertEquals("io.inverno.test.project/io.inverno.test.project.Main", projectProperties.get("app.mainmodule"));
	}
	
	@Test
	@EnabledOnOs({OS.LINUX})
	public void testExecuteWithDebArchiveFormat() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.copyright("Copyright 2023 Jeremy KUHN")
			.vendor("Inverno Framework")
			//.aboutURL(URI.create("https://inverno.io")) // requires JDK>15
			.licensePath(Path.of("src/test/resources/legal/LICENSE"))
			.linuxConfiguration(new PackageApplicationTask.LinuxConfiguration() {
				
				@Override
				public boolean isShortcut() {
					return true;
				}
				
				@Override
				public Optional<String> getRpmLicenseType() {
					return Optional.empty();
				}
				
				@Override
				public Optional<String> getPackageName() {
					return Optional.of("inverno-test-project");
				}
				
				@Override
				public Optional<String> getPackageDeps() {
					return Optional.empty();
				}
				
				@Override
				public Optional<String> getMenuGroup() {
					return Optional.of("test");
				}
				
				@Override
				public Optional<String> getDebMaintainer() {
					return Optional.of("Jeremy Kuhn <jeremy.kuhn@inverno.io>");
				}
				
				@Override
				public Optional<String> getAppRelease() {
					return Optional.of("42");
				}
				
				@Override
				public Optional<String> getAppCategory() {
					return Optional.of("test");
				}
			})
			.types(Set.of(PackageApplicationTask.PackageType.DEB))
			.execute();
		
		Assertions.assertEquals(
			Set.of(
				this.project.getApplicationPath(null),
				this.project.getApplicationPath("deb")
			),
			applicationPaths
		);
		
		applicationPaths.forEach(applicationPath -> Assertions.assertTrue(Files.exists(applicationPath)));
		
		try(ArchiveInputStream debInputStream = new ArArchiveInputStream(Files.newInputStream(this.project.getApplicationPath("deb")))) {
			ArchiveEntry debEntry = null;
			while((debEntry = debInputStream.getNextEntry()) != null) {
				switch(debEntry.getName()) {
					case "control.tar.xz": {
						// we need to untar then xz
						ArchiveInputStream controlInputStream = new TarArchiveInputStream(new XZCompressorInputStream(debInputStream));
						ArchiveEntry controlEntry = null;
						while((controlEntry = controlInputStream.getNextEntry()) != null) {
							if("control".equals(controlEntry.getName())) {
								Properties controlProperties = new Properties();
								controlProperties.load(controlInputStream);
								
								Assertions.assertEquals("inverno-test-project", controlProperties.get("Package"));
								Assertions.assertEquals(this.project.getVersion() + "-42", controlProperties.get("Version"));
								Assertions.assertEquals("test", controlProperties.get("Section"));
								Assertions.assertEquals("Jeremy Kuhn <jeremy.kuhn@inverno.io>", controlProperties.get("Maintainer"));
								Assertions.assertEquals("This is a test application", controlProperties.get("Description"));
								
								break;
							}
						}
						
						break;
					}
					case "data.tar.xz": {
						ArchiveInputStream dataInputStream = new TarArchiveInputStream(new XZCompressorInputStream(debInputStream));
						ArchiveEntry dataEntry = null;
						boolean hasCopyright = false;
						boolean hasProjectCfg = false;
						boolean hasShortcut = false;
						while((dataEntry = dataInputStream.getNextEntry()) != null) {
							switch(dataEntry.getName()) {
								case "./opt/inverno-test-project/share/doc/copyright": {
									hasCopyright = true;
									Properties controlProperties = new Properties();
									controlProperties.load(dataInputStream);
									
									Assertions.assertEquals("Copyright 2023 Jeremy KUHN", controlProperties.get("Copyright"));
									Assertions.assertEquals("TEST LICENSE", controlProperties.get("License"));
									break;
								}
								case "./opt/inverno-test-project/lib/app/project.cfg": {
									hasProjectCfg = true;
									Properties projectProperties = new Properties();
									projectProperties.load(dataInputStream);
									
									Assertions.assertEquals("io.inverno.test.project/io.inverno.test.project.Main", projectProperties.get("app.mainmodule"));
									break;
								}
								case "./opt/inverno-test-project/lib/inverno-test-project-project.desktop": {
									hasShortcut = true;
									Properties shortcutProperties = new Properties();
									shortcutProperties.load(dataInputStream);
									
									Assertions.assertEquals("project", shortcutProperties.get("Name"));
									Assertions.assertEquals("project", shortcutProperties.get("Comment"));
									Assertions.assertEquals("/opt/inverno-test-project/bin/project", shortcutProperties.get("Exec"));
									Assertions.assertEquals("/opt/inverno-test-project/lib/project.png", shortcutProperties.get("Icon"));
									Assertions.assertEquals("false", shortcutProperties.get("Terminal"));
									Assertions.assertEquals("Application", shortcutProperties.get("Type"));
									Assertions.assertEquals("test", shortcutProperties.get("Categories"));
									break;
								}
							}
						}
						
						Assertions.assertTrue(hasCopyright, "copyright is missing in .deb package");
						Assertions.assertTrue(hasProjectCfg, "project.cfg is missing in .deb package");
						Assertions.assertTrue(hasShortcut, "inverno-test-project-project.desktop is missing in .deb package");
						
						break;
					}
				}
		    }
		}
	}
	
	@Test
	@EnabledOnOs({OS.WINDOWS})
	public void testExecuteWithExeArchiveFormat() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.copyright("Copyright 2023 Jeremy KUHN")
			.vendor("Inverno Framework")
			//.aboutURL(URI.create("https://inverno.io")) // requires JDK>15
			.licensePath(Path.of("src/test/resources/legal/LICENSE"))
			.types(Set.of(PackageApplicationTask.PackageType.EXE))
			.execute();
		
		Assertions.assertEquals(
			Set.of(
				this.project.getApplicationPath("exe")
			),
			applicationPaths
		);
		
		applicationPaths.forEach(applicationPath -> Assertions.assertTrue(Files.exists(applicationPath)));
	}
	
	@Test
	@EnabledOnOs({OS.WINDOWS})
	public void testExecuteWithMsiArchiveFormat() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.copyright("Copyright 2023 Jeremy KUHN")
			.vendor("Inverno Framework")
			//.aboutURL(URI.create("https://inverno.io")) // requires JDK>15
			.licensePath(Path.of("src/test/resources/legal/LICENSE"))
			.types(Set.of(PackageApplicationTask.PackageType.MSI))
			.execute();
		
		Assertions.assertEquals(
			Set.of(
				this.project.getApplicationPath("msi")
			),
			applicationPaths
		);
		
		applicationPaths.forEach(applicationPath -> Assertions.assertTrue(Files.exists(applicationPath)));
	}
	
	@Test
	@EnabledOnOs({OS.MAC})
	public void testExecuteWithDmgArchiveFormat() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.copyright("Copyright 2023 Jeremy KUHN")
			.vendor("Inverno Framework")
			//.aboutURL(URI.create("https://inverno.io")) // requires JDK>15
			.licensePath(Path.of("src/test/resources/legal/LICENSE"))
			.types(Set.of(PackageApplicationTask.PackageType.DMG))
			.execute();
		
		Assertions.assertEquals(
			Set.of(
				this.project.getApplicationPath("msi")
			),
			applicationPaths
		);
		
		applicationPaths.forEach(applicationPath -> Assertions.assertTrue(Files.exists(applicationPath)));
	}
	
	@Test
	@EnabledOnOs({OS.MAC})
	public void testExecuteWithPkgArchiveFormat() throws Exception {
		Set<Path> applicationPaths = this.project
			.modularizeDependencies()
			.buildJmod()
			.buildRuntime()
			.packageApplication()
			.copyright("Copyright 2023 Jeremy KUHN")
			.vendor("Inverno Framework")
			//.aboutURL(URI.create("https://inverno.io")) // requires JDK>15
			.licensePath(Path.of("src/test/resources/legal/LICENSE"))
			.types(Set.of(PackageApplicationTask.PackageType.PKG))
			.execute();
		
		Assertions.assertEquals(
			Set.of(
				this.project.getApplicationPath("pkg")
			),
			applicationPaths
		);
		
		applicationPaths.forEach(applicationPath -> Assertions.assertTrue(Files.exists(applicationPath)));
	}
}
