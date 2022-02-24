
# Inverno Maven Plugin
The Inverno Maven Plugin is used to run, package and distribute modular applications and Inverno applications in particular. It relies on a set of Java tools to build native runtime or application images as well as Docker or OCI images for modular Java projects.

## Usage


The Inverno Maven plugin can be used to run a modular application project or build an image for a modular project. There are three types of images that can be build using the plugin:

- **runtime image** is a custom Java runtime containing a set of modules and their dependencies.
- **application image** is a native self-contained Java application including all the necessary dependencies to run the application without the need of a Java runtime. 
- **container image** is a Docker or CLI container image that can be packaged as a TAR archive or directly deployed on a Docker daemon or container registry.

### Run a module application project

The `inverno:run` goal is used to execute the modular application defined in the project from the command line.

```plaintext
$ mvn inverno:run
```

The application is first *modularized* which means that any non-modular dependency is modularized by generating an appropriate module descriptor using the `jdeps` tool in order for the application to be run with a module path and not a class path (and certainly not both).

The application is executed in a forked process, application arguments can be passed on the command line:

```plaintext
$ mvn inverno:run -Dinverno.run.arguments='--some.configuration=\"hello\"'
```

> Actual arguments are determined by splitting the parameter value around spaces. There are several options to declare an argument which contains spaces:
> - it can be escaped: `Hello\ World` 
> - it can be quoted: `"Hello World"` or `'Hello World'`
>
> Since quotes or double quotes are used as delimiters, they might need to be escaped as well to declare an argument that contains some: `I\'m\ happy`, `"I'm happy"`, `'I\'m happy'`.

In order to debug the application, we need to specify the appropriate options to the JVM:

```plaintext
$ mvn inverno:run -Dinverno.exec.vmOptions="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
```

By default the plugin will detect the main class of the application, but it is also possible to specify it explicitly in case multiple main classes exist in the project module.

```plaintext
$ mvn inverno:run -Dinverno.exec.mainClass=io.inverno.example.Main
```

A pidfile is created when the application is started under `${project.build.directory}/maven-inverno` directory, it indicates the pid of the process running the application. If the build exits while the application is still running or if the pidfile was not properly removed after the application has exited, it might be necessary to manually kill the process and/or remove the pidfile. 

### Start and stop the application for integration testing

The `inverno:start` and `inverno:stop` goals are used together to start and stop the application while not blocking the Maven build process which can then execute other goals targeting the running application such as integration tests.

They are bound to the `pre-integration-test` and `pre-integration-test` phases respectively:

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>start</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>start</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>stop</id>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>stop</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### Build a runtime image

A runtime image is a custom Java runtime distribution containing specific modules and their dependencies. Such image is used as a base for generating application image but it can also be distributed as a lightweight Java runtime.

The `inverno:build-runtime` goal uses `jlink` tool to assemble the project module and its dependencies.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>build-project-runtime</id>
                        <phase>package</phase>
                        <goals>
                            <goal>build-runtime</goal>
                        </goals>
                        <configuration>
                            <vm>server</vm>
                            <addModules>jdk.jdwp.agent,jdk.crypto.ec</addModules>
                            <vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
                            <formats>
                                <format>zip</format>
                                <format>tar.gz</format>
                                <format>tar.bz2</format>
                            </formats>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

By default, the project module and its dependencies are included in the resulting image, this include JDK's modules such as `java.base`, in the previous example we've also explicitly added the `jdk.jdwp.agent` to support remote debugging and `jdk.crypto.ec` to support TLS communications.

The resulting image is packaged to the formats defined in the configuration and attached, by default, to the Maven project. 

### Build an application image

An application image is built using the `inverno:build-app` goal which basically generates a runtime image and uses `jpackage` tool to generate a native platform-specific application package.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>build-application</id>
                        <phase>package</phase>
                        <goals>
                            <goal>build-app</goal>
                        </goals>
                        <configuration>
                            <vm>server</vm>
                            <addModules>jdk.jdwp.agent,jdk.crypto.ec</addModules>
                            <launchers>
                                <launcher>
                                    <name>app</name>
                                    <vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
                                </launcher>
                            </launchers>
                            <formats>
                                <format>zip</format>
                                <format>deb</format>
                            </formats>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

The `inverno:build-app` goal is very similar to the `inverno:build-runtime` goal except that the resulting image provides an application launcher and it can be packaged in a platform-specific format. For instance, we can generate a `.deb` on a Linux platform or a `.exe` or `.msi` on a Windows platform or a `.dmg` on a MacOS platform. The resulting package can be installed on these platforms in a standard way.

> This goal uses `jpackage` tool which is an incubating feature in JDK&lt;16, if you intend to build an application image with an old JDK, you'll need to explicitly add the `jdk.incubator.jpackage` module in `MAVEN_OPTS`:
> ```plaintext
> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
> ```

### Build a container image tarball

A container image can be built in a TAR archive using the `inverno:build-image-tar` goal which basically build an application package and package it in a container image.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>build-image-tar</id>
                        <phase>package</phase>
                        <goals>
                            <goal>build-image-tar</goal>
                        </goals>
                        <configuration>
                            <vm>server</vm>
                            <addModules>jdk.jdwp.agent,jdk.crypto.ec</addModules>
                            <executable>app</executable>
                            <launchers>
                                <launcher>
                                    <name>app</name>
                                    <vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
                                </launcher>
                            </launchers>
                            <repository>example</repository>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

The resulting image reference is defined by `${registry}/${repository}/${name}:${project.version}`, the registry and the repository are optional and the name default to the project artifact id. 

The resulting image can then be loaded in a docker daemon:

```plaintext
$ docker load --input target/example-1.0.0-SNAPSHOT-container_linux_amd64.tar
```

> As for `build-app` goal, this goal uses `jpackage` tool so if you intend to use a JDK&lt;16 you'll need to explicitly add the `jdk.incubator.jpackage` module in `MAVEN_OPTS`:
> ```plaintext
> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
> ```

### Build and deploy a container image to a Docker daemon

The `inverno:build-image-docker` goal is used to build a container image and deploy it to a Docker daemon using the Docker CLI.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>build-image-docker</id>
                        <phase>package</phase>
                        <goals>
                            <goal>build-image-docker</goal>
                        </goals>
                        <configuration>
                            <vm>server</vm>
                            <addModules>jdk.jdwp.agent,jdk.crypto.ec</addModules>
                            <executable>app</executable>
                            <launchers>
                                <launcher>
                                    <name>app</name>
                                    <vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
                                </launcher>
                            </launchers>
                            <repository>example</repository>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

