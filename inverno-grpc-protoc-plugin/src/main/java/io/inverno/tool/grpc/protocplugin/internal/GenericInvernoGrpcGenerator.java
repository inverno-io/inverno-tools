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
package io.inverno.tool.grpc.protocplugin.internal;

import com.google.protobuf.compiler.PluginProtos;
import io.inverno.mod.irt.Pipe;
import io.inverno.tool.grpc.protocplugin.InvernoGrpcGenerator;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link InvernoGrpcGenerator} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericInvernoGrpcGenerator implements InvernoGrpcGenerator {
	
	/**
	 * Indicates whether the generator must generate client classes.
	 */
	private final boolean generateClient;
	
	/**
	 * Indicates whether the generator must generate server classes.
	 */
	private final boolean generateServer;
	
	/**
	 * <p>
	 * Creates a generic Inverno gRPC generator.
	 * </p>
	 * 
	 * @param generateClient true to generate client classes, false otherwise
	 * @param generateServer true to generate server classes, false otherwise
	 */
	private GenericInvernoGrpcGenerator(boolean generateClient, boolean generateServer) {
		this.generateClient = generateClient;
		this.generateServer = generateServer;
	}

	/**
	 * <p>
	 * Returns a generic gRPC generator builder.
	 * </p>
	 * 
	 * @return a generator builder
	 */
	public static GenericInvernoGrpcGenerator.Builder builder() {
		return new GenericInvernoGrpcGenerator.Builder();
	}
	
	@Override
	public PluginProtos.CodeGeneratorResponse generate(PluginProtos.CodeGeneratorRequest request) {
		return PluginProtos.CodeGeneratorResponse
			.newBuilder()
			.addAllFile(this.generate(new InvernoGrpcMetadata(request.getProtoFileList())))
			.setSupportedFeatures(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL.getNumber())
			.build();
	}
	
	/**
	 * <p>
	 * Generates classes from Inverno gRPC metadata extracted from a code generator request.
	 * </p>
	 * 
	 * @param metadata Inverno gRPC metadata
	 * 
	 * @return the list of generated files
	 */
	private List<PluginProtos.CodeGeneratorResponse.File> generate(InvernoGrpcMetadata metadata) {
		return metadata.getServices().stream()
			.filter(serviceMetadata -> !serviceMetadata.getMethods().isEmpty())
			.flatMap(serviceMetadata -> this.generate(serviceMetadata).stream())
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Generates client and/or server classes for the specified service metadata.
	 * </p>
	 * 
	 * @param serviceMetadata an Inverno gRPC service metadata
	 * 
	 * @return the list of generated files
	 */
	private List<PluginProtos.CodeGeneratorResponse.File> generate(InvernoGrpcMetadata.ServiceMetadata serviceMetadata) {
		List<PluginProtos.CodeGeneratorResponse.File> result = new ArrayList<>();
		if(this.generateClient) {
			result.add(this.generateClient(serviceMetadata));
		}
		if(this.generateServer) {
			result.add(this.generateRouteConfigurer(serviceMetadata));
		}
		return result;
	}
	
	/**
	 * <p>
	 * Generates the client class for the specified service metadata.
	 * </p>
	 * 
	 * @param serviceMetadata an Inverno gRPC service metadata
	 * 
	 * @return a generated file
	 */
	private PluginProtos.CodeGeneratorResponse.File generateClient(InvernoGrpcMetadata.ServiceMetadata serviceMetadata) {
		try {
			return PluginProtos.CodeGeneratorResponse.File.newBuilder()
				.setName(fileNameBuilder(serviceMetadata).append("GrpcClient.java").toString())
				.setContent(GrpcClientTemplate.string().render(serviceMetadata).get())
				.build();
		} 
		catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Error generating service file", e);
		}
	}
	
	/**
	 * <p>
	 * Generates the Web Route configurer class for the specified service metadata.
	 * </p>
	 * 
	 * @param serviceMetadata an Inverno gRPC service metadata
	 * 
	 * @return a generated file
	 */
	private PluginProtos.CodeGeneratorResponse.File generateRouteConfigurer(InvernoGrpcMetadata.ServiceMetadata serviceMetadata) {
		try {
			return PluginProtos.CodeGeneratorResponse.File.newBuilder()
				.setName(fileNameBuilder(serviceMetadata).append("GrpcRoutesConfigurer.java").toString())
				.setContent(GrpcRouteConfigurerTemplate.string().render(serviceMetadata).get())
				.build();
		} 
		catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Error generating service file", e);
		}
	}
	
	/**
	 * <p>
	 * Creates a builder for building a the name of a class file for the specified service metadata.
	 * </p>
	 * 
	 * <p>
	 * This typically creates a {@code StringBuilder} initialized with the java package path and the java service name.
	 * </p>
	 * 
	 * @param serviceMetadata an Inverno gRPC service metadata
	 * 
	 * @return a new string builder
	 */
	private static StringBuilder fileNameBuilder(InvernoGrpcMetadata.ServiceMetadata serviceMetadata) {
		StringBuilder fileName = new StringBuilder();
		
		serviceMetadata.getJavaPackageName().ifPresent(packageName -> fileName.append(packageName.replace('.', File.separatorChar)).append(File.separatorChar));
		fileName.append(serviceMetadata.getJavaServiceName());
		return fileName;
	}
	
	/**
	 * <p>
	 * Template pipe that converts protobuf comment into javadoc
	 * </p>
	 * 
	 * @param indentDepth the indentation depth
	 * 
	 * @return the protobuf comment formatted as javadoc
	 */
	public static Pipe<String, String> javadoc(int indentDepth) {
		return protoComment -> {
			char[] indentation = new char[indentDepth];
			Arrays.fill(indentation, '\t');
			return protoComment.trim().replaceAll("\\r\\n|\\r|\\n", System.lineSeparator() + new String(indentation) + " *");
		};
	}
	
	/**
	 * <p>
	 * A {@link GenericInvernoGrpcGenerator} builder.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	public static class Builder {
	
		/**
		 * Indicates whether the generator must generate client classes.
		 */
		private boolean generateClient;
		
		/**
		 * Indicates whether the generator must generate server classes.
		 */
		private boolean generateServer;
		
		/**
		 * <p>
		 * Creates a builder.
		 * </p>
		 */
		private Builder() {
			
		}
		
		/**
		 * <p>
		 * Specifies whether the generator should generate client classes.
		 * </p>
		 * 
		 * @param generateClient true to generate client classes, false otherwise
		 * 
		 * @return the builder
		 */
		public GenericInvernoGrpcGenerator.Builder generateClient(boolean generateClient) {
			this.generateClient = generateClient;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies whether the generator should generate server classes.
		 * </p>
		 * 
		 * @param generateServer true to generate server classes, false otherwise
		 * 
		 * @return the builder
		 */
		public GenericInvernoGrpcGenerator.Builder generateServer(boolean generateServer) {
			this.generateServer = generateServer;
			return this;
		}
		
		/**
		 * <p>
		 * Builds and returns the Inverno gRPC generator.
		 * </p>
		 * 
		 * @return an Inverno gRPC generator
		 */
		public GenericInvernoGrpcGenerator build() {
			return new GenericInvernoGrpcGenerator(this.generateClient, this.generateServer);
		}
	}
}
