[inverno-tool-maven-plugin]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-maven-plugin

[jdk]: https://jdk.java.net/
[maven]: https://maven.apache.org/download.cgi
[open-container-image]: https://github.com/opencontainers/image-spec
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0

# Inverno Tools

[![CI/CD](https://github.com/inverno-io/inverno-tools/actions/workflows/maven.yml/badge.svg)](https://github.com/inverno-io/inverno-tools/actions/workflows/maven.yml)

The Inverno framework provides tools for running and building modular Java applications and Inverno applications in particular. It allows for instance to create native runtime and application images providing all the dependencies required to run a modular application. It is also possible to build Docker and [OCI][open-container-image] images.

## Inverno Maven Plugin

The [Inverno Maven Plugin][inverno-tool-maven-plugin] provides specific goals to:

- run a modular Java application.
- start/stop a modular Java application during the build process to execute integration tests.
- build native a runtime image containing a set of modules and their dependencies creating a light Java runtime.
- build native an application image containing an application and all its dependencies into an easy to install platform dependent package (eg. `.deb`, `.rpm`, `.dmg`, `.exe`, `.msi`...).
- build docker or OCI images of an application into a tarball, a Docker daemon or a container image registry.

The plugin requires [JDK][jdk] 15+ and [Apache Maven][maven] 3.6.0 or later.

## Building Inverno framework tools

The Inverno framework tools can be built using Maven and [JDK][jdk] 15+ with the following command:

```plaintext
$ mvn install
```

## License

The Inverno Framework is released under version 2.0 of the [Apache License][apache-license].