By default the `docker` command is used but it is possible to specify the path to the Docker CLI in the `inverno.container.docker.executable` parameter. 

> As for `build-app` goal, this goal uses `jpackage` tool so if you intend to use a JDK&lt;16 you'll need to explicitly add the `jdk.incubator.jpackage` module in `MAVEN_OPTS`:
> ```plaintext
> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
> ```

### Build and deploy a container image to a remote repository

The `inverno:build-image` goal builds a container image and deploy it to a remote repository.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>build-image-docker</id>
                        <phase>package</phase>
                        <goals>
                            <goal>build-image-docker</goal>
                        </goals>
                        <configuration>
                            <vm>server</vm>
                            <addModules>jdk.jdwp.agent,jdk.crypto.ec</addModules>
                            <executable>app</executable>
                            <launchers>
                                <launcher>
                                    <name>app</name>
                                    <vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
                                </launcher>
                            </launchers>
                            <registryUsername>user</registryUsername>
                            <registryPassword>password</registryPassword>
                            <registry>gcr.io</registry>
                            <repository>example</repository>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

By default the registry points to the Docker hub `registry-1.docker.io` but another registry can be specified, `gcr.io` in our example.

> As for `build-app` goal, this goal uses `jpackage` tool so if you intend to use a JDK&lt;16 you'll need to explicitly add the `jdk.incubator.jpackage` module in `MAVEN_OPTS`:
> ```plaintext
> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
> ```



## Goals

### Overview

