[jdk]: https://jdk.java.net/
[jdeps]: https://docs.oracle.com/en/java/javase/20/docs/specs/man/jdeps.html
[jmod]: https://docs.oracle.com/en/java/javase/20/docs/specs/man/jmod.html
[jlink]: https://docs.oracle.com/en/java/javase/20/docs/specs/man/jlink.html
[jpackage]: https://docs.oracle.com/en/java/javase/20/docs/specs/man/jpackage.html
[docker]: https://www.docker.com/
[oci]: https://github.com/opencontainers/image-spec/blob/master/manifest.md

# Inverno Build Tools

The Inverno Build Tools module provides an API for running, packaging and distributing Java modular applications.

A Java modular project is usually a Java module with dependencies which are not always clean Java modules (i.e. with a module descriptor). The Java ecosystem hasn't fully embraced the Java module system yet and as a result applications or libraries can still depend on automatic modules (i.e. with no module descriptor but with an `Automatic-Module-Name` entry in their `MANIFEST.MF`), which is the most common, or on unnamed modules (i.e. with no module descriptor and no `Automatic-Module-Name` entry in their `MANIFEST.MF`) which are becoming more and more rare but still exists. This situation poses mutliple problems.

An unnamed module is barely usable: because it can't be named in a deterministic way, it can't be referenced in a module descriptor. However they can be sometimes useful at runtime (e.g. WebJars) which is why it is interesting to be able to reference them in a deterministic way if only to add them to the module path.

The [JDK][jdk] now provides tools such as `jlink` or `jpackage` which can generate optimized native Java runtime with the exact dependencies required to run an application. Unfortunately these only works when an application and all its dependencies are clean Java modules.

The Inverno Build Tools module solves that issue by modularizing any automatic or unnamed dependencies defined in a Java modular project.

It also allows to create application container images that can be loaded to the local [Docker][docker] container or to a remote image registry.

The API has been designed to be easily integrated with build tools (e.g. Maven, Gradle...), as such it is not operating on the source code but on the compiled classes of a modular projects and its dependencies as JAR archives.

> As it heavily relies on tools such as [jdeps][jdeps], [jmod][jmod], [jlink][jlink] or [jpackage][jpackage] to modularize application dependencies, run and package runtimes and applications images, the module requires [JDK][jdk] 15+.

In order to use the Inverno Build Tools module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.tool.buildtools;
    ...
}
```

And also declare that dependency in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.tool</groupId>
            <artifactId>inverno-build-tools</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.tool:inverno-build-tools:${VERSION_INVERNO_TOOLS}'
...
```

## Project and Dependencies

The API defines the `Project` class and the `Dependency` interface which must be implemented in order to integrate with a build tool or more simply to invoke the tools.

The `Project` provides the group, the name and the version, which define a fully qualified name for the project, as well the path to the project's compiled classes and the set of dependencies. Just like the project, a `Dependency` is fully qualified with a group, a name and a version but unlike the project it provides the path to the dependency JAR archive.

## Build Tasks

A build `Task` represents a step in a build process which can be seen as a chain of dependent tasks executed one after the other. 

The `Project` is the entry point to create any build process which usually starts with the modularization of project dependencies. The `modularizeDependencies()` method returns a `ModularizeDependenciesTask` from which other task can be executed.

Tasks are configured and chained fluently. The `execute()` method is invoked on the final task to execute the whole build process starting with the first task and returning the final task result. Intermediary tasks results can be accessed by applying the `doOnComplete()` method on the tasks.

This example shows how to build a project server runtime while accessing the project Jmod's path:

```java
Project project = ... 
project.modularizeDependencies()
	.buildJmod()
		.mainClass("io.inverno.example.Main")
		.doOnComplete(jmodPath -> {
			// Do something usefull with the Jmod
		})
	.buildRuntime()
		.vm("server")
	.execute();
```

The following graph of tasks shows all the possible paths:

```plaintext
Project
├── modularizeDependencies()
│   ├── start()
│   ├── run()
│   └── buildJmod()
│       └── buildRuntime()
│           ├── archive()
│           └── packageApplication()
│               ├── archive()
│               └── containerize()
└── stop()
```

### ModularizeDependenciesTask

The `ModularizeDependenciesTask` is the first task in any build process, it scans all automatic and unnamed module dependencies, generates `module-info.java` descriptors for each of them, compile those descriptors and finally repackages the dependencies as clean Java module JAR archives. Subsequent tasks will rely on these modularized dependencies to build application runtime or package the application.

