[inverno-tool-build-tools]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-build-tools
[inverno-tool-maven-plugin]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-maven-plugin
[inverno-tool-grpc-protoc-plugin]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-grpc-protoc-plugin

[jdk]: https://jdk.java.net/
[maven]: https://maven.apache.org/download.cgi
[open-container-image]: https://github.com/opencontainers/image-spec
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0
[grpc]: https://grpc.io/
[protobuf]: https://protobuf.dev/

# Inverno Tools

[![CI/CD](https://github.com/inverno-io/inverno-tools/actions/workflows/maven.yml/badge.svg)](https://github.com/inverno-io/inverno-tools/actions/workflows/maven.yml)

The Inverno framework provides tools for running and building modular Java applications and Inverno applications in particular. It allows for instance to create native runtime and application images providing all the dependencies required to run a modular application. It is also possible to build Docker and [OCI][open-container-image] images, install them on a local Docker daemon or deploy them on remote registry.

## Inverno Build Tools

The [Inverno Build Tools][inverno-tool-build-tools] is a Java module exposing an API for running, packaging and distributing fully modular applications.

## Inverno Maven Plugin

The [Inverno Maven Plugin][inverno-tool-maven-plugin] is a Maven plugin based on the Inverno Build tools module which provides multiple goals to:

- run or debug a modular Java application project.
- start/stop a modular Java application during the build process to execute integration tests.
- build native runtime image containing a set of modules and their dependencies creating a light Java runtime.
- build native application image containing an application and all its dependencies into an easy to install platform dependent package (eg. `.deb`, `.rpm`, `.dmg`, `.exe`, `.msi`...).
- build docker or OCI images of an application into a tarball, a Docker daemon or a remote container image registry.

The plugin requires [JDK][jdk] 15+ and [Apache Maven][maven] 3.6.0 or later.

## Inverno gRPC Protocol Buffer compiler plugin

The [Inverno gRPC Protoc plugin][inverno-tool-grpc-protoc-plugin] is a [Protocol Buffer][protobuf] plugin for generating Inverno [gRPC][grpc] client and server stubs from Protocol Buffer service definitions.

## Building Inverno framework tools

The Inverno framework tools can be built using Maven and [JDK][jdk] 15+ with the following command:

```plaintext
$ mvn install
```

## License

The Inverno Framework is released under version 2.0 of the [Apache License][apache-license].