- [inverno:build-app](#invernobuild-app) Builds the project application package.
- [inverno:build-image](#invernobuild-image) Builds a container image and publishes it to a registry.
- [inverno:build-image-docker](#invernobuild-image-docker) Builds a Docker container image to a local Docker daemon.
- [inverno:build-image-tar](#invernobuild-image-tar) Builds a container image to a TAR archive that can be later loaded into Docker:
- [inverno:build-runtime](#invernobuild-runtime) Builds the project runtime image.
- [inverno:help](#invernohelp) Display help information on inverno-maven-plugin.
- [inverno:run](#invernorun) Runs the project application.
- [inverno:start](#invernostart) Starts the project application without blocking the Maven build.
- [inverno:stop](#invernostop) Stops the project application that has been previously started using the start goal.

### inverno:build-app

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:build-app

**Description:**

Builds the project application package.

A project application package is a native self-contained Java application including all the necessary dependencies. It can be used to distribute a complete application.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.0.
- Binds by default to the lifecycle phase: package.


#### Required parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#attach">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.attach
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#formats">formats</a>
        </td>
        <td>Set</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>Default</em>
                    : zip
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#addModules">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addModules
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addOptions">addOptions</a>
        </td>
        <td>String</td>
        <td>
            The options to prepend before any other options when invoking the JVM in the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#automaticLaunchers">automaticLaunchers</a>
        </td>
        <td>boolean</td>
        <td>
            Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.automaticLaunchers
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#bindServices">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Link in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.bindServices
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#compress">compress</a>
        </td>
        <td>String</td>
        <td>
            The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.compress
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#configurationDirectory">configurationDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing user-editable configuration files that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.configurationDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/conf/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#copyright">copyright</a>
        </td>
        <td>String</td>
        <td>
            The application copyright.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.copyright
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#description">description</a>
        </td>
        <td>String</td>
        <td>
            The description of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.description
                </li>
                <li>
                    <em>Default</em>
                    : ${project.description}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeArtifactIds">excludeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeClassifiers">excludeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : excludeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeGroupIds">excludeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupId Names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeScope">excludeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to exclude. An Empty string indicates no scopes (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ignoreSigningInformation">ignoreSigningInformation</a>
        </td>
        <td>boolean</td>
        <td>
            Suppress a fatal error when signed modular JARs are linked in the image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.ignoreSigningInformation
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeArtifactIds">includeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeClassifiers">includeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeGroupIds">includeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupIds to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeScope">includeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#installDirectory">installDirectory</a>
        </td>
        <td>String</td>
        <td>
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.installDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#jmodsOverrideDirectory">jmodsOverrideDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.jmodsOverrideDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/jmods/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers">launchers</a>
        </td>
        <td>List</td>
        <td>
            A list of extra launchers to include in the resulting application.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.legalDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/legal/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#licenseFile">licenseFile</a>
        </td>
        <td>File</td>
        <td>
            The path to the application license file.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.licenseFile
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/LICENSE
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#linuxConfiguration">linuxConfiguration</a>
        </td>
        <td>LinuxConfiguration</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration">macOSConfiguration</a>
        </td>
        <td>MacOSConfiguration</td>
        <td>
            MacOS specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#manDirectory">manDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing man pages that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.manDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/man/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#overWriteIfNewer">overWriteIfNewer</a>
        </td>
        <td>boolean</td>
        <td>
            Overwrite dependencies that don't exist or are older than the source.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.overWriteIfNewer
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#projectMainClass">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resolveProjectMainClass">resolveProjectMainClass</a>
        </td>
        <td>boolean</td>
        <td>
            Resolve the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resourceDirectory">resourceDirectory</a>
        </td>
        <td>File</td>
        <td>
            The path to resources that override resulting package resources.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.resourceDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#skip">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the generation of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripDebug">stripDebug</a>
        </td>
        <td>boolean</td>
        <td>
            Strip debug information from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripDebug
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripNativeCommands">stripNativeCommands</a>
        </td>
        <td>boolean</td>
        <td>
            Strip native command (eg. java...) from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripNativeCommands
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vendor">vendor</a>
        </td>
        <td>String</td>
        <td>
            The application vendor.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.vendor
                </li>
                <li>
                    <em>Default</em>
                    : ${project.organization.name}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vm">vm</a>
        </td>
        <td>String</td>
        <td>
            Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration">windowsConfiguration</a>
        </td>
        <td>WindowsConfiguration</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addModules&gt;

The modules to add to the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addOptions


##### &lt;attach&gt;

Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.image.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Link in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;description&gt;

The description of the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.description
- **Default**: ${project.description}


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeScope


##### &lt;formats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set
- **Required**: yes
- **Default**: zip


##### &lt;ignoreSigningInformation&gt;

Suppress a fatal error when signed modular JARs are linked in the image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.ignoreSigningInformation
- **Default**: false


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma Separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
-	runtime scope gives runtime and compile dependencies,
-	compile scope gives compile, provided, and system dependencies,
-	test (default) scope gives all dependencies,
-	provided scope just gives provided dependencies,
-	system scope just gives system dependencies.


- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;jmodsOverrideDirectory&gt;

A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.jmodsOverrideDirectory
- **Default**: ${project.basedir}/src/jmods/


##### &lt;launchers&gt;

A list of extra launchers to include in the resulting application.

- **Type**: java.util.List
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$LinuxConfiguration
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$MacOSConfiguration
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;overWriteIfNewer&gt;

Overwrite dependencies that don't exist or are older than the source.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.overWriteIfNewer
- **Default**: true


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.projectMainClass


##### &lt;resolveProjectMainClass&gt;

Resolve the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.projectMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the generation of the application.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.skip


##### &lt;stripDebug&gt;

Strip debug information from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strip native command (eg. java...) from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripNativeCommands
- **Default**: true


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false


##### &lt;vm&gt;

Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.vm


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$WindowsConfiguration
- **Required**: no


### inverno:build-image

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:build-image

**Description:**

Builds a container image and publishes it to a registry.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.0.
- Binds by default to the lifecycle phase: install.


#### Required parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#attach1">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.attach
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#executable">executable</a>
        </td>
        <td>String</td>
        <td>
            The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.executable
                </li>
                <li>
                    <em>Default</em>
                    : ${project.artifactId}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#formats1">formats</a>
        </td>
        <td>Set</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>Default</em>
                    : zip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#from">from</a>
        </td>
        <td>String</td>
        <td>
            The base container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.from
                </li>
                <li>
                    <em>Default</em>
                    : debian:buster-slim
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#addModules1">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addModules
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addOptions1">addOptions</a>
        </td>
        <td>String</td>
        <td>
            The options to prepend before any other options when invoking the JVM in the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#automaticLaunchers1">automaticLaunchers</a>
        </td>
        <td>boolean</td>
        <td>
            Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.automaticLaunchers
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#bindServices1">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Link in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.bindServices
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#compress1">compress</a>
        </td>
        <td>String</td>
        <td>
            The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.compress
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#configurationDirectory1">configurationDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing user-editable configuration files that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.configurationDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/conf/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#copyright1">copyright</a>
        </td>
        <td>String</td>
        <td>
            The application copyright.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.copyright
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#description1">description</a>
        </td>
        <td>String</td>
        <td>
            The description of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.description
                </li>
                <li>
                    <em>Default</em>
                    : ${project.description}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#environment">environment</a>
        </td>
        <td>Map</td>
        <td>
            The container's environment variables.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeArtifactIds1">excludeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeClassifiers1">excludeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : excludeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeGroupIds1">excludeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupId Names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeScope1">excludeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to exclude. An Empty string indicates no scopes (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ignoreSigningInformation1">ignoreSigningInformation</a>
        </td>
        <td>boolean</td>
        <td>
            Suppress a fatal error when signed modular JARs are linked in the image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.ignoreSigningInformation
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#imageFormat">imageFormat</a>
        </td>
        <td>ImageFormat</td>
        <td>
            The format of the container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.imageFormat
                </li>
                <li>
                    <em>Default</em>
                    : Docker
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeArtifactIds1">includeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeClassifiers1">includeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeGroupIds1">includeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupIds to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeScope1">includeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#installDirectory1">installDirectory</a>
        </td>
        <td>String</td>
        <td>
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.installDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#jmodsOverrideDirectory1">jmodsOverrideDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.jmodsOverrideDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/jmods/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#labels">labels</a>
        </td>
        <td>Map</td>
        <td>
            The labels to apply to the container image.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers1">launchers</a>
        </td>
        <td>List</td>
        <td>
            A list of extra launchers to include in the resulting application.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory1">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.legalDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/legal/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#licenseFile1">licenseFile</a>
        </td>
        <td>File</td>
        <td>
            The path to the application license file.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.licenseFile
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/LICENSE
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#linuxConfiguration1">linuxConfiguration</a>
        </td>
        <td>LinuxConfiguration</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration1">macOSConfiguration</a>
        </td>
        <td>MacOSConfiguration</td>
        <td>
            MacOS specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#manDirectory1">manDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing man pages that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.manDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/man/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#overWriteIfNewer1">overWriteIfNewer</a>
        </td>
        <td>boolean</td>
        <td>
            Overwrite dependencies that don't exist or are older than the source.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.overWriteIfNewer
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ports">ports</a>
        </td>
        <td>Set</td>
        <td>
            The ports exposed by the container at runtime defined as: port_number [ '/' udp/tcp ]
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#projectMainClass1">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#registry">registry</a>
        </td>
        <td>String</td>
        <td>
            The registry part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.registry
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#registryPassword">registryPassword</a>
        </td>
        <td>String</td>
        <td>
            The password to use to authenticate to the registry.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.registry.password
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#registryUsername">registryUsername</a>
        </td>
        <td>String</td>
        <td>
            The user name to use to authenticate to the registry.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.registry.username
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#repository">repository</a>
        </td>
        <td>String</td>
        <td>
            The repository part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.repository
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resolveProjectMainClass1">resolveProjectMainClass</a>
        </td>
        <td>boolean</td>
        <td>
            Resolve the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resourceDirectory1">resourceDirectory</a>
        </td>
        <td>File</td>
        <td>
            The path to resources that override resulting package resources.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.resourceDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#skip1">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the generation of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripDebug1">stripDebug</a>
        </td>
        <td>boolean</td>
        <td>
            Strip debug information from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripDebug
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripNativeCommands1">stripNativeCommands</a>
        </td>
        <td>boolean</td>
        <td>
            Strip native command (eg. java...) from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripNativeCommands
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#user">user</a>
        </td>
        <td>String</td>
        <td>
            The user and group used to run the container defined as: user / uid [ ':' group / gid ]
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vendor1">vendor</a>
        </td>
        <td>String</td>
        <td>
            The application vendor.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.vendor
                </li>
                <li>
                    <em>Default</em>
                    : ${project.organization.name}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose1">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vm1">vm</a>
        </td>
        <td>String</td>
        <td>
            Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#volumes">volumes</a>
        </td>
        <td>Set</td>
        <td>
            The container's mount points.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration1">windowsConfiguration</a>
        </td>
        <td>WindowsConfiguration</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addModules&gt;

The modules to add to the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addOptions


##### &lt;attach&gt;

Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.image.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Link in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;description&gt;

The description of the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.description
- **Default**: ${project.description}


##### &lt;environment&gt;

The container's environment variables.

- **Type**: java.util.Map
- **Required**: no


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeScope


##### &lt;executable&gt;

The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.app.executable
- **Default**: ${project.artifactId}


##### &lt;formats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set
- **Required**: yes
- **Default**: zip


##### &lt;from&gt;

The base container image.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.from
- **Default**: debian:buster-slim


##### &lt;ignoreSigningInformation&gt;

Suppress a fatal error when signed modular JARs are linked in the image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.ignoreSigningInformation
- **Default**: false


##### &lt;imageFormat&gt;

The format of the container image.

- **Type**: com.google.cloud.tools.jib.api.buildplan.ImageFormat
- **Required**: no
- **User property**: inverno.container.imageFormat
- **Default**: Docker


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma Separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
-	runtime scope gives runtime and compile dependencies,
-	compile scope gives compile, provided, and system dependencies,
-	test (default) scope gives all dependencies,
-	provided scope just gives provided dependencies,
-	system scope just gives system dependencies.


- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;jmodsOverrideDirectory&gt;

A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.jmodsOverrideDirectory
- **Default**: ${project.basedir}/src/jmods/


##### &lt;labels&gt;

The labels to apply to the container image.

- **Type**: java.util.Map
- **Required**: no


##### &lt;launchers&gt;

A list of extra launchers to include in the resulting application.

- **Type**: java.util.List
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$LinuxConfiguration
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$MacOSConfiguration
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;overWriteIfNewer&gt;

Overwrite dependencies that don't exist or are older than the source.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.overWriteIfNewer
- **Default**: true


##### &lt;ports&gt;

The ports exposed by the container at runtime defined as: port_number [ '/' udp/tcp ]

- **Type**: java.util.Set
- **Required**: no


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.projectMainClass


##### &lt;registry&gt;

The registry part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registry


##### &lt;registryPassword&gt;

The password to use to authenticate to the registry.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registry.password


##### &lt;registryUsername&gt;

The user name to use to authenticate to the registry.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registry.username


##### &lt;repository&gt;

The repository part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.repository


##### &lt;resolveProjectMainClass&gt;

Resolve the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.projectMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the generation of the application.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.skip


##### &lt;stripDebug&gt;

Strip debug information from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strip native command (eg. java...) from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripNativeCommands
- **Default**: true


##### &lt;user&gt;

The user and group used to run the container defined as: user / uid [ ':' group / gid ]

- **Type**: java.lang.String
- **Required**: no


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false


##### &lt;vm&gt;

Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.vm


##### &lt;volumes&gt;

The container's mount points.

- **Type**: java.util.Set
- **Required**: no


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$WindowsConfiguration
- **Required**: no


### inverno:build-image-docker

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:build-image-docker

**Description:**

Builds a Docker container image to a local Docker daemon.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.0.
- Binds by default to the lifecycle phase: install.


#### Required parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#attach2">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.attach
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#executable1">executable</a>
        </td>
        <td>String</td>
        <td>
            The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.executable
                </li>
                <li>
                    <em>Default</em>
                    : ${project.artifactId}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#formats2">formats</a>
        </td>
        <td>Set</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>Default</em>
                    : zip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#from1">from</a>
        </td>
        <td>String</td>
        <td>
            The base container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.from
                </li>
                <li>
                    <em>Default</em>
                    : debian:buster-slim
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#addModules2">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addModules
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addOptions2">addOptions</a>
        </td>
        <td>String</td>
        <td>
            The options to prepend before any other options when invoking the JVM in the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#automaticLaunchers2">automaticLaunchers</a>
        </td>
        <td>boolean</td>
        <td>
            Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.automaticLaunchers
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#bindServices2">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Link in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.bindServices
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#compress2">compress</a>
        </td>
        <td>String</td>
        <td>
            The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.compress
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#configurationDirectory2">configurationDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing user-editable configuration files that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.configurationDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/conf/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#copyright2">copyright</a>
        </td>
        <td>String</td>
        <td>
            The application copyright.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.copyright
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#description2">description</a>
        </td>
        <td>String</td>
        <td>
            The description of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.description
                </li>
                <li>
                    <em>Default</em>
                    : ${project.description}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#dockerEnvironment">dockerEnvironment</a>
        </td>
        <td>Map</td>
        <td>
            The Docker environment variables used by the Docker CLI executable.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#dockerExecutable">dockerExecutable</a>
        </td>
        <td>File</td>
        <td>
            The path to the Docker CLI executable used to load the image in the Docker daemon.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.docker.executable
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#environment1">environment</a>
        </td>
        <td>Map</td>
        <td>
            The container's environment variables.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeArtifactIds2">excludeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeClassifiers2">excludeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : excludeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeGroupIds2">excludeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupId Names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeScope2">excludeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to exclude. An Empty string indicates no scopes (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ignoreSigningInformation2">ignoreSigningInformation</a>
        </td>
        <td>boolean</td>
        <td>
            Suppress a fatal error when signed modular JARs are linked in the image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.ignoreSigningInformation
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#imageFormat1">imageFormat</a>
        </td>
        <td>ImageFormat</td>
        <td>
            The format of the container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.imageFormat
                </li>
                <li>
                    <em>Default</em>
                    : Docker
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeArtifactIds2">includeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeClassifiers2">includeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeGroupIds2">includeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupIds to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeScope2">includeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#installDirectory2">installDirectory</a>
        </td>
        <td>String</td>
        <td>
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.installDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#jmodsOverrideDirectory2">jmodsOverrideDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.jmodsOverrideDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/jmods/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#labels1">labels</a>
        </td>
        <td>Map</td>
        <td>
            The labels to apply to the container image.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers2">launchers</a>
        </td>
        <td>List</td>
        <td>
            A list of extra launchers to include in the resulting application.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory2">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.legalDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/legal/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#licenseFile2">licenseFile</a>
        </td>
        <td>File</td>
        <td>
            The path to the application license file.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.licenseFile
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/LICENSE
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#linuxConfiguration2">linuxConfiguration</a>
        </td>
        <td>LinuxConfiguration</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration2">macOSConfiguration</a>
        </td>
        <td>MacOSConfiguration</td>
        <td>
            MacOS specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#manDirectory2">manDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing man pages that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.manDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/man/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#overWriteIfNewer2">overWriteIfNewer</a>
        </td>
        <td>boolean</td>
        <td>
            Overwrite dependencies that don't exist or are older than the source.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.overWriteIfNewer
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ports1">ports</a>
        </td>
        <td>Set</td>
        <td>
            The ports exposed by the container at runtime defined as: port_number [ '/' udp/tcp ]
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#projectMainClass2">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#registry1">registry</a>
        </td>
        <td>String</td>
        <td>
            The registry part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.registry
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#repository1">repository</a>
        </td>
        <td>String</td>
        <td>
            The repository part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.repository
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resolveProjectMainClass2">resolveProjectMainClass</a>
        </td>
        <td>boolean</td>
        <td>
            Resolve the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resourceDirectory2">resourceDirectory</a>
        </td>
        <td>File</td>
        <td>
            The path to resources that override resulting package resources.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.resourceDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#skip2">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the generation of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripDebug2">stripDebug</a>
        </td>
        <td>boolean</td>
        <td>
            Strip debug information from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripDebug
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripNativeCommands2">stripNativeCommands</a>
        </td>
        <td>boolean</td>
        <td>
            Strip native command (eg. java...) from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripNativeCommands
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#user1">user</a>
        </td>
        <td>String</td>
        <td>
            The user and group used to run the container defined as: user / uid [ ':' group / gid ]
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vendor2">vendor</a>
        </td>
        <td>String</td>
        <td>
            The application vendor.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.vendor
                </li>
                <li>
                    <em>Default</em>
                    : ${project.organization.name}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose2">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vm2">vm</a>
        </td>
        <td>String</td>
        <td>
            Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#volumes1">volumes</a>
        </td>
        <td>Set</td>
        <td>
            The container's mount points.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration2">windowsConfiguration</a>
        </td>
        <td>WindowsConfiguration</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addModules&gt;

The modules to add to the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addOptions


##### &lt;attach&gt;

Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.image.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Link in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;description&gt;

The description of the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.description
- **Default**: ${project.description}


##### &lt;dockerEnvironment&gt;

The Docker environment variables used by the Docker CLI executable.

- **Type**: java.util.Map
- **Required**: no


##### &lt;dockerExecutable&gt;

The path to the Docker CLI executable used to load the image in the Docker daemon.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.container.docker.executable


##### &lt;environment&gt;

The container's environment variables.

- **Type**: java.util.Map
- **Required**: no


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeScope


##### &lt;executable&gt;

The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.app.executable
- **Default**: ${project.artifactId}


##### &lt;formats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set
- **Required**: yes
- **Default**: zip


##### &lt;from&gt;

The base container image.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.from
- **Default**: debian:buster-slim


##### &lt;ignoreSigningInformation&gt;

Suppress a fatal error when signed modular JARs are linked in the image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.ignoreSigningInformation
- **Default**: false


##### &lt;imageFormat&gt;

The format of the container image.

- **Type**: com.google.cloud.tools.jib.api.buildplan.ImageFormat
- **Required**: no
- **User property**: inverno.container.imageFormat
- **Default**: Docker


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma Separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
-	runtime scope gives runtime and compile dependencies,
-	compile scope gives compile, provided, and system dependencies,
-	test (default) scope gives all dependencies,
-	provided scope just gives provided dependencies,
-	system scope just gives system dependencies.


- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;jmodsOverrideDirectory&gt;

A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.jmodsOverrideDirectory
- **Default**: ${project.basedir}/src/jmods/


##### &lt;labels&gt;

The labels to apply to the container image.

- **Type**: java.util.Map
- **Required**: no


##### &lt;launchers&gt;

A list of extra launchers to include in the resulting application.

- **Type**: java.util.List
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$LinuxConfiguration
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$MacOSConfiguration
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;overWriteIfNewer&gt;

Overwrite dependencies that don't exist or are older than the source.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.overWriteIfNewer
- **Default**: true


##### &lt;ports&gt;

The ports exposed by the container at runtime defined as: port_number [ '/' udp/tcp ]

- **Type**: java.util.Set
- **Required**: no


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.projectMainClass


##### &lt;registry&gt;

The registry part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registry


##### &lt;repository&gt;

The repository part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.repository


##### &lt;resolveProjectMainClass&gt;

Resolve the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.projectMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the generation of the application.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.skip


##### &lt;stripDebug&gt;

Strip debug information from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strip native command (eg. java...) from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripNativeCommands
- **Default**: true


##### &lt;user&gt;

The user and group used to run the container defined as: user / uid [ ':' group / gid ]

- **Type**: java.lang.String
- **Required**: no


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false


##### &lt;vm&gt;

Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.vm


##### &lt;volumes&gt;

The container's mount points.

- **Type**: java.util.Set
- **Required**: no


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$WindowsConfiguration
- **Required**: no


### inverno:build-image-tar

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:build-image-tar

**Description:**

Builds a container image to a TAR archive that can be later loaded into Docker:

$ docker load --input target/&lt;image&gt;.tar 


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.0.
- Binds by default to the lifecycle phase: package.


#### Required parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#attach3">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.attach
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#executable2">executable</a>
        </td>
        <td>String</td>
        <td>
            The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.executable
                </li>
                <li>
                    <em>Default</em>
                    : ${project.artifactId}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#formats3">formats</a>
        </td>
        <td>Set</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>Default</em>
                    : zip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#from2">from</a>
        </td>
        <td>String</td>
        <td>
            The base container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.from
                </li>
                <li>
                    <em>Default</em>
                    : debian:buster-slim
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#addModules3">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addModules
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addOptions3">addOptions</a>
        </td>
        <td>String</td>
        <td>
            The options to prepend before any other options when invoking the JVM in the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#automaticLaunchers3">automaticLaunchers</a>
        </td>
        <td>boolean</td>
        <td>
            Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.automaticLaunchers
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#bindServices3">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Link in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.bindServices
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#compress3">compress</a>
        </td>
        <td>String</td>
        <td>
            The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.compress
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#configurationDirectory3">configurationDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing user-editable configuration files that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.configurationDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/conf/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#copyright3">copyright</a>
        </td>
        <td>String</td>
        <td>
            The application copyright.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.copyright
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#description3">description</a>
        </td>
        <td>String</td>
        <td>
            The description of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.description
                </li>
                <li>
                    <em>Default</em>
                    : ${project.description}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#environment2">environment</a>
        </td>
        <td>Map</td>
        <td>
            The container's environment variables.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeArtifactIds3">excludeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeClassifiers3">excludeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : excludeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeGroupIds3">excludeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupId Names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeScope3">excludeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to exclude. An Empty string indicates no scopes (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ignoreSigningInformation3">ignoreSigningInformation</a>
        </td>
        <td>boolean</td>
        <td>
            Suppress a fatal error when signed modular JARs are linked in the image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.ignoreSigningInformation
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#imageFormat2">imageFormat</a>
        </td>
        <td>ImageFormat</td>
        <td>
            The format of the container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.imageFormat
                </li>
                <li>
                    <em>Default</em>
                    : Docker
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeArtifactIds3">includeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeClassifiers3">includeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeGroupIds3">includeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupIds to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeScope3">includeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#installDirectory3">installDirectory</a>
        </td>
        <td>String</td>
        <td>
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.installDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#jmodsOverrideDirectory3">jmodsOverrideDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.jmodsOverrideDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/jmods/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#labels2">labels</a>
        </td>
        <td>Map</td>
        <td>
            The labels to apply to the container image.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers3">launchers</a>
        </td>
        <td>List</td>
        <td>
            A list of extra launchers to include in the resulting application.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory3">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.legalDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/legal/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#licenseFile3">licenseFile</a>
        </td>
        <td>File</td>
        <td>
            The path to the application license file.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.licenseFile
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/LICENSE
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#linuxConfiguration3">linuxConfiguration</a>
        </td>
        <td>LinuxConfiguration</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration3">macOSConfiguration</a>
        </td>
        <td>MacOSConfiguration</td>
        <td>
            MacOS specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#manDirectory3">manDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing man pages that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.manDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/man/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#overWriteIfNewer3">overWriteIfNewer</a>
        </td>
        <td>boolean</td>
        <td>
            Overwrite dependencies that don't exist or are older than the source.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.overWriteIfNewer
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ports2">ports</a>
        </td>
        <td>Set</td>
        <td>
            The ports exposed by the container at runtime defined as: port_number [ '/' udp/tcp ]
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#projectMainClass3">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#registry2">registry</a>
        </td>
        <td>String</td>
        <td>
            The registry part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.registry
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#repository2">repository</a>
        </td>
        <td>String</td>
        <td>
            The repository part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.repository
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resolveProjectMainClass3">resolveProjectMainClass</a>
        </td>
        <td>boolean</td>
        <td>
            Resolve the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resourceDirectory3">resourceDirectory</a>
        </td>
        <td>File</td>
        <td>
            The path to resources that override resulting package resources.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.resourceDirectory
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#skip3">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the generation of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripDebug3">stripDebug</a>
        </td>
        <td>boolean</td>
        <td>
            Strip debug information from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripDebug
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripNativeCommands3">stripNativeCommands</a>
        </td>
        <td>boolean</td>
        <td>
            Strip native command (eg. java...) from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripNativeCommands
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#user2">user</a>
        </td>
        <td>String</td>
        <td>
            The user and group used to run the container defined as: user / uid [ ':' group / gid ]
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vendor3">vendor</a>
        </td>
        <td>String</td>
        <td>
            The application vendor.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.vendor
                </li>
                <li>
                    <em>Default</em>
                    : ${project.organization.name}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose3">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vm3">vm</a>
        </td>
        <td>String</td>
        <td>
            Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#volumes2">volumes</a>
        </td>
        <td>Set</td>
        <td>
            The container's mount points.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration3">windowsConfiguration</a>
        </td>
        <td>WindowsConfiguration</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addModules&gt;

The modules to add to the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addOptions


##### &lt;attach&gt;

Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.image.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Link in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;description&gt;

The description of the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.description
- **Default**: ${project.description}


##### &lt;environment&gt;

The container's environment variables.

- **Type**: java.util.Map
- **Required**: no


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeScope


##### &lt;executable&gt;

The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.app.executable
- **Default**: ${project.artifactId}


##### &lt;formats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set
- **Required**: yes
- **Default**: zip


##### &lt;from&gt;

The base container image.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.from
- **Default**: debian:buster-slim


##### &lt;ignoreSigningInformation&gt;

Suppress a fatal error when signed modular JARs are linked in the image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.ignoreSigningInformation
- **Default**: false


##### &lt;imageFormat&gt;

The format of the container image.

- **Type**: com.google.cloud.tools.jib.api.buildplan.ImageFormat
- **Required**: no
- **User property**: inverno.container.imageFormat
- **Default**: Docker


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma Separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
-	runtime scope gives runtime and compile dependencies,
-	compile scope gives compile, provided, and system dependencies,
-	test (default) scope gives all dependencies,
-	provided scope just gives provided dependencies,
-	system scope just gives system dependencies.


- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as 'Program Files' or 'AppData' on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;jmodsOverrideDirectory&gt;

A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.jmodsOverrideDirectory
- **Default**: ${project.basedir}/src/jmods/


##### &lt;labels&gt;

The labels to apply to the container image.

- **Type**: java.util.Map
- **Required**: no


##### &lt;launchers&gt;

A list of extra launchers to include in the resulting application.

- **Type**: java.util.List
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$LinuxConfiguration
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$MacOSConfiguration
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;overWriteIfNewer&gt;

Overwrite dependencies that don't exist or are older than the source.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.overWriteIfNewer
- **Default**: true


##### &lt;ports&gt;

The ports exposed by the container at runtime defined as: port_number [ '/' udp/tcp ]

- **Type**: java.util.Set
- **Required**: no


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.projectMainClass


##### &lt;registry&gt;

The registry part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registry


##### &lt;repository&gt;

The repository part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.repository


##### &lt;resolveProjectMainClass&gt;

Resolve the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.projectMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the generation of the application.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.skip


##### &lt;stripDebug&gt;

Strip debug information from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strip native command (eg. java...) from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripNativeCommands
- **Default**: true


##### &lt;user&gt;

The user and group used to run the container defined as: user / uid [ ':' group / gid ]

- **Type**: java.lang.String
- **Required**: no


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false


##### &lt;vm&gt;

Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.vm


##### &lt;volumes&gt;

The container's mount points.

- **Type**: java.util.Set
- **Required**: no


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.internal.task.CreateProjectApplicationTask$WindowsConfiguration
- **Required**: no


### inverno:build-runtime

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:build-runtime

**Description:**

Builds the project runtime image.

A runtime image is a custom Java runtime containing a set of modules and their dependencies.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.0.
- Binds by default to the lifecycle phase: package.


#### Required parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#attach4">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.attach
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#formats4">formats</a>
        </td>
        <td>Set</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>Default</em>
                    : zip
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#addModules4">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addModules
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addOptions4">addOptions</a>
        </td>
        <td>String</td>
        <td>
            The options to prepend before any other options when invoking the JVM in the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#bindServices4">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Link in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.bindServices
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#compress4">compress</a>
        </td>
        <td>String</td>
        <td>
            The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.compress
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#configurationDirectory4">configurationDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing user-editable configuration files that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.configurationDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/conf/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeArtifactIds4">excludeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeClassifiers4">excludeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : excludeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeGroupIds4">excludeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupId Names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#excludeScope4">excludeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to exclude. An Empty string indicates no scopes (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.excludeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ignoreSigningInformation4">ignoreSigningInformation</a>
        </td>
        <td>boolean</td>
        <td>
            Suppress a fatal error when signed modular JARs are linked in the image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.ignoreSigningInformation
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeArtifactIds4">includeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeArtifactIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeClassifiers4">includeClassifiers</a>
        </td>
        <td>String</td>
        <td>
            Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeClassifiers
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeGroupIds4">includeGroupIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of GroupIds to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeGroupIds
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#includeScope4">includeScope</a>
        </td>
        <td>String</td>
        <td>
            Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.includeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#jmodsOverrideDirectory4">jmodsOverrideDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.jmodsOverrideDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/jmods/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers4">launchers</a>
        </td>
        <td>List</td>
        <td>
            A list of launchers to include in the resulting runtime.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory4">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.legalDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/legal/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#manDirectory4">manDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing man pages that will be copied to the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.manDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/man/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#overWriteIfNewer4">overWriteIfNewer</a>
        </td>
        <td>boolean</td>
        <td>
            Overwrite dependencies that don't exist or are older than the source.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.overWriteIfNewer
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#projectMainClass4">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#resolveProjectMainClass4">resolveProjectMainClass</a>
        </td>
        <td>boolean</td>
        <td>
            Resolve the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.projectMainClass
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#skip4">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the generation of the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripDebug4">stripDebug</a>
        </td>
        <td>boolean</td>
        <td>
            Strip debug information from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripDebug
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#stripNativeCommands4">stripNativeCommands</a>
        </td>
        <td>boolean</td>
        <td>
            Strip native command (eg. java...) from the resulting image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.stripNativeCommands
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose4">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vm4">vm</a>
        </td>
        <td>String</td>
        <td>
            Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.vm
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addModules&gt;

The modules to add to the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting image.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.addOptions


##### &lt;attach&gt;

Attach the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.image.attach
- **Default**: true


##### &lt;bindServices&gt;

Link in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.excludeScope


##### &lt;formats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set
- **Required**: yes
- **Default**: zip


##### &lt;ignoreSigningInformation&gt;

Suppress a fatal error when signed modular JARs are linked in the image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.ignoreSigningInformation
- **Default**: false


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma Separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary:
-	runtime scope gives runtime and compile dependencies,
-	compile scope gives compile, provided, and system dependencies,
-	test (default) scope gives all dependencies,
-	provided scope just gives provided dependencies,
-	system scope just gives system dependencies.


- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.includeScope


##### &lt;jmodsOverrideDirectory&gt;

A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.jmodsOverrideDirectory
- **Default**: ${project.basedir}/src/jmods/


##### &lt;launchers&gt;

A list of launchers to include in the resulting runtime.

- **Type**: java.util.List
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting image.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.image.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;overWriteIfNewer&gt;

Overwrite dependencies that don't exist or are older than the source.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.overWriteIfNewer
- **Default**: true


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.projectMainClass


##### &lt;resolveProjectMainClass&gt;

Resolve the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.projectMainClass
- **Default**: false


##### &lt;skip&gt;

Skips the generation of the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.skip


##### &lt;stripDebug&gt;

Strip debug information from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strip native command (eg. java...) from the resulting image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.stripNativeCommands
- **Default**: true


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false


##### &lt;vm&gt;

Select the HotSpot VM in the output image defined as: 'client' / 'server' / 'minimal' / 'all'

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.image.vm


### inverno:help

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:help

**Description:**

Display help information on inverno-maven-plugin.
Call mvn inverno:help -Ddetail=true -Dgoal=&lt;goal-name&gt; to display parameter details.

**Attributes:**



#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#detail">detail</a>
        </td>
        <td>boolean</td>
        <td>
            If true, display all settable properties for each goal.
            <ul>
                <li>
                    <em>User property</em>
                    : detail
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#goal">goal</a>
        </td>
        <td>String</td>
        <td>
            The name of the goal for which to show help. If unspecified, all goals will be displayed.
            <ul>
                <li>
                    <em>User property</em>
                    : goal
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#indentSize">indentSize</a>
        </td>
        <td>int</td>
        <td>
            The number of spaces per indentation level, should be positive.
            <ul>
                <li>
                    <em>User property</em>
                    : indentSize
                </li>
                <li>
                    <em>Default</em>
                    : 2
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#lineLength">lineLength</a>
        </td>
        <td>int</td>
        <td>
            The maximum length of a display line, should be positive.
            <ul>
                <li>
                    <em>User property</em>
                    : lineLength
                </li>
                <li>
                    <em>Default</em>
                    : 80
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;detail&gt;

If true, display all settable properties for each goal.

- **Type**: boolean
- **Required**: no
- **User property**: detail
- **Default**: false


##### &lt;goal&gt;

The name of the goal for which to show help. If unspecified, all goals will be displayed.

- **Type**: java.lang.String
- **Required**: no
- **User property**: goal


##### &lt;indentSize&gt;

The number of spaces per indentation level, should be positive.

- **Type**: int
- **Required**: no
- **User property**: indentSize
- **Default**: 2


##### &lt;lineLength&gt;

The maximum length of a display line, should be positive.

- **Type**: int
- **Required**: no
- **User property**: lineLength
- **Default**: 80


### inverno:run

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:run

**Description:**

Runs the project application.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.0.
- Binds by default to the lifecycle phase: validate.


#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#addUnnamedModules">addUnnamedModules</a>
        </td>
        <td>boolean</td>
        <td>
            Adds the unnamed modules when executing the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.addUnnamedModules
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#arguments">arguments</a>
        </td>
        <td>String</td>
        <td>
            The arguments to pass to the application.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#commandLineArguments">commandLineArguments</a>
        </td>
        <td>String</td>
        <td>
            The command line arguments to pass to the application. This parameter overrides AbstractExecMojo.arguments when specified.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.run.arguments
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#configurationDirectory5">configurationDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing user-editable configuration files that will be copied to the image to execute.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.configurationDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/conf/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#jmodsOverrideDirectory5">jmodsOverrideDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.jmodsOverrideDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/jmods/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#mainClass">mainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class to use to run the application. If not specified, a main class is automatically selected.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.mainClass
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#overWriteIfNewer5">overWriteIfNewer</a>
        </td>
        <td>boolean</td>
        <td>
            Overwrites dependencies that don't exist or are older than the source.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.overWriteIfNewer
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#skip5">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the execution.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose5">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vmOptions">vmOptions</a>
        </td>
        <td>String</td>
        <td>
            The VM options to use when executing the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.vmOptions
                </li>
                <li>
                    <em>Default</em>
                    : -Dorg.apache.logging.log4j.simplelog.level=INFO -Dorg.apache.logging.log4j.level=INFO
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#workingDirectory">workingDirectory</a>
        </td>
        <td>File</td>
        <td>
            The working directory of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.run.workingDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.build.directory}/maven-inverno/working
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addUnnamedModules&gt;

Adds the unnamed modules when executing the application.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.exec.addUnnamedModules
- **Default**: true


##### &lt;arguments&gt;

The arguments to pass to the application.

- **Type**: java.lang.String
- **Required**: no


##### &lt;commandLineArguments&gt;

The command line arguments to pass to the application. This parameter overrides AbstractExecMojo.arguments when specified.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.run.arguments


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the image to execute.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.exec.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;jmodsOverrideDirectory&gt;

A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.exec.jmodsOverrideDirectory
- **Default**: ${project.basedir}/src/jmods/


##### &lt;mainClass&gt;

The main class to use to run the application. If not specified, a main class is automatically selected.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.mainClass


##### &lt;overWriteIfNewer&gt;

Overwrites dependencies that don't exist or are older than the source.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.exec.overWriteIfNewer
- **Default**: true


##### &lt;skip&gt;

Skips the execution.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.exec.skip


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false


##### &lt;vmOptions&gt;

The VM options to use when executing the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.vmOptions
- **Default**: -Dorg.apache.logging.log4j.simplelog.level=INFO -Dorg.apache.logging.log4j.level=INFO


##### &lt;workingDirectory&gt;

The working directory of the application.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.run.workingDirectory
- **Default**: ${project.build.directory}/maven-inverno/working


### inverno:start

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:start

**Description:**

Starts the project application without blocking the Maven build.

This goal is used together with the stop goal in the pre-integration-test and post-integration-test phases to run integration tests.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.0.
- Binds by default to the lifecycle phase: pre-integration-test.


#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#addUnnamedModules1">addUnnamedModules</a>
        </td>
        <td>boolean</td>
        <td>
            Adds the unnamed modules when executing the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.addUnnamedModules
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#arguments1">arguments</a>
        </td>
        <td>String</td>
        <td>
            The arguments to pass to the application.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#configurationDirectory6">configurationDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing user-editable configuration files that will be copied to the image to execute.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.configurationDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/main/conf/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#jmodsOverrideDirectory6">jmodsOverrideDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.jmodsOverrideDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/jmods/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#mainClass1">mainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class to use to run the application. If not specified, a main class is automatically selected.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.mainClass
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#overWriteIfNewer6">overWriteIfNewer</a>
        </td>
        <td>boolean</td>
        <td>
            Overwrites dependencies that don't exist or are older than the source.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.overWriteIfNewer
                </li>
                <li>
                    <em>Default</em>
                    : true
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#skip6">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the execution.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#timeout">timeout</a>
        </td>
        <td>long</td>
        <td>
            The amount of time in milliseconds to wait for the application to start.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.start.timeout
                </li>
                <li>
                    <em>Default</em>
                    : 60000
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose6">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vmOptions1">vmOptions</a>
        </td>
        <td>String</td>
        <td>
            The VM options to use when executing the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.vmOptions
                </li>
                <li>
                    <em>Default</em>
                    : -Dorg.apache.logging.log4j.simplelog.level=INFO -Dorg.apache.logging.log4j.level=INFO
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#workingDirectory1">workingDirectory</a>
        </td>
        <td>File</td>
        <td>
            The working directory of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.run.workingDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.build.directory}/maven-inverno/working
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addUnnamedModules&gt;

Adds the unnamed modules when executing the application.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.exec.addUnnamedModules
- **Default**: true


##### &lt;arguments&gt;

The arguments to pass to the application.

- **Type**: java.lang.String
- **Required**: no


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the image to execute.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.exec.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;jmodsOverrideDirectory&gt;

A directory containing module descriptors to use to modularize unnamed dependency modules and which override the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.exec.jmodsOverrideDirectory
- **Default**: ${project.basedir}/src/jmods/


##### &lt;mainClass&gt;

The main class to use to run the application. If not specified, a main class is automatically selected.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.mainClass


##### &lt;overWriteIfNewer&gt;

Overwrites dependencies that don't exist or are older than the source.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.exec.overWriteIfNewer
- **Default**: true


##### &lt;skip&gt;

Skips the execution.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.exec.skip


##### &lt;timeout&gt;

The amount of time in milliseconds to wait for the application to start.

- **Type**: long
- **Required**: no
- **User property**: inverno.start.timeout
- **Default**: 60000


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false


##### &lt;vmOptions&gt;

The VM options to use when executing the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.vmOptions
- **Default**: -Dorg.apache.logging.log4j.simplelog.level=INFO -Dorg.apache.logging.log4j.level=INFO


##### &lt;workingDirectory&gt;

The working directory of the application.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.run.workingDirectory
- **Default**: ${project.build.directory}/maven-inverno/working


### inverno:stop

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.2.4:stop

**Description:**

Stops the project application that has been previously started using the start goal.

This goal is used together with the start goal in the pre-integration-test and post-integration-test phases to run integration tests.


**Attributes:**

- Requires a Maven project to be executed.
- Since version: 1.0.
- Binds by default to the lifecycle phase: post-integration-test.


#### Optional parameters

<table>
    <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>
            <a href="#skip7">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the execution.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.stop.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#timeout1">timeout</a>
        </td>
        <td>long</td>
        <td>
            The amount of time in milliseconds to wait for the application to stop.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.stop.timeout
                </li>
                <li>
                    <em>Default</em>
                    : 60000
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#verbose7">verbose</a>
        </td>
        <td>boolean</td>
        <td>
            Enables verbose logging.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.verbose
                </li>
                <li>
                    <em>Default</em>
                    : false
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;skip&gt;

Skips the execution.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.stop.skip


##### &lt;timeout&gt;

The amount of time in milliseconds to wait for the application to stop.

- **Type**: long
- **Required**: no
- **User property**: inverno.stop.timeout
- **Default**: 60000


##### &lt;verbose&gt;

Enables verbose logging.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.verbose
- **Default**: false

