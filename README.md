# Winter Tools

The Winter framework provides tools for running and building modular Java applications and Winter applications in particular. It allows for instance to create native runtime and application images providing all the dependencies required to run a modular application. It is also possible to build Docker and [OCI](https://github.com/opencontainers/image-spec) images.

## Winter Maven Plugin

The [Winter Maven Plugin](winter-maven-plugin/README.md) provides specific goals to:

- run a modular Java application.
- start/stop a modular Java application during the build process to execute integration tests.
- build native a runtime image containing a set of modules and their dependencies creating a light Java runtime.
- build native an application image containing an application and all its dependencies into an easy to install platform dependent package (eg. `.deb`, `.rpm`, `.dmg`, `.exe`, `.msi`...).
- build docker or OCI images of an application into a tarball, a Docker daemon or a container image registry.

The plugin requires [JDK](https://jdk.java.net/) 9 or later and [Apache Maven](https://maven.apache.org/download.cgi) 3.6.0 or later.