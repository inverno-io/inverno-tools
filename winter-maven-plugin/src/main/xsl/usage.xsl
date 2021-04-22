<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.w3.org/1999/XSL/Transform https://www.w3.org/2007/schema-for-xslt20.xsd">
	
<xsl:template name="getUsage">
The Winter Maven plugin can be used to run a modular application project or build an image for a modular project. There are three types of images that can be build using the plugin:

- **runtime image** is a custom Java runtime containing a set of modules and their dependencies.
- **application image** is a native self-contained Java application including all the necessary dependencies to run the application without the need of a Java runtime. 
- **container image** is a Docker or CLI container image that can be packaged as a TAR archive or directly deployed on a Docker daemon or container registry.

### Run a module application project

The `winter:run` goal is used to execute the modular application defined in the project from the command line.

```
$ mvn winter:run
```

The application is first *modularized* which means that any non-modular dependency is modularized by generating an appropriate module descriptor using the `jdeps` tool in order for the application to be run with a module path and not a class path (and certainly not both).

The application is executed in a forked process, application arguments can be passed on the command line:

```
$ mvn winter:run -Dwinter.run.arguments='--some.configuration=\"hello\"'
```

<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> Actual arguments are determined by splitting the parameter value around spaces. There are several options to declare an argument which contains spaces:
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> - it can be escaped: `Hello\ World` 
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> - it can be quoted: `"Hello World"` or `'Hello World'`
<xsl:text disable-output-escaping="yes"><![CDATA[>
]]></xsl:text> 
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> Since quotes or double quotes are used as delimiters, they might need to be escaped as well to declare an argument that contains some: `I\'m\ happy`, `"I'm happy"`, `'I\'m happy'`.

In order to debug the application, we need to specify the appropriate options to the JVM:

```
$ mvn winter:run -Dwinter.exec.vmOptions="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
```

By default the plugin will detect the main class of the application, but it is also possible to specify it explicitly in case multiple main classes exist in the project module.

```
$ mvn winter:run -Dwinter.exec.mainClass=io.winterframework.example.Main
```

A pidfile is created when the application is started under `${project.build.directory}/maven-winter` directory, it indicates the pid of the process running the application. If the build exits while the application is still running or if the pidfile was not properly removed after the application has exited, it might be necessary to manually kill the process and/or remove the pidfile. 

### Start and stop the application for integration testing

The `winter:start` and `winter:stop` goals are used together to start and stop the application while not blocking the Maven build process which can then execute other goals targeting the running application such as integration tests.

They are bound to the `pre-integration-test` and `pre-integration-test` phases respectively:

```xml
<xsl:text disable-output-escaping="yes"><![CDATA[<project>
	<build>
		<plugins>
			<plugin>
				<groupId>io.winterframework.tool</groupId>
				<artifactId>winter-maven-plugin</artifactId>
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
</project>]]></xsl:text>
```

### Build a runtime image

A runtime image is a custom Java runtime distribution containing specific modules and their dependencies. Such image is used as a base for generating application image but it can also be distributed as a lightweight Java runtime.

The `winter:build-runtime` goal uses `jlink` tool to assemble the project module and its dependencies.

```xml
<xsl:text disable-output-escaping="yes"><![CDATA[<project>
	<build>
		<plugins>
			<plugin>
				<groupId>io.winterframework.tool</groupId>
				<artifactId>winter-maven-plugin</artifactId>
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
</project>]]></xsl:text>
```

By default, the project module and its dependencies are included in the resulting image, this include JDK's modules such as `java.base`, in the previous example we've also explicitly added the `jdk.jdwp.agent` to support remote debugging and `jdk.crypto.ec` to support TLS communications.

The resulting image is packaged to the formats defined in the configuration and attached, by default, to the Maven project. 

### Build an application image

An application image is built using the `winter:build-app` goal which basically generates a runtime image and uses `jpackage` tool to generate a native platform-specific application package.

```xml
<xsl:text disable-output-escaping="yes"><![CDATA[<project>
	<build>
		<plugins>
			<plugin>
				<groupId>io.winterframework.tool</groupId>
				<artifactId>winter-maven-plugin</artifactId>
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
							<vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
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
</project>]]></xsl:text>
```

The `winter:build-app` goal is very similar to the `winter:build-runtime` goal except that the resulting image provides an application launcher and it is packaged in a platform-specific format. For instance, we can generate a `.deb` on a Linux platform or a `.exe` or `.msi` on a Windows platform or a `.dmg` on a MacOS platform. The resulting package can be installed on these platform in a standard way.

<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> This goal uses `jpackage` tool which is an incubating feature in JDK&lt;16, if you intend to build an application image with an old JDK, you'll need to explicitly add the `jdk.incubator.jpackage` in `MAVEN_OPTS`:
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```

### Build a container image tarball

A container image can be built in a TAR archive using the `winter:build-image-tar` goal which basically build an application package and package it in a container image.

```xml
<xsl:text disable-output-escaping="yes"><![CDATA[<project>
	<build>
		<plugins>
			<plugin>
				<groupId>io.winterframework.tool</groupId>
				<artifactId>winter-maven-plugin</artifactId>
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
							<vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
							<repository>example</repository>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>]]></xsl:text>
```

The resulting image reference is defined by `${registry}/${repository}/${name}:${project.version}`, the registry and the repository are optional and the name default to the project artifact id. 

The resulting image can then be loaded in a docker daemon:

```
$ docker load --input target/example-1.0.0-SNAPSHOT-container_linux_amd64.tar
```

<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> As for `build-app` goal, this goal uses `jpackage` tool so if you intend to use a JDK&lt;16 you'll need to explicitly add the `jdk.incubator.jpackage` in `MAVEN_OPTS`:
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```

### Build and deploy a container image to a Docker daemon

The `winter:build-image-docker` goal is used to build a container image and deploy it to a Docker daemon using the Docker CLI.

```xml
<xsl:text disable-output-escaping="yes"><![CDATA[<project>
	<build>
		<plugins>
			<plugin>
				<groupId>io.winterframework.tool</groupId>
				<artifactId>winter-maven-plugin</artifactId>
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
							<vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
							<repository>example</repository>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>]]></xsl:text>
```

By default the `docker` command is used but it is possible to specify the path to the Docker CLI in the `winter.container.docker.executable` parameter. 

<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> As for `build-app` goal, this goal uses `jpackage` tool so if you intend to use a JDK&lt;16 you'll need to explicitly add the `jdk.incubator.jpackage` in `MAVEN_OPTS`:
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```

### Build and deploy a container image to a remote repository

The `winter:build-image` goal builds a container image and deploy it to a remote repository.

```xml
<xsl:text disable-output-escaping="yes"><![CDATA[<project>
	<build>
		<plugins>
			<plugin>
				<groupId>io.winterframework.tool</groupId>
				<artifactId>winter-maven-plugin</artifactId>
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
							<vmOptions>-Xms2G -Xmx2G -XX:+UseNUMA -XX:+UseParallelGC</vmOptions>
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
</project>]]></xsl:text>
```

By default the registry points to the Docker hub `registry-1.docker.io` but another registry can be specified, `gcr.io` in our example.

<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> As for `build-app` goal, this goal uses `jpackage` tool so if you intend to use a JDK&lt;16 you'll need to explicitly add the `jdk.incubator.jpackage` in `MAVEN_OPTS`:
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> $ export MAVEN_OPTS="--add-modules jdk.incubator.jpackage"
<xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text> ```

</xsl:template>

</xsl:stylesheet>