It returns the paths to the modularized dependencies JAR archives:

```java
Project project = ... 
Set<Path> modularizedJars = project
	.modularizeDependencies()
	.execute();
```

Modules descriptors are generated using JDK's [jdeps][jdeps] command which basically analyzes classes in the original JAR archive but this process is not always accurate, especially for modules using reflection or services. Such use cases usually result in errors when the generated descriptor is compiled. The task provides two ways to overcome this issue:

It is possible to provide clean descriptors explicitly for specific modules in such situations bypassing the descriptor generation. In the following code, the `moduleOverridesPath()` specifies the path to overriding module descriptors `[moduleName]/module-info.java`:

```java
Project project = ... 
Set<Path> modularizedJars = project
	.modularizeDependencies()
		.moduleOverridesPath(Path.of("path/to/modules/descriptors"))
	.execute();
```

Another way it to let the generation goes through and provide specific directive overrides to fix/complete the generation. In the following example, the directives provided for `io.inverno.example.SampleModule` are merged with the generated descriptor:

```java
Project project = ... 
Set<Path> modularizedJars = project
	.modularizeDependencies()
		.moduleOverrides(List.of(
			new ModuleInfo(
				"io.inverno.example.SampleModule",                                       // module name
				false,                                                                   // open module
				null,                                                                    // imports directives
				null,                                                                    // requires directives
				null,                                                                    // exports directives
				null,                                                                    // opens directives
				List.of(new ModuleInfo.UsesDirective("io.inverno.example.SomeService")), // uses directives
				null                                                                     // provides directives
			)
		))
	.execute();
```

### RunTask

The `RunTask` is chained after the `ModularizeDependenciesTask`, it allows to run the project application in a forked JVM. The project module must define a main class (i.e. a class with a `main()` method).

The following example runs the project, launching the module's default main class which is automatically resolved:

```java
Project project = ... 
project
	.modularizeDependencies()
	.run()
	.execute();
```

The `execute()` method blocks the invoking thread as it waits for the project application to terminate.

If more than one main class is defined in the module, the main class to launch must be specified explicitly, otherwise the task execution will fail:

```java
Project project = ... 
project
	.modularizeDependencies()
	.run()
		.mainClass("io.inverno.example.MainClassToLaunch")
	.execute();
```

Arguments, VM options and/or the application working path can be specified as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.run()
		.workingPath(Path.of("path/to/working"))
		.vmOptions("-DsomeProperty=1234")
		.arguments("arg1 arg2")
	.execute();
```

> Althouh it is always possible to run an application with a mix of modular and non-modular dependencies, the advantage of running the application with modularized dependencie is that it only uses the module path and non-modular dependencies are no longer grouped into the `ALL-UNNAMED` module, this fully embraces the Java module system.

### DebugTask

The `DebugTask` is chained after the `ModularizeDependenciesTask`, it is identical to the `RunTask`, the onyl difference being that it adds debugging VM options to be able to attach a debugger to the process.

The following example runs the project in debug mode and waiting for a debugger to be attached on port 8000:

```java
Project project = ... 
project
	.modularizeDependencies()
	.debug()
	.execute();
```

The debug port, whether to suspend the execution until a debugger is attached, as well as any `RunTask` options can be specified as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.debug()
		.port(9000)
		.suspend(false)
		.mainClass("io.inverno.example.MainClassToLaunch")
		.vmOptions("-DsomeProperty=1234")
	.execute();
```

### StartTask

Just like the `RunTask` the `StartTask` is chained to the `ModularizeDependenciesTask` and runs the project application in a forked VM. But unlike the `RunTask` the invoking thread doesn't wait for the application to terminate, the `execute()` returns the pid once it has determined that the application has started.

This task allows to control the project application execution and possibly interacts with the application from the invoking thread.

In order to determine when the application has started, the task expects the application to create a pid file once it is ready. If the task can't find that pid file after a specific timeout, the task terminates the forked process and raises an error. The ̀StopTask` which is used to stop the project application also uses that pid file to determine the process to which a `SIGINT` signal must be sent.

An application can be started as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.start()
	.execute();
