
[inverno-tool-build-tools]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-build-tools

# io.inverno.tool.maven
The Inverno Maven Plugin is used to run, package and distribute modular applications and Inverno applications in particular. It relies on a set of Java tools to build native runtime or application images as well as Docker or OCI images for modular Java projects.

## Usage


Considering a modular application project, the Inverno Maven plugin is used to run, start, stop a the application or build project images. There are three types of images that can be built using the plugin:

- **runtime image** is a custom Java runtime containing a set of modules and their dependencies.
- **application image** is a native self-contained Java application including all the necessary dependencies to run the project application without the need of a Java runtime. 
- **container image** is a Docker or OCI container image that can be packaged as a `.tar` archive or directly loaded on a Docker daemon or pushed to a container registry.

The plugin is a Maven implementation of the [Inverno Build Tools][inverno-tool-build-tools], it can be used to build any Java modular application project and Inverno application in particular.

### Run a module application project

The `inverno:run` goal is used to execute the modular application defined in the project from the command line.

```plaintext
$ mvn inverno:run
```

The application is first *modularized* which means that any non-modular dependency is modularized by generating an appropriate module descriptor in order for the application to run with a module path and not a class path (and certainly not both).

The application is executed in a forked process, application arguments can be passed on the command line as follows:

```plaintext
$ mvn inverno:run -Dinverno.run.arguments='--some.configuration=\"hello\"'
```

> Actual arguments are determined by splitting the parameter value around spaces. There are several options to declare an argument which contains spaces:
> - it can be escaped: `Hello\ World` 
> - it can be quoted: `"Hello World"` or `'Hello World'`
>
> Since quotes or double quotes are used as delimiters, they might need to be escaped as well to declare an argument that contains some: `I\'m\ happy`, `"I'm happy"`, `'I\'m happy'`.

> The way quotes are escaped greatly depends on the operating system. Above examples refers to Unix systems with proper shells, please look for the right documentation if you are using a different one.

VM options can be specified as follows:

```plaintext
$ mvn inverno:run -Dinverno.exec.vmOptions="-Xms2G -Xmx2G"
```

By default the plugin will detect the main class of the application, but it is also possible to specify it explicitly in case multiple main classes exist in the project module.

```plaintext
$ mvn inverno:run -Dinverno.exec.mainClass=io.inverno.example.Main
```

> When building an Inverno application, a pidfile is normally created when the application is started under `${project.build.directory}/maven-inverno` directory, it indicates the pid of the process running the application. If the build exits while the application is still running or if the pidfile was not properly removed after the application has exited, it might be necessary to manually kill the process and/or remove the pidfile. 

### Debug a module application project

The `inverno:debug` goal is used to execute the modular application defined in the project from the command line with JVM debug options (e.g. `-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=localhost:8000`).

```plaintext
$ mvn inverno:debug
...
Listening for transport dt_socket at address: 8000
...
```

This goal is similar to the `inverno:run` goal and accepts the same arguments:

```plaintext
$ mvn inverno:debug -Dinverno.exec.vmOptions="-Xms2G -Xmx2G" -Dinverno.exec.mainClass="io.inverno.example.Main" -Dinverno.debug.arguments='--some.configuration=\"hello\"'
```

The debug port and whether to suspend or not the execution until a debugger is attached can also be specified:

```plaintext
$ mvn inverno:debug -Dinverno.debug.port=9000 -Dinverno.debug.suspend=false
```

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

In order to detect when the application has started, the start goal waits for a pidfile containing the application pid to be created by the application. If the application doesn't create that pidfile, the goal eventually times out and the build fails.

### Build a runtime image

A runtime image is a custom Java runtime distribution containing specific modules and their dependencies. Such image is used as a base for generating application image but it can also be distributed as a lightweight Java runtime specific to the project module.

The `inverno:build-runtime` goal assemble the project module and its dependencies.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>build-runtime</id>
                        <phase>package</phase>
                        <goals>
                            <goal>build-runtime</goal>
                        </goals>
                        <configuration>
                            <vm>server</vm>
                            <addModules>jdk.jdwp.agent,jdk.crypto.ec</addModules>
                            <vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
                            <archiveFormats>
                                <archiveFormat>zip</archiveFormat>
                                <archiveFormat>tar.gz</archiveFormat>
                                <archiveFormat>tar.bz2</archiveFormat>
                            </archiveFormats>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

By default, the project module and its dependencies are included in the resulting image, this include JDK's modules such as `java.base`, in the previous example we've also explicitly added the `jdk.jdwp.agent` to support remote debugging and `jdk.crypto.ec` to support TLS communications.

The resulting image is packaged to the formats defined in the configuration and attached, by default, to the Maven project as a result they are installed and published along with the project `.jar`.

### Package the application

An application image is built using the `inverno:package-app` goal which generates a native platform-specific application package.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>package-application</id>
                        <phase>package</phase>
                        <goals>
                            <goal>package-app</goal>
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
                            <packageTypes>
                            	<packageType>deb</packageType>
                            </packageTypes>
                            <archiveFormat>
                                <archiveFormat>zip</archiveFormat>
                            </archiveFormat>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

The `inverno:build-app` goal is very similar to the `inverno:build-runtime` goal except that the resulting image provides a native application launcher and it can be packaged in a platform-specific format. For instance, we can generate a `.deb` on a Linux platform or a `.exe` or `.msi` installer on a Windows platform or a `.dmg` on a MacOS platform. The resulting package can be installed on these platforms in a standard way.

> This goal uses `jpackage` tool which is an incubating feature in JDK&lt;16, if you intend to build an application image with an old JDK, you'll need to explicitly add the `jdk.incubator.jpackage` module in `MAVEN_OPTS`:
> ```plaintext
> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
> ```

### Build the container image

A container image can be built in a TAR archive using the `inverno:package-image` goal which basically build an application image and package it in a container image.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>package-image</id>
                        <phase>package</phase>
                        <goals>
                            <goal>package-image</goal>
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

