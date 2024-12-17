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
package io.inverno.tool.grpc.protocplugin;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.compiler.PluginProtos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>
 * A generator for generating Inverno specific classes to create gRPC client and/or server.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface InvernoGrpcGenerator {

	/**
	 * <p>
	 * Parses the specified descriptor dump file into a code generator request and generates Inverno specific gRPC classes.
	 * </p>
	 *
	 * @param protocRequestDumpPath the path to the descriptor dump file
	 *
	 * @return a code generator response containing the generated files
	 *
	 * @throws IOException                    if there was an error accessing the dump file
	 * @throws InvalidProtocolBufferException if the dump descriptor does not contain a valid code generator data
	 */
	default PluginProtos.CodeGeneratorResponse generate(Path protocRequestDumpPath) throws IOException, InvalidProtocolBufferException {
		return this.generate(protocRequestDumpPath, ExtensionRegistry.getEmptyRegistry());
	}
	
	/**
	 * <p>
	 * Parses the specified descriptor dump file into a code generator request and generates Inverno specific gRPC classes.
	 * </p>
	 *
	 * @param protocRequestDumpPath the path to the descriptor dump file
	 * @param extensionRegistry     an extension registry
	 *
	 * @return a code generator response containing the generated files
	 *
	 * @throws IOException                    if there was an error accessing the dump file
	 * @throws InvalidProtocolBufferException if the dump descriptor does not contain a valid code generator data
	 */
	default PluginProtos.CodeGeneratorResponse generate(Path protocRequestDumpPath, ExtensionRegistry extensionRegistry) throws IOException, InvalidProtocolBufferException {
		return this.generate(Files.readAllBytes(protocRequestDumpPath), extensionRegistry);
	}
	
	/**
	 * <p>
	 * Parses the specified descriptor data into a code generator request and generates Inverno specific gRPC classes.
	 * </p>
	 *
	 * @param protocRequestBytes descriptor data
	 *
	 * @return a code generator response containing the generated files
	 *
	 * @throws InvalidProtocolBufferException if data are not a valid code generator request
	 */
	default PluginProtos.CodeGeneratorResponse generate(byte[] protocRequestBytes) throws InvalidProtocolBufferException {
		return this.generate(protocRequestBytes, ExtensionRegistry.getEmptyRegistry());
	}
	
	/**
	 * <p>
	 * Parses the specified descriptor data into a code generator request and generates Inverno specific gRPC classes.
	 * </p>
	 *
	 * @param protocRequestBytes descriptor data
	 * @param extensionRegistry  an extension registry
	 *
	 * @return a code generator response containing the generated files
	 *
	 * @throws InvalidProtocolBufferException if data are not a valid code generator request
	 */
	default PluginProtos.CodeGeneratorResponse generate(byte[] protocRequestBytes, ExtensionRegistry extensionRegistry) throws InvalidProtocolBufferException {
		return this.generate(PluginProtos.CodeGeneratorRequest.parseFrom(protocRequestBytes, extensionRegistry));
	}
	
	/**
	 * <p>
	 * Generates Inverno specific gRPC classes.
	 * </p>
	 * 
	 * @param request the code generator request
	 *
	 * @return the code generator response
	 */
	PluginProtos.CodeGeneratorResponse generate(PluginProtos.CodeGeneratorRequest request);
}