```

By default, that task looks for the pid file in the working directory at `[WORKING_PATH]/[PROJECT_NAME].pid` and waits 60 seconds before terminating the process raise a timeout error, alternate pid file path  and timeout can be provided by configuration, as well as execution parameters such as arguments or VM options:

```java
Project project = ... 
project
	.modularizeDependencies()
	.start()
		.pidfile(Path.of("path/to/pidfile")
		.timeout(30000)
		.vmOptions("-DsomeProperty=1234")
		.arguments("arg1 arg2")
	.execute();
```

### StopTask

The `StopTask` is the only task not to be chained to the `ModularizeDependenciesTask`, it is obtained directly from the `Project` and it is used to gracefully stop a projet application started with the `StartTask`. It gets the pid of the application process to stop from the application pidfile.

If the task fails to stop gracefully the process within a given timeout, it will try to kill the process during the same timeout before raising an error.

The project application can be stopped as follows:

```java
Project project = ... 
project
	.stop()
	.execute();
```

By default, the task looks for the pif file in the working directory at `[WORKING_PATH]/[PROJECT_NAME].pid` and waits 60 seconds for the process to stop, these can also be specified by configuration:

```java
Project project = ... 
project
	.stop()
		.pidfile(Path.of("path/to/pidfile")
		.timeout(30000)
	.execute();
```

### BuildJmodTask

The `BuildJmodTask` is chained to the `ModularizeDependenciesTask`, it used to create a `jmod` archive of the project module.

The project `jmod` archive is created as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.execute();
```

The task relies on JDK's [jmod][jmod] tool and as such it provides options to include configuration files, legal resources or manuals inside the archive. The module's main class, if any, can also be specified or automatically resolved:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
		.resolveMainClass(true)
		.configurationPath(Path.of("path/to/config)
		.legalPath(Path.of("path/to/legal)
		.manPath(Path.of("path/to/manual)
	.execute();
```

Note that if the module defines multipled main class, the task will raise an error asking to specify the main class explicitly:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
		.mainClass("io.inverno.example.MainClass")
	.execute();
```

### BuildRuntimeTask

The `BuildRuntimeTask` is chained to the `BuildJmodTask`, it is used to create an optimized runtime image which contains the project module and its exact dependency modules including JDK's modules. A runtime image can be used to compile or run Java applications with a reduced footprint Java runtime, but it is especially required to create native application images.

A project application runtime image can be created as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.execute();
```

The task relies on JDK's [jlink][jlink] tool, it is then possible to configure how the runtime image is generated:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
		.addModules("io.inverno.example.module1, io.inverno.example.module2") // --add-modules
		.addOptions("-DsomeProperty=1234")                                    // VM options
		.compress(2)                                                          // 2=ZIP
		.stripDebug(true)                                                     // Remove debug information
		.stripNativeCommands(false)                                           // Do not include native commands in the image: java, keytool...
		.vm("server")                                                         // Optimize for server application
	.execute();
```

Application launchers can also be generated in which case native commands must be included in the image (i.e. `java`), the `stripNativeCommands` option must then be set to false. Unlike the `PackageApplicationTask` which generates native launchers, runtime launchers are simple shell scripts invoking the runtime's `java` command to launch a particular main class in a module bundled in the runtime image.

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
		.stripNativeCommands(false)
		.launchers(BuildRuntimeTask.Launcher.of("myApp", "io.inverno.example.myapp", "io.inverno.example.MyAppMain"))
	.execute();
```

A runtime image generated with a launcher should have the following structure:

```
.
├── bin
│   ├── myApp
│   ├── java
│   ├── jrunscript
│   ├── keytool
│   └── rmiregistry
├── conf
│   └── ...
├── include
│   └── ...
├── legal
│   └── ...
├── lib
│   └── ...
├── man
│   └── ...
└── release
```

> Note that a runtime image is native as it embeds a JVM which relates to the building environment as a result an image generated on a Linux environment can't *run* on a Windows environment.

### PackageApplicationTask

The `PackageApplicationTask` is chained to the `BuildRuntimeTask`, it is used to create a self-contained Java application image including project launchers, the project application module, all its dependencies and an optimized Java runtime all packaged in a native OS specific package (e.g. `.deb`, `.msi`, `.dmg`...). Unlike a runtime image which can be created without application launchers, an application image requires at least one launcher otherwise it wouldn't be considered an application, as a result a main class must be defined, ideally in the project application module so it can be automatically resolved.

A project application module defining a main class can be packaged in a Debian archive as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
		.types(Set.of(PackageApplicationTask.PackageType.DEB))
	.execute();
```

The task relies on JDK's [jpackage][jpackage] tool and it exposes most of its options:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
		.copyright("Copyright 2021 Inverno")
		.vendor("Inverno")
		.licensePath(Path.of("path/to/license")
		.resourcesPath(Path.of("path/to/resources")
	.execute();
```

The application image is also built on top of the runtime image, itself built on top of the jmod archive which were generated by previous tasks, it then inherit information such as configurations, legals and manuals...

Application launchers are native binaries starting the JVM, at least one launcher must be specified to generate an application image. If none is specified, the task will try to automatically create one looking for a main class in the project application module.

Launchers can be specified explicitly as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
		.launchers(Set.of(new PackageApplicationTask.Launcher() {
			
			public Optional<String> getName() {
				return Optional.of("myApp");
			}

			public Optional<String> getDescription() {
				return Optional.of("This is my application");
			}

			public Optional<String> getModule() {
				return Optional.of("io.inverno.example.myapp");
			}

			public Optional<String> getMainClass() {
				return Optional.of("io.inverno.example.MyAppMain");
			}

			public Optional<Path> getIconPath() {
				return Optional.of(Path.of("path/to/icon"));
			}

			public boolean isLauncherAsService() {
				return true;
			}

			...
		}))
	.execute();
```

The task can also automatically generates as many launchers as there are main classes in the project application module, launcher names being the class names:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
		.automaticLaunchers(true)
	.execute();
```

OS specific configuration can also be specified when generating OS specific packages. For instance, a Linux configuration can be provided as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
		.linuxConfiguration(new PackageApplicationTask.LinuxConfiguration() {

			public Optional<String> getPackageName() {
				return Optional.of("myApp");
			}

			public Optional<String> getDebMaintainer() {
				return Optional.of("John Smith");
			}

			...
		})
	.execute();
```

> The task generates the application image to a folder ([jpackage][jpackage]'s `app-image` type) or to OS specific package formats such as `.deb`, `.msi`, `.dmg`... In order to package the application image in a portable archive format (e.g. `zip`, `tar.gz`...), the `ArchiveTask` must be chained.

> Just like a runtime image, an application image is native and tight to the building environment, a Linux application image can't run on a Windows environment. Cross-platform is also not supported by [jpackage][jpackage] so a Windows application image can't be built on a Linux environment.

### ArchiveTask

The `ArchiveTask` is chained to the `BuildRuntimeTask` or the `PackageApplicationTask` in order to respectively package the runtime image or the application image into a portable archive format (e.g. `zip`, `tar.gz`...).

A project runtime can be packaged in a `.zip` archive as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.archive()
		.formats("zip")
	.execute();
```

A project application image can be packaged in a `tar.gz` archive as follows:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
	.archive()
		.formats("tar.gz")
	.execute();
```

By default, within the archive the image is placed in a folder named after the project's final name but it is possible to override this and explicitly specify where to place the image in the archive:

```java
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
	.archive()
		.prefix("some/path")
		.formats("tar.bz2")
	.execute();
```

### ContainerizeTask

The `ContainerizeTask` is chained to the `PackageApplicationTask` it is used to create a container image of the project application image so it can be run in a container. The resulting image can be packaged in a portable `.tar` archive, loaded into the local [Docker][docker] daemon or published to a remote container image registry.

A `.tar` archive containing the project application container image can be generated as follows:

```
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
	.containerize()
	.execute();
```

Note that the image is generated in [OCI][oci] format, in order to generate a `.tar` archive that can be loaded in a [Docker][docker] daemon, the format must be set explicitly:

```
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
	.containerize()
		.format(ContainerizeTask.Format.Docker)
	.execute();
```

The `.tar` thus obtained can be loaded to a [Docker][docker] daemon as follows:

```
$ docker load --input myApp-1.0.0-SNAPSHOT-container_linux_amd64.tar
```

The container image can also be directly loaded to the local [Docker][docker] daemon by selecting the `Docker` target:

```
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
	.containerize()
		.target(ContainerizeTask.Target.DOCKER)
	.execute();
```

The task can be configured to push the image to a registry:

```
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
	.containerize()
		.registry("docker.io")
		.repository("my-docker-id")
		.registryUsername("username")
		.registryPassword("password")
	.execute();
```

The task also supports options to configure the resulting container image:

```
Project project = ... 
project
	.modularizeDependencies()
	.buildJmod()
	.buildRuntime()
	.packageApplication()
	.containerize()
		.from("ubuntu:24.04")
		.labels(Map.of("io.inverno.category", "example"))
		.ports(8080)
		.volumes(Set.of("/opt/my-app/logs"))
		.user("user")
		.environment(Map.of("INVERNO_PROFILE", "prod"))
	.execute();
```
