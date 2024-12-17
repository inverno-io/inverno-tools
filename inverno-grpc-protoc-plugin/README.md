[inverno-tools-grpc-protoc-plugin]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-grpc-protoc-plugin

[protocol-buffers]: https://protobuf.dev/
[protoc]: https://grpc.io/docs/protoc-installation/
[protoc-plugin]: https://protobuf.dev/reference/cpp/api-docs/google.protobuf.compiler.plugin/
[protobuf-maven-plugin]: https://github.com/xolstice/protobuf-maven-plugin

# Inverno gRPC Protoc plugin

The Inverno gRPC Protoc plugin is a [protoc][protoc] [plugin][protoc-plugin] for generating Inverno gRPC client and server stubs from service definitions in [Protocol buffers][protocol-buffers] files.

It can be used with the [Protocol Buffers Maven plugin][protobuf-maven-plugin] or directly with the protoc executable.

Let's consider following Protocol buffers file:

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "test.hello";
option java_outer_classname = "HelloWorldProto";

package test;

/* 
 * <p>
 * This is a hello service.
 * </p>
 * 
 * <p>
 * It is called Greeter and has 2 methods:
 * </p>
 * 
 * <ul>
 *   <li>SayHello</li>
 *   <li>SayHelloClientStreaming</li>
 *   <li>SayHelloServerStreaming</li>
 *   <li>SayHelloBidirectionalStreaming</li>
 * </ul>
 *
 */
service Greeter {
    /*
     * <p>
     * Says hello to someone
     * </p>
     */
    rpc SayHello (HelloRequest) returns (HelloResponse) {}
  
    /*
     * <p>
     * Says hello to eveyrbody
     * </p>
     */
    rpc SayHellos (stream HelloRequest) returns (stream HelloResponse) {}
}

/*
 * Hello request.
 */
message HelloRequest {
    string name = 1;
}

/*
 * Hello response.
 */
message HelloResponse {
    string message = 1;
}
```

## Generates gRPC stubs with Maven

In order to generate Inverno client or server stubs from `.proto` interface description files, the [Protocol Buffers Maven plugin][protobuf-maven-plugin] must be declared in the `pom.xml` of your project.

```xml
<project>
    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <extensions>true</extensions>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>test-compile</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:${version.protobuf}:exe:${os.detected.classifier}</protocArtifact>
                    <protocPlugins>
                        <protocPlugin>
                            <id>inverno-grpc-protoc-plugin</id>
                            <groupId>io.inverno.tool</groupId>
                            <artifactId>inverno-grpc-protoc-plugin</artifactId>
                            <version>${version.inverno.tools}</version>
                            <mainClass>io.inverno.tool.grpc.protocplugin.InvernoGrpcProtocPlugin</mainClass>
                            <args>--client --server</args>
                        </protocPlugin>
                    </protocPlugins>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

> Maven plugins and artifact versions in above example are all provided in the Inverno parent or dependencies poms.

The arguments passed to the plugin specify whether client stubs, server stubs or both must be generated from `.proto` files under `src/main/proto` (`compile` goal) or `src/test/proto` (`test-compile` goal). Java sources are generated to `${project.build.directory}/generated-sources/protobuf/java` or `${project.build.directory}/generated-test-sources/protobuf/java` by default. 

The plugin is executed when building the project (`generate-sources` and `generate-test-sources` phases)

```plaintext
$ ls src/main/proto
helloworld.proto
$ mvn compile
...
[INFO] --- protobuf:0.6.1:compile (default) @ inverno-example-grpc-client ---
[INFO] Building protoc plugin: inverno-grpc-protoc-plugin
[INFO] Compiling 1 proto file(s) to /home/jkuhn/Devel/git/winter/inverno-examples/inverno-example-grpc-client/target/generated-sources/protobuf/java
...
$ tree target/generated-sources/protobuf/java
target/generated-sources/protobuf/java
└── test
    └── hello
        ├── GreeterGrpcClient.java
        ├── GreeterGrpcRoutesConfigurer.java
        ├── HelloRequest.java
        ├── HelloRequestOrBuilder.java
        ├── HelloResponse.java
        ├── HelloResponseOrBuilder.java
        └── HelloWorldProto.java

3 directories, 7 files
```

Please refer to [protobuf-maven-plugin]: https://github.com/xolstice/protobuf-maven-plugin documentation to learn how to configure the Protocol buffer compiler.

