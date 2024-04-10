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

import com.google.protobuf.DescriptorProtos;
import io.inverno.mod.irt.Pipe;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Metadata extracted from a list of protobuf file descriptors and used to represent the services to generate.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class InvernoGrpcMetadata {
	
	/**
	 * https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.9
	 */
	private static final List<CharSequence> JAVA_RESERVED_KEYWORD = List.of(
		"abstract",   "continue",   "for",          "new",         "switch",
		"assert",     "default",    "if",           "package",     "synchronized",
		"boolean",    "do",         "goto",         "private",     "this",
		"break",      "double",     "implements",   "protected",   "throw",
		"byte",       "else",       "import",       "public",      "throws",
		"case",       "enum",       "instanceof",   "return",      "transient",
		"catch",      "extends",    "int",          "short",       "try",
		"char",       "final",      "interface",    "static",      "void",
		"class",      "finally",    "long",         "strictfp",    "volatile",
		"const",      "float",      "native",       "super",       "while"
	);
	
	/**
	 * Specifies the {@code .proto} file extension
	 */
	private static final String FILE_EXTENSION_PROTO = ".proto";
	
	/**
	 * Specifies the outer class suffic added by java protobuf compiler in case of conflicting type names.
	 */
	private static final String OUTER_CLASS_SUFFIX = "OuterClass";
	
	/**
	 * A map containing the mapping between protobuf type names and java type names.
	 */
	private final Map<String, String> typesMap;

	/**
	 * The list of service metadata extracted from the protobuf file descriptors.
	 */
	private final List<ServiceMetadata> services;

	/**
	 * <p>
	 * Creates Inverno gRPC metadata from the specified list of protobuf file descriptors.
	 * </p>
	 * 
	 * @param files a list of protobuf file descriptors
	 */
	public InvernoGrpcMetadata(List<DescriptorProtos.FileDescriptorProto> files) {
		Map<String, String> tmpTypesMap = new HashMap<>();
		List<ServiceMetadata> tmpServices = new LinkedList<>();
		for(DescriptorProtos.FileDescriptorProto file : files) {
			Optional<String> protoPackageName = Optional.ofNullable(file.getPackage()).filter(s -> !s.isBlank());
			Optional<String> javaPackageName = Optional.ofNullable(file.getOptions().getJavaPackage()).filter(s -> !s.isBlank());
			Optional<String> javaOuterClassName = getJavaOuterClassName(file);
			
			file.getEnumTypeList().forEach(enumType -> putType(tmpTypesMap, protoPackageName, javaPackageName, javaOuterClassName, enumType));
			file.getMessageTypeList().forEach(messageType -> putType(tmpTypesMap, protoPackageName, javaPackageName, javaOuterClassName, messageType));
		}
		this.typesMap = Collections.unmodifiableMap(tmpTypesMap);
		
		for(DescriptorProtos.FileDescriptorProto file : files) {
			final AtomicInteger serviceIndex = new AtomicInteger();
			tmpServices.addAll(file.getServiceList().stream()
				.map(service -> new ServiceMetadata(file, service, serviceIndex.getAndIncrement()))
				.collect(Collectors.toList())
			);
		}
		this.services = Collections.unmodifiableList(tmpServices);
	}
	
	/**
	 * <p>
	 * Returns the name of the java type corresponding the specified protobuf type name.
	 * </p>
	 * 
	 * @param protoType a protobuf type name
	 * 
	 * @return a java type or null
	 */
	public String getJavaType(String protoType) {
		return this.typesMap.get(protoType);
	}

	/**
	 * <p>
	 * Returns the list of service metadata extracted from the protobuf file descriptors.
	 * </p>
	 * 
	 * @return a list of service metadata
	 */
	public List<ServiceMetadata> getServices() {
		return services;
	}
	
	/**
	 * <p>
	 * Registers the specified enum type in the specified proto to java types map.
	 * </p>
	 *
	 * @param javaTypesByProtoTypes the map in which the type should be registered
	 * @param protoPackageName      the protobuf package name
	 * @param javaPackageName       the java package name
	 * @param javaOuterClassName    the java outer class name
	 * @param enumType              the proto enum type descriptor
	 */
	private static void putType(Map<String, String> javaTypesByProtoTypes, Optional<String> protoPackageName, Optional<String> javaPackageName, Optional<String> javaOuterClassName, DescriptorProtos.EnumDescriptorProto enumType) {
		// apparently there's a '.' in front of all proto types
		javaTypesByProtoTypes.put("." + protoPackageName.map(s -> s + ".").orElse("") + enumType.getName(), javaPackageName.map(s -> s + ".").orElse("") + javaOuterClassName.map(s -> s + ".").orElse("") + enumType.getName());
	}
	
	/**
	 * <p>
	 * Registers the specified message type in the specified proto to java types map.
	 * </p>
	 *
	 * @param javaTypesByProtoTypes the map in which the type should be registered
	 * @param protoPackageName      the protobuf package name
	 * @param javaPackageName       the java package name
	 * @param javaOuterClassName    the java outer class name
	 * @param messageType           the proto message type descriptor
	 */
	private static void putType(Map<String, String> javaTypesByProtoTypes, Optional<String> protoPackageName, Optional<String> javaPackageName, Optional<String> javaOuterClassName, DescriptorProtos.DescriptorProto messageType) {
		javaTypesByProtoTypes.put("." + protoPackageName.map(s -> s + ".").orElse("") + messageType.getName(), javaPackageName.map(s -> s + ".").orElse("") + javaOuterClassName.map(s -> s + ".").orElse("") + messageType.getName());
		messageType.getEnumTypeList().forEach(enumType -> putType(javaTypesByProtoTypes, protoPackageName, javaPackageName, javaOuterClassName, enumType));
		messageType.getNestedTypeList().forEach(nestedType -> putType(javaTypesByProtoTypes, protoPackageName, javaPackageName, javaOuterClassName, nestedType));
	}
	
	/**
	 * <p>
	 * Returns the outer class name for the specified proto file descriptor.
	 * </p>
	 * 
	 * @param file a proto file descriptor
	 * 
	 * @return an optional returning the outer class name or an empty optional when {@code java_multiple_files} has been set to {@code true}
	 */
	private static Optional<String> getJavaOuterClassName(DescriptorProtos.FileDescriptorProto file) {
		if(file.getOptions().getJavaMultipleFiles()) {
			return Optional.empty();
		}
		if(file.getOptions().hasJavaOuterClassname()) {
			return Optional.of(file.getOptions().getJavaOuterClassname());
		}
		
		int fileNameIndex = file.getName().lastIndexOf('/') + 1;
		String filename = file.getName().substring(Math.max(0, fileNameIndex));
		
		final StringBuilder javaOuterClassName = new StringBuilder();
		if(filename.endsWith(FILE_EXTENSION_PROTO)) {
			javaOuterClassName.append(toJavaIdentifier(filename.substring(0, filename.length() - FILE_EXTENSION_PROTO.length()), true));
		}
		else {
			javaOuterClassName.append(toJavaIdentifier(filename, true));
		}
		
		if(Stream.of(file.getEnumTypeList(), file.getMessageTypeList(), file.getServiceList()).flatMap(List::stream).anyMatch(enumType -> javaOuterClassName.toString().equals(enumType))) {
			javaOuterClassName.append(OUTER_CLASS_SUFFIX);
		}
		return Optional.of(javaOuterClassName.toString());
	}
	
	/**
	 * <p>
	 * Converts the specified protobuf identifier to a valid java identifier.
	 * </p>
	 * 
	 * <p>
	 * This method basically:
	 * </p>
	 * 
	 * <ul>
	 * <li>uppercases or lowercases the first letter</li>
	 * <li>removes {@code _} and {@code .} characters and uppercases the next character</li>
	 * <li>uppercases characters after digits</li>
	 * <li>appends an {@code _} when the resulting identifier is a java reserved keyword.
	 * </ul>
	 * 
	 * @param protoIdentifier  a protobuf identifier
	 * @param upperFirstLetter true to uppercase the first letter, false otherwise
	 * 
	 * @return a valid java identifier
	 */
	private static String toJavaIdentifier(String protoIdentifier, boolean upperFirstLetter) {
		StringBuilder javaIdentifierBuilder = new StringBuilder();
		
		if(upperFirstLetter) {
			javaIdentifierBuilder.append(Character.toUpperCase(protoIdentifier.charAt(0)));
		}
		else {
			javaIdentifierBuilder.append(Character.toLowerCase(protoIdentifier.charAt(0)));
		}
		
		boolean upperLetter = false;
		for(int i=1;i<protoIdentifier.length();i++) {
			char c = protoIdentifier.charAt(i);
			if(c == '_' || c == '.') {
				upperLetter = true;
			}
			else if(c >= '0' && c <= '9') {
				upperLetter = true;
				javaIdentifierBuilder.append(c);
			}
			else if(upperLetter) {
				javaIdentifierBuilder.append(Character.toUpperCase(c));
				upperLetter = false;
			}
			else {
				javaIdentifierBuilder.append(c);
			}
		}
		
		String javaIdentifier = javaIdentifierBuilder.toString();
		
		return JAVA_RESERVED_KEYWORD.contains(javaIdentifier) ? javaIdentifier + '_' : javaIdentifier;
	}
	
	/**
	 * <p>
	 * Service metadata extracted from a protobuf service descriptor and used to represent a service to generate.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	public class ServiceMetadata {

		/**
		 * The protobuf package name.
		 */
		private final Optional<String> packageName;

		/**
		 * The protobuf service name.
		 */
		private final String serviceName;
		
		/**
		 * The protobuf service comment.
		 */
		private final Optional<String> serviceComment;

		/**
		 * The java package name.
		 */
		private final Optional<String> javaPackageName;

		/**
		 * The java service name deriving from the protobuf service name.
		 */
		private final String javaServiceName;

		/**
		 * Flag indicating whether the service is deprecated.
		 */
		private final boolean deprecated;

		/**
		 * The list of methods defined by the service extracted from the protobuf service descriptor.
		 */
		private final List<MethodMetadata> methods;

		/**
		 * Flag indicating whether the {@code reactor.core.publisher.Mono} class should be imported.
		 */
		private final boolean importMono;

		/**
		 * Flag indicating whether the {@code org.reactivestreams.Publisher} class should be imported.
		 */
		private final boolean importPublisher;

		/**
		 * <p>
		 * Creates Inverno gRPC service metadata from the specified protobuf service descriptor.
		 * </p>
		 *
		 * @param file         the protobuf file descriptor
		 * @param service      the protobuf service descriptor
		 * @param serviceIndex the index of the service in the protobuf file
		 */
		public ServiceMetadata(DescriptorProtos.FileDescriptorProto file, DescriptorProtos.ServiceDescriptorProto service, int serviceIndex) {
			this.packageName = Optional.ofNullable(file.getPackage()).filter(name -> !name.isBlank());
			this.serviceName = service.getName();
			this.javaServiceName = toJavaIdentifier(this.serviceName, true);
			this.javaPackageName = Optional.ofNullable(file.getOptions())
				.map(DescriptorProtos.FileOptions::getJavaPackage)
				.filter(s -> !s.isBlank());

			this.serviceComment = file.getSourceCodeInfo().getLocationList().stream()
				.filter(location -> location.getPathCount() == 2 &&
					location.getPath(0) == DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER &&
					location.getPath(1) == serviceIndex
				)
				.map(location -> location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments())
				.findFirst();

			this.deprecated = service.getOptions() != null && service.getOptions().getDeprecated();

			final AtomicInteger methodNumber = new AtomicInteger();
			AtomicBoolean hasMono = new AtomicBoolean();
			AtomicBoolean hasPublisher  = new AtomicBoolean();
			this.methods = service.getMethodList().stream()
				.map(method -> new MethodMetadata(file, method, methodNumber.getAndIncrement()))
				.peek(method -> {
					if(!hasMono.get()) {
						hasMono.set(method.isUnary() || method.isClientStreaming());
					}
					if(!hasPublisher.get()) {
						hasPublisher.set(method.isClientStreaming() || method.isServerStreaming() || method.isBidirectionalStreaming());
					}
				})
				.collect(Collectors.toList());

			this.importMono = hasMono.get();
			this.importPublisher = hasPublisher.get();
		}

		/**
		 * <p>
		 * Returns the protobuf package name.
		 * </p>
		 * 
		 * @return an optional returning the protobuf package name or an empty optional if none was specified
		 */
		public Optional<String> getPackageName() {
			return packageName;
		}

		/**
		 * <p>
		 * Returns the protobuf service name.
		 * </p>
		 * 
		 * @return the protobuf service name
		 */
		public String getServiceName() {
			return serviceName;
		}

		/**
		 * <p>
		 * Returns the protobuf service comment.
		 * </p>
		 * 
		 * @return the protobuf service comment
		 */
		public Optional<String> getServiceComment() {
			return serviceComment;
		}
		
		/**
		 * <p>
		 * Returns the java package name to be used when generating the java class.
		 * </p>
		 * 
		 * @return the java package name
		 */
		public Optional<String> getJavaPackageName() {
			return javaPackageName;
		}

		/**
		 * <p>
		 * Returns the java service name to be used when generating the java class.
		 * </p>
		 * 
		 * @return the java service name
		 */
		public String getJavaServiceName() {
			return javaServiceName;
		}

		/**
		 * <p>
		 * Determines whether the service is deprecated.
		 * </p>
		 * 
		 * @return true if the service is deprecated, false otherwise
		 */
		public boolean isDeprecated() {
			return deprecated;
		}

		/**
		 * <p>
		 * Returns the list of method metadata extracted from the protobuf service descriptors.
		 * </p>
		 * 
		 * @return a list of method metadata
		 */
		public List<MethodMetadata> getMethods() {
			return methods;
		}

		/**
		 * <p>
		 * Determines whether the {@code reactor.core.publisher.Mono} class should be imported in the generated class.
		 * </p>
		 * 
		 * <p>
		 * This is typically the case when generating unary or client streaming clients or servers.
		 * </p>
		 * 
		 * @return true to import the {@code reactor.core.publisher.Mono} class, false otherwise
		 */
		public boolean isImportMono() {
			return importMono;
		}

		/**
		 * <p>
		 * Determines whether the {@code org.reactivestreams.Publisher} class should be imported in the generated class.
		 * </p>
		 * 
		 * <p>
		 * This is typically the case when generating unary, client streaming or server streaming clients or servers.
		 * </p>
		 * 
		 * @return true to import the {@code org.reactivestreams.Publisher} class, false otherwise
		 */
		public boolean isImportPublisher() {
			return importPublisher;
		}

		/**
		 * <p>
		 * Method metadata extracted from a protobuf method descriptor and used to represent a method in a service to generate.
		 * </p>
		 * 
		 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.9
		 */
		public class MethodMetadata {

			/**
			 * The protobuf method name.
			 */
			private final String methodName;
			
			/**
			 * The protobuf method comment.
			 */
			private final Optional<String> methodComment;

			/**
			 * The java method name deriving from the protobuf method name.
			 */
			private final String javaMethodName;

			/**
			 * The java request type.
			 */
			private final String javaRequestType;

			/**
			 * The java response type.
			 */
			private final String javaResponseType;

			/**
			 * Flag indicating whether the method is deprecated.
			 */
			private final boolean deprecated;

			/**
			 * Flag indicating whether the method represents a unary RPC.
			 */
			private final boolean unary;
			/**
			 * Flag indicating whether the method represents a client streaming RPC.
			 */
			private final boolean clientStreaming;
			/**
			 * Flag indicating whether the method represents a server streaming RPC.
			 */
			private final boolean serverStreaming;
			/**
			 * Flag indicating whether the method represents a bidirectional streaming RPC.
			 */
			private final boolean bidirectionalStreaming;

			/**
			 * <p>
			 * Creates Inverno gRPC method metadata from the specified protobuf method descriptor.
			 * </p>
			 *
			 * @param file        the protobuf file descriptor
			 * @param method      the protobuf method descriptor
			 * @param methodIndex the index of the service in the protobuf service
			 */
			public MethodMetadata(DescriptorProtos.FileDescriptorProto file, DescriptorProtos.MethodDescriptorProto method, int methodIndex) {
				this.methodName = method.getName();
				this.javaMethodName = toJavaIdentifier(this.methodName, false);

				this.javaRequestType = InvernoGrpcMetadata.this.getJavaType(method.getInputType());
				this.javaResponseType = InvernoGrpcMetadata.this.getJavaType(method.getOutputType());

				this.methodComment = file.getSourceCodeInfo().getLocationList().stream()
					.filter(location -> location.getPathCount() == 4 &&
						location.getPath(3) == methodIndex
					)
					.map(location -> location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments())
					.findFirst();

				this.deprecated = method.getOptions() != null && method.getOptions().getDeprecated();

				this.unary = !method.getClientStreaming() && !method.getServerStreaming();
				this.clientStreaming = method.getClientStreaming() && !method.getServerStreaming();
				this.serverStreaming = !method.getClientStreaming() && method.getServerStreaming();
				this.bidirectionalStreaming = method.getClientStreaming() && method.getServerStreaming();
			}

			/**
			 * <p>
			 * Returns the protobuf service name.
			 * </p>
			 * 
			 * @return the protobuf service name
			 */
			public String getServiceName() {
				return InvernoGrpcMetadata.ServiceMetadata.this.getServiceName();
			}
			
			/**
			 * <p>
			 * Returns the java service name to be used when generating the java class.
			 * </p>
			 * 
			 * @return the java service name
			 */
			public String getJavaServiceName() {
				return InvernoGrpcMetadata.ServiceMetadata.this.getJavaServiceName();
			}
			
			/**
			 * <p>
			 * Returns the protobuf method name.
			 * </p>
			 * 
			 * @return the protobuf method name
			 */
			public String getMethodName() {
				return methodName;
			}

			/**
			 * <p>
			 * Returns the protobuf method comment.
			 * </p>
			 * 
			 * @return the protobuf method comment
			 */
			public Optional<String> getMethodComment() {
				return methodComment;
			}
			
			/**
			 * <p>
			 * Returns the java method name.
			 * </p>
			 * 
			 * @return the java method name
			 */
			public String getJavaMethodName() {
				return javaMethodName;
			}

			/**
			 * <p>
			 * Returns the java request type.
			 * </p>
			 * 
			 * @return the java request type
			 */
			public String getJavaRequestType() {
				return javaRequestType;
			}

			/**
			 * <p>
			 * Returns the java response type.
			 * </p>
			 * 
			 * @return the java response type
			 */
			public String getJavaResponseType() {
				return javaResponseType;
			}

			/**
			 * <p>
			 * Determines whether the method is deprecated.
			 * </p>
			 * 
			 * @return true if the method is deprecated, false otherwise
			 */
			public boolean isDeprecated() {
				return deprecated;
			}

			/**
			 * <p>
			 * Determines whether the method represents a unary RPC.
			 * </p>
			 * 
			 * @return true if the method is a unary RPC, false otherwise
			 */
			public boolean isUnary() {
				return unary;
			}

			/**
			 * <p>
			 * Determines whether the method represents a client streaming RPC.
			 * </p>
			 * 
			 * @return true if the method is a client streaming RPC, false otherwise
			 */
			public boolean isClientStreaming() {
				return clientStreaming;
			}

			/**
			 * <p>
			 * Determines whether the method represents a server streaming RPC.
			 * </p>
			 * 
			 * @return true if the method is a server streaming RPC, false otherwise
			 */
			public boolean isServerStreaming() {
				return serverStreaming;
			}

			/**
			 * <p>
			 * Determines whether the method represents a bidirectional streaming RPC.
			 * </p>
			 * 
			 * @return true if the method is a bidirectional streaming RPC, false otherwise
			 */
			public boolean isBidirectionalStreaming() {
				return bidirectionalStreaming;
			}
		}
	}
}
