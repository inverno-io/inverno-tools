/*
 * Copyright 2024 Jeremy KUHN
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

import io.inverno.tool.grpc.protocplugin.InvernoGrpcProtocPlugin;

/**
 * <p>
 * The Inverno gRPC protoc plugin module provides a <a href="https://protobuf.dev/reference/cpp/api-docs/google.protobuf.compiler.plugin/">protoc plugin</a> for generating Inverno gRPC client and 
 * server classes.
 * </p>
 * 
 * <p>
 * The {@link InvernoGrpcProtocPlugin} class is the plugin's entry point, it is used to generate client and/or server Inverno classes corresponding to protobuf service definitions.
 * </p>
 * 
 * <p>
 * The plugin is invoked by specifying the executable to the {@code protoc.exe} command as follows:
 * </p>
 * 
 * <pre>{@code
 * $ protoc.exe --plugin=protoc-gen-grpc=invernoGrpcProtocPlugin --grpc_out=target/generated-sources/protobuf/java file.proto
 * }</pre>
 * 
 * <p>
 * The gRPC generator can also be invoked programatically to process a descriptor dump for instance:
 * </p>
 * 
 * <pre>{@code
 * PluginProtos.CodeGeneratorResponse response = InvernoGrpcProtocPlugin.serverGenerator().generate(Path.of("path/to/dump");
 * response.write(System.out);
 * }</pre>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@io.inverno.core.annotation.Module
module io.inverno.tool.grpc.protocplugin {
	requires io.inverno.core;
	requires io.inverno.mod.irt;
	
	requires com.google.common;
	requires com.google.protobuf;
	requires com.google.protobuf.util;
	
	exports io.inverno.tool.grpc.protocplugin;
}