## Generates gRPC stubs with protoc

The Inverno gRPC protoc plugin can also be directly passed to `protoc.exe`, we first need to package the plugin in a native Java application.

From the plugin [source folder][inverno-tools-grpc-protoc-plugin], we have to run:

```plaintext
$ mvn inverno:package-app
...
[═════════════════════════════════════════════ 100 % ════════════════════════════════════════════] Project application created
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  19.974 s
[INFO] Finished at: 2024-04-08T16:29:13+02:00
[INFO] ------------------------------------------------------------------------

```

This should generate a native plugin launcher under `target/inverno-grpc-protoc-plugin-${plugin.version}-application_${os.classifier}/bin/inverno-grpc-protoc-plugin` which has been configured to generate both client and server stubs.

It can be passed to `protoc.exe` command as follows:

```plaintext
$ protoc.exe --java_out=out foc/helloworld.proto --plugin=protoc-gen-grpc=./target/inverno-grpc-protoc-plugin-${plugin.version}-application_${os.classifier}/bin/inverno-grpc-protoc-plugin --grpc_out=out

$ tree out
out
└── test
    └── hello
        ├── GreeterGrpcClient.java
        ├── GreeterGrpcRoutesConfigurer.java
        ├── HelloRequest.java
        ├── HelloRequestOrBuilder.java
        ├── HelloResponse.java
        ├── HelloResponseOrBuilder.java
        └── HelloWorldProto.java

3 directories, 7 files
```

## Using gRPC client stub

The plugin generates one client stub per service, in our example we have defined one service: `Greeter`, the plugin should have generated the message types classes and `GreeterGrpcClient` class used to invoke service methods.

The client application module requires the boot module, the gRPC client module, the HTTP client module, the Web client module and one or more HTTP discovery modules (this is required by the Web client module):

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-boot</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-grpc-client</artifactId>
        </dependency>
		<dependency>
			<groupId>io.inverno.mod</groupId>
			<artifactId>inverno-http-client</artifactId>
		</dependency>
		<dependency>
			<groupId>io.inverno.mod</groupId>
			<artifactId>inverno-web-client</artifactId>
		</dependency>
		<dependency>
			<groupId>io.inverno.mod</groupId>
			<artifactId>inverno-discovery-http</artifactId>
		</dependency>
    </dependencies>
</project>
```

```
@io.inverno.core.annotation.Module
module io.inverno.example.app_grpc_client {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.grpc.client;
    requires io.inverno.mod.http.client;
    requires io.inverno.mod.discovery.http;
    requires io.inverno.mod.web.client;
}
```

The `GreeterGrpcClient` class provides two base implementations for creating client beans based on the `HttpClient` or the `WebClient`. The `GreeterGrpcClient.Web` class is based on the `WebClient`, it is recommended for most cases as it abstracts service discovery and connection management, and it also allows to specify the exchange context type eventually aggregated in the global context by the Inverno Web compiler plugin. As for the `GreeterGrpcClient.Http`, it is based on the `HttpClient` and directly creates or uses an externally provided `Endpoint` to connect to the server, it should be favoured when there is a need to handle connections explicitly.

 Depending on the needs of an application, one can create a bean implementing one, the other or both:

```java
package io.inverno.example.app_grpc_client;

import examples.GreeterGrpcClient;
import examples.HelloReply;
import examples.HelloRequest;
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.grpc.client.GrpcClient;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.web.client.WebClient;

public class Main {
    
    @Bean
    public static class HttpGreeterGrpcClient extends GreeterGrpcClient.Http {
    
