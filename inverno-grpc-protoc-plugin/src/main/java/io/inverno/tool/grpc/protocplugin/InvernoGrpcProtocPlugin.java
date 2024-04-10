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

import com.google.common.io.ByteStreams;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos;
import io.inverno.tool.grpc.protocplugin.internal.GenericInvernoGrpcGenerator;
import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>
 * A {@code protoc} plugin used to generate Inverno specific gRPC client and/or server classes.
 * </p>
 * 
 * <p>
 * The command accepts two options:
 * </p>
 * 
 * <ul>
 * <li>{@code --client}: tells the plugin to generate client classes</li>
 * <li>{@code --server}: tells the plugin to generate server classes</li>
 * </ul>
 * 
 * <p>
 * If none of the above is specified, the plugin generates no classes.
 * </p>
 * 
 * <p>
 * The command parses the code generator request from the standard input by default (i.e. {@code System.in}) as expected when invoked inside {@code protoc.exe} as follows:
 * </p>
 * 
 * <pre>{@code
 * $ protoc.exe --plugin=protoc-gen-grpc=invernoGrpcProtocPlugin.sh --grpc_out=target/generated-sources/protobuf/java file.proto
 * }</pre>
 * 
 * <p>
 * In above command line {@code invernoGrpcProtocPlugin.sh} is an executable script invoking the plugin with the right arguments. For instance:
 * </p>
 * 
 * <pre>{@code
 * #!/bin/sh
 * 
 * java -jar inverno-grpc-protoc-plugin.jar --server --client "@$"
 * }</pre>
 * 
 * <p>
 * It also possible to specify the path to a dump on the command line and execute the command in standalone:
 * </p>
 * 
 * <pre>{@code
 * $ java -jar inverno-grpc-protoc-plugin.jar --server --client path/to/dump
 * }</pre>
 * 
 * <p>
 * The class also exposes several methods to create {@link InvernoGrpcGenerator} instances for generating client and/or server from raw descriptors.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class InvernoGrpcProtocPlugin {
	
	private static final String OPTION_CLIENT = "--client";
	
	private static final String OPTION_SERVER = "--server";
	
	/**
	 * <p>
	 * Executes the protoc plugin.
	 * </p>
	 * 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		try {
			GenericInvernoGrpcGenerator.Builder builder = GenericInvernoGrpcGenerator.builder();

			String dumpPath = null;
			for(int i=0;i<args.length;i++) {
				if(args[i].startsWith("--")) {
					switch(args[i]) {
						case OPTION_CLIENT:
							builder.generateClient(true);
							break;
						case OPTION_SERVER:
							builder.generateServer(true);
							break;
						default: printUsageAndExit();
					}
				}
				else if(i < args.length - 1) {
					printUsageAndExit();
				}
				else {
					dumpPath = args[i];
				}
			}
			
			GenericInvernoGrpcGenerator generator = builder.build();
			
			// Do we need extensions?
			ExtensionRegistry extensionRegistry = ExtensionRegistry.getEmptyRegistry();
			
			PluginProtos.CodeGeneratorResponse response;
			if(dumpPath != null) {
				response = generator.generate(Path.of(dumpPath), extensionRegistry);
			}
			else {
				response = generator.generate(ByteStreams.toByteArray(System.in), extensionRegistry);
			}
			response.writeTo(System.out);
		}
		catch(Throwable t) {
			try {
				PluginProtos.CodeGeneratorResponse
					.newBuilder()
					.setError(t.toString())
					.build()
					.writeTo(System.out);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			System.exit(1);
		}
	}
	
	/**
	 * <p>
	 * Prints command usage and exit with code 1.
	 * </p>
	 */
	private static void printUsageAndExit() {
		System.err.println("Usage: InvernoProtocPlugin [--client] [--server] [DUMP_PATH]");
		System.exit(1);
	}
	
	/**
	 * <p>
	 * Creates generator that generates both client and server classes.
	 * </p>
	 * 
	 * @return a generator that generates both client and server
	 */
	public static InvernoGrpcGenerator generator() {
		return GenericInvernoGrpcGenerator.builder().generateClient(true).generateServer(true).build();
	}
	
	/**
	 * <p>
	 * Creates generator that generates client classes only.
	 * </p>
	 * 
	 * @return a generator that generates client
	 */
	public static InvernoGrpcGenerator clientGenerator() {
		return GenericInvernoGrpcGenerator.builder().generateClient(true).build();
	}
	
	/**
	 * <p>
	 * Creates generator that generates server classes only.
	 * </p>
	 * 
	 * @return a generator that generates server
	 */
	public static InvernoGrpcGenerator serverGenerator() {
		return GenericInvernoGrpcGenerator.builder().generateServer(true).build();
	}
}
