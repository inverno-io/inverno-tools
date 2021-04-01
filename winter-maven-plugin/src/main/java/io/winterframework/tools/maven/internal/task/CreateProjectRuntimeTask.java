/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.tools.maven.internal.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;

import io.winterframework.tools.maven.BuildRuntimeMojo;
import io.winterframework.tools.maven.internal.NullPrintStream;
import io.winterframework.tools.maven.internal.ProjectModule;
import io.winterframework.tools.maven.internal.Task;
import io.winterframework.tools.maven.internal.TaskExecutionException;

/**
 * @author jkuhn
 *
 */
public class CreateProjectRuntimeTask extends Task<Void> {

	private final ToolProvider jlink;
	private final ProjectModule projectModule;
	
	private Optional<String> addModules;
	
	private Optional<String> addOptions;
	
	private Optional<String> compress;
	
	private boolean bindServices;
	
	private boolean ignoreSigningInformation;
	
	private boolean stripDebug;
	
	private boolean stripNativeCommands;
	
	private Optional<String> vm;
	
	private List<BuildRuntimeMojo.Launcher> launchers;
	
	public CreateProjectRuntimeTask(AbstractMojo mojo, ToolProvider jlink, ProjectModule projectModule) {
		super(mojo);
		this.jlink = jlink;
		this.projectModule = projectModule;
	}

	@Override
	public Void call() throws TaskExecutionException {
		Path runtimeImagePath = this.projectModule.getRuntimeImagePath();
		if(this.verbose) {
			this.getLog().info("[ Creating project runtime: " + runtimeImagePath + "... ]");
		}
		if(Files.exists(runtimeImagePath)) {
			try (Stream<Path> walk = Files.walk(runtimeImagePath)) {
				for(Iterator<Path> pathIterator = walk.sorted(Comparator.reverseOrder()).iterator(); pathIterator.hasNext();) {
					Files.delete(pathIterator.next());
				}
			}
			catch (IOException e) {
				throw new TaskExecutionException("Error cleaning project runtime", e);
			}
		}
		
		List<String> jlink_args = new LinkedList<>();

		if(this.isVerbose()) {
			jlink_args.add("--verbose");
		}
		if(this.bindServices) {
			jlink_args.add("--bind-services");
		}
		if(this.ignoreSigningInformation) {
			jlink_args.add("--ignore-signing-information");
		}
		if(this.stripDebug) {
			jlink_args.add("--strip-debug");
		}
		if(this.stripNativeCommands) {
			jlink_args.add("--strip-native-commands");
		}
		this.compress.ifPresent(value -> {
			jlink_args.add("--compress=" + value);
		});
		this.vm.ifPresent(value -> {
			jlink_args.add("--vm");
			jlink_args.add(value);
		});
		this.addOptions.ifPresent(value -> {
			jlink_args.add("--add-options");
			jlink_args.add("\"" + value.replace("\"", "\\\"") + "\"");
		});
		
		String javaModulePath = Paths.get(System.getProperty("java.home"), "jmods").toAbsolutePath().toString();
		String dependenciesModulePath = this.projectModule.getModuleDependencies().stream().map(dependencyModule -> dependencyModule.getJmodPath().toString()).collect(Collectors.joining(System.getProperty("path.separator")));
		String projectModulePath = projectModule.getJmodPath().toString();
		
		String jlink_modulePath = String.join(System.getProperty("path.separator"), javaModulePath, dependenciesModulePath, projectModulePath);
		
		jlink_args.add("--module-path");
		jlink_args.add(jlink_modulePath);
		
		Set<String> modules = new HashSet<>();
		this.addModules.ifPresent(value -> {
			for(String addModule : value.split(",")) {
				modules.add(StringUtils.strip(addModule));				
			}
		});
		this.projectModule.getModuleDependencies().forEach(d -> modules.add(d.getModuleName())); // We must add current project as well...
		modules.add(this.projectModule.getModuleName());
		
		jlink_args.add("--add-modules");
		jlink_args.add(modules.stream().collect(Collectors.joining(",")));
		
		jlink_args.add("--limit-modules");
		jlink_args.add(modules.stream().collect(Collectors.joining(",")));
		
		if(this.launchers != null && !this.launchers.isEmpty()) {
			jlink_args.add(this.launchers.stream()
				.map(launcher -> {
					StringBuilder launcherString = new StringBuilder();
					launcherString.append("--launcher ");
					launcherString.append(launcher.getName()).append("=").append(launcher.getModule().orElse(this.projectModule.getModuleName()));
					launcher.getMainClass().ifPresent(mainClass -> launcherString.append("/").append(mainClass));
					return launcherString.toString();
				})
				.collect(Collectors.joining(" "))
			);
		}
		
		jlink_args.add("--output");
		jlink_args.add(runtimeImagePath.toString());
		
		if(this.verbose) {
			this.getLog().info(" - jlink " + jlink_args.stream().collect(Collectors.joining(" ")));			
		}
		if(this.jlink.run(this.verbose ? this.getOutStream() : new NullPrintStream(), this.getErrStream(), jlink_args.stream().toArray(String[]::new)) != 0) {
			throw new TaskExecutionException("Error creating project runtime, activate '-Dwinter.image.verbose=true' to display full log");
		}
		return null;
	}
	
	public Optional<String> getAddModules() {
		return addModules;
	}

	public void setAddModules(Optional<String> addModules) {
		this.addModules = addModules;
	}

	public Optional<String> getAddOptions() {
		return addOptions;
	}

	public void setAddOptions(Optional<String> addOptions) {
		this.addOptions = addOptions;
	}

	public Optional<String> getCompress() {
		return compress;
	}

	public void setCompress(Optional<String> compress) {
		this.compress = compress;
	}

	public boolean isIgnoreSigningInformation() {
		return ignoreSigningInformation;
	}
	
	public void setIgnoreSigningInformation(boolean ignoreSigningInformation) {
		this.ignoreSigningInformation = ignoreSigningInformation;
	}
	
	public boolean isBindServices() {
		return bindServices;
	}

	public void setBindServices(boolean bindServices) {
		this.bindServices = bindServices;
	}

	public boolean isStripDebug() {
		return stripDebug;
	}
	
	public void setStripDebug(boolean stripDebug) {
		this.stripDebug = stripDebug;
	}
	
	public boolean isStripNativeCommands() {
		return stripNativeCommands;
	}

	public void setStripNativeCommands(boolean stripNativeCommands) {
		this.stripNativeCommands = stripNativeCommands;
	}

	public Optional<String> getVm() {
		return vm;
	}

	public void setVm(Optional<String> vm) {
		this.vm = vm;
	}

	public List<BuildRuntimeMojo.Launcher> getLaunchers() {
		return launchers;
	}

	public void setLaunchers(List<BuildRuntimeMojo.Launcher> launchers) {
		this.launchers = launchers;
	}
}
