/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.tool.protoc.plugin.internal;

import io.inverno.tool.protoc.plugin.ProtocGrpcRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class InvernoGrpcGeneratorTest {
	
	private static final Path SOURCE_PROTO_PATH = Path.of("src/test/proto");
	
	private static final Path EXPECTED_GRPC_PATH = Path.of("src/test/resources/expected-grpc-out");
	
	private static final Path TARGET_GRPC_PATH = Path.of("target/test-grpc-out");

	@Test
	public void test_server_grpc_testing_test() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("grpc/testing/test.proto"));
		
		String[] generatedFileNames = new String[] {
			"LoadBalancerStatsServiceGrpcRoutesConfigurer.java", 
			"ReconnectServiceGrpcRoutesConfigurer.java", 
			"TestServiceGrpcRoutesConfigurer.java", 
			"UnimplementedServiceGrpcRoutesConfigurer.java", 
			"XdsUpdateClientConfigureServiceGrpcRoutesConfigurer.java", 
			"XdsUpdateHealthServiceGrpcRoutesConfigurer.java"
		};
		
		for(String fileName : generatedFileNames) {
			Path generatedGrpcRouteConfigurer = Path.of("io/grpc/testing/integration", fileName);
			Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
			Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		}
	}
	
	@Test
	public void test_server_unary() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/unary.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/unary/UnaryGrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_server_client_streaming() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/client_streaming.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/clientstreaming/ClientStreamingGrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_server_server_streaming() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/server_streaming.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/serverstreaming/ServerStreamingGrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_server_bidirectional_streaming() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/bidirectional_streaming.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/bidirectionalstreaming/BidirectionalStreamingGrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_server_hello() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/helloworld.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/hello/GreeterGrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_server_multi_services() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/multi_service.proto"));
		
		Path generatedGrpcRouteConfigurer1 = Path.of("test/multi/Service1GrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer1)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer1)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer1)));
		
		Path generatedGrpcRouteConfigurer2 = Path.of("test/multi/Service2GrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer2)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer2)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer2)));
	}
	
	@Test
	public void test_server_outer_class() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/outer_class.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/outer/OuterServiceGrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_server_reserved_keywords() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/reserved_keywords.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/reserved/ReservedKeywordsServiceGrpcRoutesConfigurer.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_server_no_methods() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--server", SOURCE_PROTO_PATH.resolve("test/no_methods.proto"));
		
		Path generatedGrpcRouteConfigurer = Path.of("test/no_methods/NoMethodsGrpcRoutesConfigurer.java"); 
		
		Assertions.assertFalse(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcRouteConfigurer)));
	}
	
	@Test
	public void test_client_grpc_testing_test() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("grpc/testing/test.proto"));
		
		String[] generatedFileNames = new String[] {
			"LoadBalancerStatsServiceGrpcClient.java", 
			"ReconnectServiceGrpcClient.java", 
			"TestServiceGrpcClient.java", 
			"UnimplementedServiceGrpcClient.java", 
			"XdsUpdateClientConfigureServiceGrpcClient.java", 
			"XdsUpdateHealthServiceGrpcClient.java"
		};
		
		for(String fileName : generatedFileNames) {
			Path generatedGrpcClient = Path.of("io/grpc/testing/integration", fileName);
			Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
			Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		}
	}
	
	@Test
	public void test_client_unary() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/unary.proto"));
		
		Path generatedGrpcClient = Path.of("test/unary/UnaryGrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	@Test
	public void test_client_client_streaming() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/client_streaming.proto"));
		
		Path generatedGrpcClient = Path.of("test/clientstreaming/ClientStreamingGrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	@Test
	public void test_client_server_streaming() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/server_streaming.proto"));
		
		Path generatedGrpcClient = Path.of("test/serverstreaming/ServerStreamingGrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	@Test
	public void test_client_bidirectional_streaming() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/bidirectional_streaming.proto"));
		
		Path generatedGrpcClient = Path.of("test/bidirectionalstreaming/BidirectionalStreamingGrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	@Test
	public void test_client_hello() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/helloworld.proto"));
		
		Path generatedGrpcClient = Path.of("test/hello/GreeterGrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	@Test
	public void test_client_multi_services() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/multi_service.proto"));
		
		Path generatedGrpcClient1 = Path.of("test/multi/Service1GrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient1)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient1)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient1)));
		
		Path generatedGrpcClient2 = Path.of("test/multi/Service2GrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient2)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient2)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient2)));
	}
	
	@Test
	public void test_client_outer_class() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/outer_class.proto"));
		
		Path generatedGrpcClient = Path.of("test/outer/OuterServiceGrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	@Test
	public void test_client_reserved_keywords() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/reserved_keywords.proto"));
		
		Path generatedGrpcClient = Path.of("test/reserved/ReservedKeywordsServiceGrpcClient.java");
		Assertions.assertTrue(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
		Assertions.assertArrayEquals(Files.readAllBytes(EXPECTED_GRPC_PATH.resolve(generatedGrpcClient)), Files.readAllBytes(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	@Test
	public void test_client_no_methods() throws Exception {
		ProtocGrpcRunner.runProtocGrpc(SOURCE_PROTO_PATH, "--client", SOURCE_PROTO_PATH.resolve("test/no_methods.proto"));
		
		Path generatedGrpcClient = Path.of("test/no_methods/NoMethodsGrpcClient.java"); 
		Assertions.assertFalse(Files.exists(TARGET_GRPC_PATH.resolve(generatedGrpcClient)));
	}
	
	/*@Test
	public void test_dump() throws Exception {
		PluginProtos.CodeGeneratorResponse response = InvernoGrpcProtocPlugin.serverGenerator().generate(Path.of("src/test/resources/protoc.dump"));
	}*/
}