### Install the container image to a Docker daemon

The `inverno:install-image` goal is used to build a container image and load it to a Docker daemon using the Docker CLI.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>install-image</id>
                        <phase>install</phase>
                        <goals>
                            <goal>install-image</goal>
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

### Deploy the container image to a registry

The `inverno:deploy-image` goal builds a container image and deploy it to an image registry.

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.inverno.tool</groupId>
                <artifactId>inverno-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>deploy-image</id>
                        <phase>deploy</phase>
                        <goals>
                            <goal>deploy-image</goal>
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

By default the registry points to the Docker hub `docker.io` but another registry can be specified, `gcr.io` in our example.

> As for `build-app` goal, this goal uses `jpackage` tool so if you intend to use a JDK&lt;16 you'll need to explicitly add the `jdk.incubator.jpackage` module in `MAVEN_OPTS`:
> ```plaintext
> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
> ```



## Goals

### Overview

- [inverno:build-runtime](#invernobuild-runtime) Builds the project runtime image.
- [inverno:debug](#invernodebug) Debugs the project application.
- [inverno:deploy-image](#invernodeploy-image) Builds and deploys the project application container image to an image registry.
- [inverno:help](#invernohelp) Display help information on inverno-maven-plugin. 
- [inverno:install-image](#invernoinstall-image) Builds and installs the project application container image to the local Docker daemon.
- [inverno:package-app](#invernopackage-app) Builds and packages the project application image.
- [inverno:package-image](#invernopackage-image) Builds and packages the project application container image in a TAR archive.
- [inverno:run](#invernorun) Runs the project application.
- [inverno:start](#invernostart) Starts the project application without blocking the Maven build.
- [inverno:stop](#invernostop) Stops the project application that has been previously started using the start goal.

### inverno:build-runtime

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:build-runtime

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
            <a href="#archiveFormats">archiveFormats</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archiveFormats
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#attach">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.attach
                </li>
                <li>
                    <em>Default</em>
                    : true
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
            The modules to add to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addModules
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
            The options to prepend before any other options when invoking the JVM in the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addUnnamedModules">addUnnamedModules</a>
        </td>
        <td>boolean</td>
        <td>
            Adds unnamed modules when generating the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addUnnamedModules
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
            <a href="#archivePrefix">archivePrefix</a>
        </td>
        <td>String</td>
        <td>
            The path to the runtime image within the archive.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archivePrefix
                </li>
                <li>
                    <em>Default</em>
                    : ${project.build.finalName}
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
            Links in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.bindServices
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
                    : inverno.runtime.compress
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
            A directory containing user-editable configuration files that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.configurationDirectory
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
            <a href="#excludeArtifactIds">excludeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.excludeArtifactIds
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
            Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.excludeClassifiers
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
                    : inverno.runtime.excludeGroupIds
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
                    : inverno.runtime.excludeScope
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
            Suppresses a fatal error when signed modular JARs are linked in the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.ignoreSigningInformation
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
                    : inverno.runtime.includeArtifactIds
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
            Comma separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.includeClassifiers
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
                    : inverno.runtime.includeGroupIds
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
                    : inverno.runtime.includeScope
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers">launchers</a>
        </td>
        <td>RuntimeLauncherParameters&gt;</td>
        <td>
            A list of launchers to include in the resulting runtime.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.legalDirectory
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
            <a href="#manDirectory">manDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing man pages that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.manDirectory
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
            <a href="#moduleOverrides">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
                    : inverno.runtime.mainClass
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
            Resolves the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.resolveMainClass
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
            <a href="#skip">skip</a>
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
            <a href="#stripDebug">stripDebug</a>
        </td>
        <td>boolean</td>
        <td>
            Strips debug information from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripDebug
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
            Strips native command (e.g. java...) from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripNativeCommands
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
            <a href="#vm">vm</a>
        </td>
        <td>String</td>
        <td>
            Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.vm
                </li>
            </ul>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;addModules&gt;

The modules to add to the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addOptions


##### &lt;addUnnamedModules&gt;

Adds unnamed modules when generating the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.addUnnamedModules
- **Default**: true


##### &lt;archiveFormats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: yes
- **User property**: inverno.runtime.archiveFormats


##### &lt;archivePrefix&gt;

The path to the runtime image within the archive.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.archivePrefix
- **Default**: ${project.build.finalName}


##### &lt;attach&gt;

Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.attach
- **Default**: true


##### &lt;bindServices&gt;

Links in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeScope


##### &lt;ignoreSigningInformation&gt;

Suppresses a fatal error when signed modular JARs are linked in the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.ignoreSigningInformation
- **Default**: false


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary: 
* runtime scope gives runtime and compile dependencies, 
* compile scope gives compile, provided, and system dependencies, 
* test (default) scope gives all dependencies, 
* provided scope just gives provided dependencies, 
* system scope just gives system dependencies. 

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeScope


##### &lt;launchers&gt;

A list of launchers to include in the resulting runtime.

- **Type**: java.util.List&lt;io.inverno.tool.maven.RuntimeLauncherParameters&gt;
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.mainClass


##### &lt;resolveProjectMainClass&gt;

Resolves the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.resolveMainClass
- **Default**: false


##### &lt;skip&gt;

Skips the generation of the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.skip


##### &lt;stripDebug&gt;

Strips debug information from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strips native command (e.g. java...) from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripNativeCommands
- **Default**: true


##### &lt;vm&gt;

Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.vm


### inverno:debug

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:debug

**Description:**


Debugs the project application.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.4.
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
            The command line arguments to pass to the application. This parameter overrides arguments when specified.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.debug.arguments
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
            The main class to use to run the application. If not specified, one of the main class in the project module is automatically selected.
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
            <a href="#moduleOverrides1">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory1">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#port">port</a>
        </td>
        <td>int</td>
        <td>
            The debug port.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.debug.port
                </li>
                <li>
                    <em>Default</em>
                    : 8000
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar1">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
            <a href="#skip1">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the execution.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.debug.skip
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#suspend">suspend</a>
        </td>
        <td>boolean</td>
        <td>
            Indicates whether to suspend execution until a debugger is attached.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.debug.suspend
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
                    : -Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO
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
                    : inverno.exec.workingDirectory
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

The command line arguments to pass to the application. This parameter overrides arguments when specified.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.debug.arguments


##### &lt;mainClass&gt;

The main class to use to run the application. If not specified, one of the main class in the project module is automatically selected.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.mainClass


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;port&gt;

The debug port.

- **Type**: int
- **Required**: no
- **User property**: inverno.debug.port
- **Default**: 8000


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;skip&gt;

Skips the execution.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.debug.skip


##### &lt;suspend&gt;

Indicates whether to suspend execution until a debugger is attached.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.debug.suspend
- **Default**: true


##### &lt;vmOptions&gt;

The VM options to use when executing the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.vmOptions
- **Default**: -Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO


##### &lt;workingDirectory&gt;

The working directory of the application.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.exec.workingDirectory
- **Default**: ${project.build.directory}/maven-inverno/working


### inverno:deploy-image

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:deploy-image

**Description:**


Builds and deploys the project application container image to an image registry.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.4.
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
            <a href="#archiveFormats1">archiveFormats</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archiveFormats
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#attach1">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.attach
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
                    : inverno.container.executable
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
            <a href="#aboutURL">aboutURL</a>
        </td>
        <td>String</td>
        <td>
            The application's home page URL.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.aboutURL
                </li>
                <li>
                    <em>Default</em>
                    : ${project.url}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addModules1">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addModules
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
            The options to prepend before any other options when invoking the JVM in the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addUnnamedModules2">addUnnamedModules</a>
        </td>
        <td>boolean</td>
        <td>
            Adds unnamed modules when generating the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addUnnamedModules
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
            <a href="#archivePrefix1">archivePrefix</a>
        </td>
        <td>String</td>
        <td>
            The path to the runtime image within the archive.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archivePrefix
                </li>
                <li>
                    <em>Default</em>
                    : ${project.build.finalName}
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
            <a href="#bindServices1">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Links in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.bindServices
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
                    : inverno.runtime.compress
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
            A directory containing user-editable configuration files that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.configurationDirectory
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
            <a href="#contentFiles">contentFiles</a>
        </td>
        <td>File&gt;</td>
        <td>
            Files to add to the application payload.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.contentFiles
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
            <a href="#environment">environment</a>
        </td>
        <td>String&gt;</td>
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
                    : inverno.runtime.excludeArtifactIds
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
            Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.excludeClassifiers
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
                    : inverno.runtime.excludeGroupIds
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
                    : inverno.runtime.excludeScope
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
            Suppresses a fatal error when signed modular JARs are linked in the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.ignoreSigningInformation
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
        <td>Format</td>
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
                    : inverno.runtime.includeArtifactIds
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
            Comma separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.includeClassifiers
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
                    : inverno.runtime.includeGroupIds
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
                    : inverno.runtime.includeScope
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
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.
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
            <a href="#labels">labels</a>
        </td>
        <td>String&gt;</td>
        <td>
            The labels to apply to the container image.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers1">launchers</a>
        </td>
        <td>ApplicationLauncherParameters&gt;</td>
        <td>
            The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory1">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.legalDirectory
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
        <td>LinuxConfigurationParameters</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration">macOSConfiguration</a>
        </td>
        <td>MacOSConfigurationParameters</td>
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
            A directory containing man pages that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.manDirectory
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
            <a href="#moduleOverrides2">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory2">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#packageTypes">packageTypes</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.packageTypes
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ports">ports</a>
        </td>
        <td>String&gt;</td>
        <td>
            The ports exposed by the container at runtime defined as: port_number [ "/" udp/tcp ] .
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar2">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
            <a href="#projectMainClass1">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.mainClass
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
                    : inverno.container.registryPassword
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
                    : inverno.container.registryUsername
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
            Resolves the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.resolveMainClass
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
            <a href="#skip2">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the build and deployment of the container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.install.skip
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
            Strips debug information from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripDebug
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
            Strips native command (e.g. java...) from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripNativeCommands
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
            The user and group used to run the container defined as: user / uid [ ":" group / gid ].
            <ul/>
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
            <a href="#vm1">vm</a>
        </td>
        <td>String</td>
        <td>
            Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#volumes">volumes</a>
        </td>
        <td>String&gt;</td>
        <td>
            The container's mount points.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration">windowsConfiguration</a>
        </td>
        <td>WindowsConfigurationParameters</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;aboutURL&gt;

The application's home page URL.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.aboutURL
- **Default**: ${project.url}


##### &lt;addModules&gt;

The modules to add to the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addOptions


##### &lt;addUnnamedModules&gt;

Adds unnamed modules when generating the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.addUnnamedModules
- **Default**: true


##### &lt;archiveFormats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: yes
- **User property**: inverno.runtime.archiveFormats


##### &lt;archivePrefix&gt;

The path to the runtime image within the archive.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.archivePrefix
- **Default**: ${project.build.finalName}


##### &lt;attach&gt;

Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Links in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;contentFiles&gt;

Files to add to the application payload.

- **Type**: java.util.List&lt;java.io.File&gt;
- **Required**: no
- **User property**: inverno.app.contentFiles


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;environment&gt;

The container's environment variables.

- **Type**: java.util.Map&lt;java.lang.String, java.lang.String&gt;
- **Required**: no


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeScope


##### &lt;executable&gt;

The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.executable
- **Default**: ${project.artifactId}


##### &lt;from&gt;

The base container image.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.from
- **Default**: debian:buster-slim


##### &lt;ignoreSigningInformation&gt;

Suppresses a fatal error when signed modular JARs are linked in the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.ignoreSigningInformation
- **Default**: false


##### &lt;imageFormat&gt;

The format of the container image.

- **Type**: io.inverno.tool.buildtools.ContainerizeTask$Format
- **Required**: no
- **User property**: inverno.container.imageFormat
- **Default**: Docker


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary: 
* runtime scope gives runtime and compile dependencies, 
* compile scope gives compile, provided, and system dependencies, 
* test (default) scope gives all dependencies, 
* provided scope just gives provided dependencies, 
* system scope just gives system dependencies. 

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;labels&gt;

The labels to apply to the container image.

- **Type**: java.util.Map&lt;java.lang.String, java.lang.String&gt;
- **Required**: no


##### &lt;launchers&gt;

The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ApplicationLauncherParameters&gt;
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.LinuxConfigurationParameters
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.MacOSConfigurationParameters
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;packageTypes&gt;

A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no
- **User property**: inverno.app.packageTypes


##### &lt;ports&gt;

The ports exposed by the container at runtime defined as: port_number [ "/" udp/tcp ] .

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.mainClass


##### &lt;registry&gt;

The registry part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registry


##### &lt;registryPassword&gt;

The password to use to authenticate to the registry.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registryPassword


##### &lt;registryUsername&gt;

The user name to use to authenticate to the registry.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.registryUsername


##### &lt;repository&gt;

The repository part of the target image reference defined as: ${registry}/${repository}/${name}:${project.version}

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.container.repository


##### &lt;resolveProjectMainClass&gt;

Resolves the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.resolveMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the build and deployment of the container image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.install.skip


##### &lt;stripDebug&gt;

Strips debug information from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strips native command (e.g. java...) from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripNativeCommands
- **Default**: true


##### &lt;user&gt;

The user and group used to run the container defined as: user / uid [ ":" group / gid ].

- **Type**: java.lang.String
- **Required**: no


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;vm&gt;

Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.vm


##### &lt;volumes&gt;

The container's mount points.

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.WindowsConfigurationParameters
- **Required**: no


### inverno:help

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:help

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


### inverno:install-image

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:install-image

**Description:**


Builds and installs the project application container image to the local Docker daemon.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.4.
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
            <a href="#archiveFormats2">archiveFormats</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archiveFormats
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#attach2">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.attach
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
                    : inverno.container.executable
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
            <a href="#aboutURL1">aboutURL</a>
        </td>
        <td>String</td>
        <td>
            The application's home page URL.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.aboutURL
                </li>
                <li>
                    <em>Default</em>
                    : ${project.url}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addModules2">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addModules
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
            The options to prepend before any other options when invoking the JVM in the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addUnnamedModules3">addUnnamedModules</a>
        </td>
        <td>boolean</td>
        <td>
            Adds unnamed modules when generating the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addUnnamedModules
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
            <a href="#archivePrefix2">archivePrefix</a>
        </td>
        <td>String</td>
        <td>
            The path to the runtime image within the archive.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archivePrefix
                </li>
                <li>
                    <em>Default</em>
                    : ${project.build.finalName}
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
            <a href="#bindServices2">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Links in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.bindServices
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
                    : inverno.runtime.compress
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
            A directory containing user-editable configuration files that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.configurationDirectory
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
            <a href="#contentFiles1">contentFiles</a>
        </td>
        <td>File&gt;</td>
        <td>
            Files to add to the application payload.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.contentFiles
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
            <a href="#dockerEnvironment">dockerEnvironment</a>
        </td>
        <td>String&gt;</td>
        <td>
            The Docker environment variables used by the Docker CLI executable.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#dockerExecutableFile">dockerExecutableFile</a>
        </td>
        <td>File</td>
        <td>
            The path to the Docker CLI executable used to load the image in the Docker daemon.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.container.dockerExecutable
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#environment1">environment</a>
        </td>
        <td>String&gt;</td>
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
                    : inverno.runtime.excludeArtifactIds
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
            Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.excludeClassifiers
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
                    : inverno.runtime.excludeGroupIds
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
                    : inverno.runtime.excludeScope
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
            Suppresses a fatal error when signed modular JARs are linked in the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.ignoreSigningInformation
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
        <td>Format</td>
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
                    : inverno.runtime.includeArtifactIds
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
            Comma separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.includeClassifiers
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
                    : inverno.runtime.includeGroupIds
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
                    : inverno.runtime.includeScope
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
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.
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
            <a href="#labels1">labels</a>
        </td>
        <td>String&gt;</td>
        <td>
            The labels to apply to the container image.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers2">launchers</a>
        </td>
        <td>ApplicationLauncherParameters&gt;</td>
        <td>
            The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory2">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.legalDirectory
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
        <td>LinuxConfigurationParameters</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration1">macOSConfiguration</a>
        </td>
        <td>MacOSConfigurationParameters</td>
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
            A directory containing man pages that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.manDirectory
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
            <a href="#moduleOverrides3">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory3">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#packageTypes1">packageTypes</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.packageTypes
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ports1">ports</a>
        </td>
        <td>String&gt;</td>
        <td>
            The ports exposed by the container at runtime defined as: port_number [ "/" udp/tcp ] .
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar3">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
            <a href="#projectMainClass2">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.mainClass
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
            Resolves the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.resolveMainClass
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
            <a href="#skip3">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the build and installation of the container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.install.skip
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
            Strips debug information from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripDebug
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
            Strips native command (e.g. java...) from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripNativeCommands
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
            The user and group used to run the container defined as: user / uid [ ":" group / gid ].
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
            <a href="#vm2">vm</a>
        </td>
        <td>String</td>
        <td>
            Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#volumes1">volumes</a>
        </td>
        <td>String&gt;</td>
        <td>
            The container's mount points.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration1">windowsConfiguration</a>
        </td>
        <td>WindowsConfigurationParameters</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;aboutURL&gt;

The application's home page URL.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.aboutURL
- **Default**: ${project.url}


##### &lt;addModules&gt;

The modules to add to the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addOptions


##### &lt;addUnnamedModules&gt;

Adds unnamed modules when generating the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.addUnnamedModules
- **Default**: true


##### &lt;archiveFormats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: yes
- **User property**: inverno.runtime.archiveFormats


##### &lt;archivePrefix&gt;

The path to the runtime image within the archive.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.archivePrefix
- **Default**: ${project.build.finalName}


##### &lt;attach&gt;

Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Links in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;contentFiles&gt;

Files to add to the application payload.

- **Type**: java.util.List&lt;java.io.File&gt;
- **Required**: no
- **User property**: inverno.app.contentFiles


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;dockerEnvironment&gt;

The Docker environment variables used by the Docker CLI executable.

- **Type**: java.util.Map&lt;java.lang.String, java.lang.String&gt;
- **Required**: no


##### &lt;dockerExecutableFile&gt;

The path to the Docker CLI executable used to load the image in the Docker daemon.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.container.dockerExecutable


##### &lt;environment&gt;

The container's environment variables.

- **Type**: java.util.Map&lt;java.lang.String, java.lang.String&gt;
- **Required**: no


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeScope


##### &lt;executable&gt;

The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.executable
- **Default**: ${project.artifactId}


##### &lt;from&gt;

The base container image.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.from
- **Default**: debian:buster-slim


##### &lt;ignoreSigningInformation&gt;

Suppresses a fatal error when signed modular JARs are linked in the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.ignoreSigningInformation
- **Default**: false


##### &lt;imageFormat&gt;

The format of the container image.

- **Type**: io.inverno.tool.buildtools.ContainerizeTask$Format
- **Required**: no
- **User property**: inverno.container.imageFormat
- **Default**: Docker


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary: 
* runtime scope gives runtime and compile dependencies, 
* compile scope gives compile, provided, and system dependencies, 
* test (default) scope gives all dependencies, 
* provided scope just gives provided dependencies, 
* system scope just gives system dependencies. 

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;labels&gt;

The labels to apply to the container image.

- **Type**: java.util.Map&lt;java.lang.String, java.lang.String&gt;
- **Required**: no


##### &lt;launchers&gt;

The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ApplicationLauncherParameters&gt;
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.LinuxConfigurationParameters
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.MacOSConfigurationParameters
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;packageTypes&gt;

A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no
- **User property**: inverno.app.packageTypes


##### &lt;ports&gt;

The ports exposed by the container at runtime defined as: port_number [ "/" udp/tcp ] .

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.mainClass


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

Resolves the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.resolveMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the build and installation of the container image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.install.skip


##### &lt;stripDebug&gt;

Strips debug information from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strips native command (e.g. java...) from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripNativeCommands
- **Default**: true


##### &lt;user&gt;

The user and group used to run the container defined as: user / uid [ ":" group / gid ].

- **Type**: java.lang.String
- **Required**: no


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;vm&gt;

Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.vm


##### &lt;volumes&gt;

The container's mount points.

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.WindowsConfigurationParameters
- **Required**: no


### inverno:package-app

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:package-app

**Description:**


Builds and packages the project application image.

A project application package is a native self-contained Java application including all the necessary dependencies. It can be used to distribute a complete application.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.4.
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
            <a href="#archiveFormats3">archiveFormats</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archiveFormats
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#attach3">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.attach
                </li>
                <li>
                    <em>Default</em>
                    : true
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
            <a href="#aboutURL2">aboutURL</a>
        </td>
        <td>String</td>
        <td>
            The application's home page URL.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.aboutURL
                </li>
                <li>
                    <em>Default</em>
                    : ${project.url}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addModules3">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addModules
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
            The options to prepend before any other options when invoking the JVM in the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addUnnamedModules4">addUnnamedModules</a>
        </td>
        <td>boolean</td>
        <td>
            Adds unnamed modules when generating the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addUnnamedModules
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
            <a href="#archivePrefix3">archivePrefix</a>
        </td>
        <td>String</td>
        <td>
            The path to the runtime image within the archive.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archivePrefix
                </li>
                <li>
                    <em>Default</em>
                    : ${project.build.finalName}
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
            <a href="#bindServices3">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Links in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.bindServices
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
                    : inverno.runtime.compress
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
            A directory containing user-editable configuration files that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.configurationDirectory
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
            <a href="#contentFiles2">contentFiles</a>
        </td>
        <td>File&gt;</td>
        <td>
            Files to add to the application payload.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.contentFiles
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
            <a href="#excludeArtifactIds3">excludeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to exclude.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.excludeArtifactIds
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
            Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.excludeClassifiers
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
                    : inverno.runtime.excludeGroupIds
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
                    : inverno.runtime.excludeScope
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
            Suppresses a fatal error when signed modular JARs are linked in the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.ignoreSigningInformation
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
            <a href="#includeArtifactIds3">includeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.includeArtifactIds
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
            Comma separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.includeClassifiers
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
                    : inverno.runtime.includeGroupIds
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
                    : inverno.runtime.includeScope
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
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.
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
            <a href="#launchers3">launchers</a>
        </td>
        <td>ApplicationLauncherParameters&gt;</td>
        <td>
            The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory3">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.legalDirectory
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
        <td>LinuxConfigurationParameters</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration2">macOSConfiguration</a>
        </td>
        <td>MacOSConfigurationParameters</td>
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
            A directory containing man pages that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.manDirectory
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
            <a href="#moduleOverrides4">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory4">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#packageTypes2">packageTypes</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.packageTypes
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar4">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
            <a href="#projectMainClass3">projectMainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class in the project module to use when building the project JMOD package.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.mainClass
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
            Resolves the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.resolveMainClass
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
            <a href="#skip4">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the build and packaging of the application image.
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
            Strips debug information from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripDebug
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
            Strips native command (e.g. java...) from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripNativeCommands
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
            <a href="#vm3">vm</a>
        </td>
        <td>String</td>
        <td>
            Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration2">windowsConfiguration</a>
        </td>
        <td>WindowsConfigurationParameters</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;aboutURL&gt;

The application's home page URL.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.aboutURL
- **Default**: ${project.url}


##### &lt;addModules&gt;

The modules to add to the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addOptions


##### &lt;addUnnamedModules&gt;

Adds unnamed modules when generating the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.addUnnamedModules
- **Default**: true


##### &lt;archiveFormats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: yes
- **User property**: inverno.runtime.archiveFormats


##### &lt;archivePrefix&gt;

The path to the runtime image within the archive.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.archivePrefix
- **Default**: ${project.build.finalName}


##### &lt;attach&gt;

Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Links in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;contentFiles&gt;

Files to add to the application payload.

- **Type**: java.util.List&lt;java.io.File&gt;
- **Required**: no
- **User property**: inverno.app.contentFiles


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeScope


##### &lt;ignoreSigningInformation&gt;

Suppresses a fatal error when signed modular JARs are linked in the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.ignoreSigningInformation
- **Default**: false


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary: 
* runtime scope gives runtime and compile dependencies, 
* compile scope gives compile, provided, and system dependencies, 
* test (default) scope gives all dependencies, 
* provided scope just gives provided dependencies, 
* system scope just gives system dependencies. 

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;launchers&gt;

The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ApplicationLauncherParameters&gt;
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.LinuxConfigurationParameters
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.MacOSConfigurationParameters
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;packageTypes&gt;

A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no
- **User property**: inverno.app.packageTypes


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.mainClass


##### &lt;resolveProjectMainClass&gt;

Resolves the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.resolveMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the build and packaging of the application image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.skip


##### &lt;stripDebug&gt;

Strips debug information from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strips native command (e.g. java...) from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripNativeCommands
- **Default**: true


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;vm&gt;

Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.vm


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.WindowsConfigurationParameters
- **Required**: no


### inverno:package-image

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:package-image

**Description:**


Builds and packages the project application container image in a TAR archive.


**Attributes:**

- Requires a Maven project to be executed.
- Requires dependency resolution of artifacts in scope: compile+runtime.
- Requires dependency collection of artifacts in scope: compile+runtime.
- Since version: 1.4.
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
            <a href="#archiveFormats4">archiveFormats</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of archive formats to generate (eg. zip, tar.gz...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archiveFormats
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#attach4">attach</a>
        </td>
        <td>boolean</td>
        <td>
            Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.attach
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
                    : inverno.container.executable
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
            <a href="#aboutURL3">aboutURL</a>
        </td>
        <td>String</td>
        <td>
            The application's home page URL.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.aboutURL
                </li>
                <li>
                    <em>Default</em>
                    : ${project.url}
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addModules4">addModules</a>
        </td>
        <td>String</td>
        <td>
            The modules to add to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addModules
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
            The options to prepend before any other options when invoking the JVM in the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addOptions
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#addUnnamedModules5">addUnnamedModules</a>
        </td>
        <td>boolean</td>
        <td>
            Adds unnamed modules when generating the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.addUnnamedModules
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
            <a href="#archivePrefix4">archivePrefix</a>
        </td>
        <td>String</td>
        <td>
            The path to the runtime image within the archive.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.archivePrefix
                </li>
                <li>
                    <em>Default</em>
                    : ${project.build.finalName}
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
            <a href="#bindServices4">bindServices</a>
        </td>
        <td>boolean</td>
        <td>
            Links in service provider modules and their dependencies.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.bindServices
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
                    : inverno.runtime.compress
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
            A directory containing user-editable configuration files that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.configurationDirectory
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
            <a href="#contentFiles3">contentFiles</a>
        </td>
        <td>File&gt;</td>
        <td>
            Files to add to the application payload.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.contentFiles
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
            <a href="#environment2">environment</a>
        </td>
        <td>String&gt;</td>
        <td>
            The container's environment variables.
            <ul/>
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
                    : inverno.runtime.excludeArtifactIds
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
            Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.excludeClassifiers
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
                    : inverno.runtime.excludeGroupIds
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
                    : inverno.runtime.excludeScope
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
            Suppresses a fatal error when signed modular JARs are linked in the runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.ignoreSigningInformation
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
        <td>Format</td>
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
            <a href="#includeArtifactIds4">includeArtifactIds</a>
        </td>
        <td>String</td>
        <td>
            Comma separated list of Artifact names to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.includeArtifactIds
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
            Comma separated list of Classifiers to include. Empty String indicates include everything (default).
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.includeClassifiers
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
                    : inverno.runtime.includeGroupIds
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
                    : inverno.runtime.includeScope
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
            Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.
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
            <a href="#labels2">labels</a>
        </td>
        <td>String&gt;</td>
        <td>
            The labels to apply to the container image.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#launchers4">launchers</a>
        </td>
        <td>ApplicationLauncherParameters&gt;</td>
        <td>
            The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#legalDirectory4">legalDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing legal notices that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.legalDirectory
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
        <td>LinuxConfigurationParameters</td>
        <td>
            Linux specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#macOSConfiguration3">macOSConfiguration</a>
        </td>
        <td>MacOSConfigurationParameters</td>
        <td>
            MacOS specific configuration.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#manDirectory4">manDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing man pages that will be copied to the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.manDirectory
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
            <a href="#moduleOverrides5">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory5">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#packageTypes3">packageTypes</a>
        </td>
        <td>String&gt;</td>
        <td>
            A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.app.packageTypes
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#ports2">ports</a>
        </td>
        <td>String&gt;</td>
        <td>
            The ports exposed by the container at runtime defined as: port_number [ "/" udp/tcp ] .
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar5">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
                    : inverno.runtime.mainClass
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
            <a href="#resolveProjectMainClass4">resolveProjectMainClass</a>
        </td>
        <td>boolean</td>
        <td>
            Resolves the project main class when not specified explicitly.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.resolveMainClass
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
            <a href="#skip5">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the build and packaging of the container image.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.image.package.skip
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
            Strips debug information from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripDebug
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
            Strips native command (e.g. java...) from the resulting runtime.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.stripNativeCommands
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
            The user and group used to run the container defined as: user / uid [ ":" group / gid ].
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
            <a href="#vm4">vm</a>
        </td>
        <td>String</td>
        <td>
            Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.runtime.vm
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#volumes2">volumes</a>
        </td>
        <td>String&gt;</td>
        <td>
            The container's mount points.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#windowsConfiguration3">windowsConfiguration</a>
        </td>
        <td>WindowsConfigurationParameters</td>
        <td>
            Windows specific configuration.
            <ul/>
        </td>
    </tr>
</table>

#### Parameter details



##### &lt;aboutURL&gt;

The application's home page URL.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.aboutURL
- **Default**: ${project.url}


##### &lt;addModules&gt;

The modules to add to the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addModules


##### &lt;addOptions&gt;

The options to prepend before any other options when invoking the JVM in the resulting runtime.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.addOptions


##### &lt;addUnnamedModules&gt;

Adds unnamed modules when generating the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.addUnnamedModules
- **Default**: true


##### &lt;archiveFormats&gt;

A list of archive formats to generate (eg. zip, tar.gz...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: yes
- **User property**: inverno.runtime.archiveFormats


##### &lt;archivePrefix&gt;

The path to the runtime image within the archive.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.archivePrefix
- **Default**: ${project.build.finalName}


##### &lt;attach&gt;

Attaches the resulting image archives to the project to install them in the local Maven repository and deploy them to remote repositories.

- **Type**: boolean
- **Required**: yes
- **User property**: inverno.attach
- **Default**: true


##### &lt;automaticLaunchers&gt;

Enables the automatic generation of launchers based on the main classes extracted from the application module. If enabled, a launcher is generated for all main classes other than the main launcher.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.app.automaticLaunchers
- **Default**: false


##### &lt;bindServices&gt;

Links in service provider modules and their dependencies.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.bindServices
- **Default**: false


##### &lt;compress&gt;

The compress level of the resulting image: 0=No compression, 1=constant string sharing, 2=ZIP.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.compress


##### &lt;configurationDirectory&gt;

A directory containing user-editable configuration files that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.configurationDirectory
- **Default**: ${project.basedir}/src/main/conf/


##### &lt;contentFiles&gt;

Files to add to the application payload.

- **Type**: java.util.List&lt;java.io.File&gt;
- **Required**: no
- **User property**: inverno.app.contentFiles


##### &lt;copyright&gt;

The application copyright.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.copyright


##### &lt;environment&gt;

The container's environment variables.

- **Type**: java.util.Map&lt;java.lang.String, java.lang.String&gt;
- **Required**: no


##### &lt;excludeArtifactIds&gt;

Comma separated list of Artifact names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeArtifactIds


##### &lt;excludeClassifiers&gt;

Comma separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeClassifiers


##### &lt;excludeGroupIds&gt;

Comma separated list of GroupId Names to exclude.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeGroupIds


##### &lt;excludeScope&gt;

Scope to exclude. An Empty string indicates no scopes (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.excludeScope


##### &lt;executable&gt;

The executable in the application image to use as image entry point. The specified name should correspond to a declared application image launchers or the project artifact id if no launcher was specified.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.executable
- **Default**: ${project.artifactId}


##### &lt;from&gt;

The base container image.

- **Type**: java.lang.String
- **Required**: yes
- **User property**: inverno.container.from
- **Default**: debian:buster-slim


##### &lt;ignoreSigningInformation&gt;

Suppresses a fatal error when signed modular JARs are linked in the runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.ignoreSigningInformation
- **Default**: false


##### &lt;imageFormat&gt;

The format of the container image.

- **Type**: io.inverno.tool.buildtools.ContainerizeTask$Format
- **Required**: no
- **User property**: inverno.container.imageFormat
- **Default**: Docker


##### &lt;includeArtifactIds&gt;

Comma separated list of Artifact names to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeArtifactIds


##### &lt;includeClassifiers&gt;

Comma separated list of Classifiers to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeClassifiers


##### &lt;includeGroupIds&gt;

Comma separated list of GroupIds to include. Empty String indicates include everything (default).

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeGroupIds


##### &lt;includeScope&gt;

Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as Maven sees them, not as specified in the pom. In summary: 
* runtime scope gives runtime and compile dependencies, 
* compile scope gives compile, provided, and system dependencies, 
* test (default) scope gives all dependencies, 
* provided scope just gives provided dependencies, 
* system scope just gives system dependencies. 

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.includeScope


##### &lt;installDirectory&gt;

Absolute path of the installation directory of the application on OS X or Linux. Relative sub-path of the installation location of the application such as "Program Files" or "AppData" on Windows.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.installDirectory


##### &lt;labels&gt;

The labels to apply to the container image.

- **Type**: java.util.Map&lt;java.lang.String, java.lang.String&gt;
- **Required**: no


##### &lt;launchers&gt;

The specific list of launchers to include in the resulting application. The first launcher in the list will be considered as the main launcher.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ApplicationLauncherParameters&gt;
- **Required**: no


##### &lt;legalDirectory&gt;

A directory containing legal notices that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.legalDirectory
- **Default**: ${project.basedir}/src/main/legal/


##### &lt;licenseFile&gt;

The path to the application license file.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.licenseFile
- **Default**: ${project.basedir}/LICENSE


##### &lt;linuxConfiguration&gt;

Linux specific configuration.

- **Type**: io.inverno.tool.maven.LinuxConfigurationParameters
- **Required**: no


##### &lt;macOSConfiguration&gt;

MacOS specific configuration.

- **Type**: io.inverno.tool.maven.MacOSConfigurationParameters
- **Required**: no


##### &lt;manDirectory&gt;

A directory containing man pages that will be copied to the resulting runtime.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.runtime.manDirectory
- **Default**: ${project.basedir}/src/main/man/


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;packageTypes&gt;

A list of package types to generate (eg. rpm, deb, exe, msi, dmg pkg...)

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no
- **User property**: inverno.app.packageTypes


##### &lt;ports&gt;

The ports exposed by the container at runtime defined as: port_number [ "/" udp/tcp ] .

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;projectMainClass&gt;

The main class in the project module to use when building the project JMOD package.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.mainClass


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

Resolves the project main class when not specified explicitly.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.resolveMainClass
- **Default**: false


##### &lt;resourceDirectory&gt;

The path to resources that override resulting package resources.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.app.resourceDirectory


##### &lt;skip&gt;

Skips the build and packaging of the container image.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.image.package.skip


##### &lt;stripDebug&gt;

Strips debug information from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripDebug
- **Default**: true


##### &lt;stripNativeCommands&gt;

Strips native command (e.g. java...) from the resulting runtime.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.runtime.stripNativeCommands
- **Default**: true


##### &lt;user&gt;

The user and group used to run the container defined as: user / uid [ ":" group / gid ].

- **Type**: java.lang.String
- **Required**: no


##### &lt;vendor&gt;

The application vendor.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.app.vendor
- **Default**: ${project.organization.name}


##### &lt;vm&gt;

Selects the HotSpot VM in the output image defined as: "client" / "server" / "minimal" / "all".

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.runtime.vm


##### &lt;volumes&gt;

The container's mount points.

- **Type**: java.util.Set&lt;java.lang.String&gt;
- **Required**: no


##### &lt;windowsConfiguration&gt;

Windows specific configuration.

- **Type**: io.inverno.tool.maven.WindowsConfigurationParameters
- **Required**: no


### inverno:run

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:run

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
            <a href="#addUnnamedModules6">addUnnamedModules</a>
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
            <a href="#commandLineArguments1">commandLineArguments</a>
        </td>
        <td>String</td>
        <td>
            The command line arguments to pass to the application. This parameter overrides arguments when specified.
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
            <a href="#mainClass1">mainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class to use to run the application. If not specified, one of the main class in the project module is automatically selected.
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
            <a href="#moduleOverrides6">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory6">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar6">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
                    : inverno.run.skip
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
                    : -Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO
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
                    : inverno.exec.workingDirectory
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

The command line arguments to pass to the application. This parameter overrides arguments when specified.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.run.arguments


##### &lt;mainClass&gt;

The main class to use to run the application. If not specified, one of the main class in the project module is automatically selected.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.mainClass


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;skip&gt;

Skips the execution.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.run.skip


##### &lt;vmOptions&gt;

The VM options to use when executing the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.vmOptions
- **Default**: -Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO


##### &lt;workingDirectory&gt;

The working directory of the application.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.exec.workingDirectory
- **Default**: ${project.build.directory}/maven-inverno/working


### inverno:start

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:start

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
            <a href="#addUnnamedModules7">addUnnamedModules</a>
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
            <a href="#arguments2">arguments</a>
        </td>
        <td>String</td>
        <td>
            The arguments to pass to the application.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#mainClass2">mainClass</a>
        </td>
        <td>String</td>
        <td>
            The main class to use to run the application. If not specified, one of the main class in the project module is automatically selected.
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
            <a href="#moduleOverrides7">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory7">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar7">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
            <a href="#skip7">skip</a>
        </td>
        <td>boolean</td>
        <td>
            Skips the execution.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.start.skip
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
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#vmOptions2">vmOptions</a>
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
                    : -Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#workingDirectory2">workingDirectory</a>
        </td>
        <td>File</td>
        <td>
            The working directory of the application.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.exec.workingDirectory
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


##### &lt;mainClass&gt;

The main class to use to run the application. If not specified, one of the main class in the project module is automatically selected.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.mainClass


##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


##### &lt;skip&gt;

Skips the execution.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.start.skip


##### &lt;timeout&gt;

The amount of time in milliseconds to wait for the application to start.

- **Type**: long
- **Required**: no
- **User property**: inverno.start.timeout


##### &lt;vmOptions&gt;

The VM options to use when executing the application.

- **Type**: java.lang.String
- **Required**: no
- **User property**: inverno.exec.vmOptions
- **Default**: -Dlog4j2.simplelogLevel=INFO -Dlog4j2.level=INFO


##### &lt;workingDirectory&gt;

The working directory of the application.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.exec.workingDirectory
- **Default**: ${project.build.directory}/maven-inverno/working


### inverno:stop

**Full name:**

io.inverno.tool:inverno-maven-plugin:1.5.0-SNAPSHOT:stop

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
            <a href="#moduleOverrides8">moduleOverrides</a>
        </td>
        <td>ModuleInfoParameters&gt;</td>
        <td>
            A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.
            <ul/>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#moduleOverridesDirectory8">moduleOverridesDirectory</a>
        </td>
        <td>File</td>
        <td>
            A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.moduleOverridesDirectory
                </li>
                <li>
                    <em>Default</em>
                    : ${project.basedir}/src/modules/
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>
            <a href="#progressBar8">progressBar</a>
        </td>
        <td>boolean</td>
        <td>
            Displays a progress bar.
            <ul>
                <li>
                    <em>User property</em>
                    : inverno.progressBar
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
            <a href="#skip8">skip</a>
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
</table>

#### Parameter details



##### &lt;moduleOverrides&gt;

A list of module-info.java overrides that will be merged into the generated module descriptors for unnamed or automatic modules.

- **Type**: java.util.List&lt;io.inverno.tool.maven.ModuleInfoParameters&gt;
- **Required**: no


##### &lt;moduleOverridesDirectory&gt;

A directory containing module descriptors to use to modularize unnamed or automatic dependency modules and which replace the ones that are otherwise generated.

- **Type**: java.io.File
- **Required**: no
- **User property**: inverno.moduleOverridesDirectory
- **Default**: ${project.basedir}/src/modules/


##### &lt;progressBar&gt;

Displays a progress bar.

- **Type**: boolean
- **Required**: no
- **User property**: inverno.progressBar
- **Default**: true


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