        public HttpGreeterGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
            super(httpClient, grpcClient);
        }
    }
    
    @Bean
    public static class WebGreeterGrpcClient extends GreeterGrpcClient.Web<ExchangeContext> {
    
        public WebGreeterGrpcClient(WebClient<? extends ExchangeContext> webClient, GrpcClient grpcClient) {
            super(ServiceID.of("http://127.0.0.1:8080"), webClient, grpcClient);
        }
    }
    
    public static void main(String[] args) {
        App_grpc_client app_grpc_client = Application.run(new App_grpc_client.Builder());
        try {
            // Using the HttpClient based implementation, the stub must be closed explicitly to close connections
            try(GreeterGrpcClient.HttpClientStub<ExchangeContext> stub = app_grpc_client.httpGreeterGrpcClient().createStub("127.0.0.1", 8080)) {
                HelloReply response = stub
                    .sayHello(HelloRequest.newBuilder()
                        .setName("Bob")
                        .build()
                    )
                    .block();
            }
    
            // Using the WebClient based implementation, connections are closed by the WebClient when the application module is stopped
            HelloReply response = app_grpc_client.webGreeterGrpcClient()
                .sayHello(HelloRequest.newBuilder()
                    .setName("Bob")
                    .build()
                )
                .block();
        }
        finally {
            app_grpc_client.stop();
        }
    }
}
```

It is also possible to create derived instances with specific metadata.

Using the `HttpClient` based implementation:

```java
try(GreeterGrpcClient.HttpClientStub<ExchangeContext> stub = app_grpc_client.httpGreeterGrpcClient().createStub("127.0.0.1", 8080)) {
    HelloReply response = stub
		.withMetadata(metadata -> metadata.messageEncoding("gzip"))
        .sayHello(HelloRequest.newBuilder()
            .setName("Bob")
            .build()
        )
        .block();
}
```

Using the `WebClient` based implementation:

```java
HelloReply response = app_grpc_client.webGreeterGrpcClient()
    .withMetadata(metadata -> metadata.messageEncoding("gzip"))
    .sayHello(HelloRequest.newBuilder()
        .setName("Bob")
        .build()
    )
    .block();
```

> You should only specify `--client` argument to the plugin to only generate client stubs.

## Implementing gRPC services

The plugin generates one server stub per service, in our example we have defined one service: `Greeter`, the plugin should have generated the message types classes and `GreeterGrpcRoutesConfigurer` class used to implement service methods.

The generated stub is a Web routes configurer, The server application module then requires the boot module, the Web server module and the gRPC server module:


```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-boot</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-web-server</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-grpc-server</artifactId>
        </dependency>
    </dependencies>
</project>
```

```
@io.inverno.core.annotation.Module
module io.inverno.example.app_grpc_server {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.grpc.server;
    requires io.inverno.mod.web.server;
}
```

The `GreeterGrpcRoutesConfigurer` is an abstract Web routes configurer that must be implemented and exposed as a bean in the application module in order for the gRPC service method endpoints to be registered in the Web server. 

By default, service methods are implemented by throwing `UnsupportedOperationException`, they can be implemented as follows:

```java
package io.inverno.example.app_grpc_server;

import io.inverno.http.base.ExchangeContext;
import org.reactivestreams.Publisher;
import test.hello.GreeterGrpcRoutesConfigurer;
import test.hello.HelloRequest;
import test.hello.HelloResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Bean( visibility = Visibility.PRIVATE )
public class GreeterController extends GreeterGrpcRoutesConfigurer<ExchangeContext> {

    @Override
    public Mono<HelloResponse> sayHello(HelloRequest request) {
        return Mono.just(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
    }

    @Override
    public Publisher<HelloReply> sayHellos(Publisher<HelloRequest> request) {
        return Flux.from(request)
            .map(helloRequest -> HelloResponse.newBuilder().setMessage("Hello " + helloRequest.getName()).build());
    }
}
```

GRPC request and response metadata can also be accessed or set as follows:

```java
package io.inverno.example.app_grpc_server;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.grpc.server.GrpcExchange;
import org.reactivestreams.Publisher;
import test.hello.GreeterGrpcRoutesConfigurer;
import test.hello.HelloRequest;
import test.hello.HelloResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Bean( visibility = Visibility.PRIVATE )
public class GreeterController extends GreeterGrpcRoutesConfigurer<ExchangeContext> {

    @Override
    public void sayHello(GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply> grpcExchange) {
        String encoding = grpxExchange.request().metadata().encoding().orElse("identity");

        grpxExchange.response()
            .metadata(metadata -> metadata.encoding(encoding))
            .value(grpcExchange.request().value().map(request -> HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build()));
    }

    @Override
    public void sayHello(GrpcExchange.BidirectionalStreaming<ExchangeContext, HelloRequest, HelloReply> grpcExchange) {
        String encoding = grpxExchange.request().metadata().encoding().orElse("identity");

        grpxExchange.response()
            .metadata(metadata -> metadata.encoding(encoding))
            .stream(Flux.from(grpcExchange.request().stream()).map(request -> HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build()));
    }
}
```

> You should only specify `--server` argument to the plugin to only generate server stubs.