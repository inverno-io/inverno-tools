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
package io.inverno.tool.protoc.plugin;

import io.inverno.tool.grpc.protocplugin.InvernoGrpcProtocPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public final class ProtocGrpcRunner {
	
	private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

	private static final Path CLASSES_PATH = Path.of("target/classes").toAbsolutePath();
	
	private static final Path PROTOC_PATH = Path.of("target/protoc").toAbsolutePath();
	
	private static final Path GRPC_OUTPUT_PATH = Path.of("target/test-grpc-out");
	
	private static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
	
	private static final String PROTOC;
	
	static {
		try {
			Path protocPath = Files.list(PROTOC_PATH).filter(p -> p.getFileName().toString().endsWith(".exe")).findFirst().orElseThrow(() -> new RuntimeException("Cant't resolve protoc.exe")).toAbsolutePath();
			Files.setPosixFilePermissions(protocPath, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
			PROTOC = protocPath.toString();
		}
		catch(IOException e) {
			throw new RuntimeException("Can't resolve protoc.exe", e);
		}
	}
	
	private static final String INVERNO_GRPC_PROTOC_PLUGIN;
	
	static {
		StringBuilder invernoGrpcProtocPluginCommand = new StringBuilder();
		
		invernoGrpcProtocPluginCommand.append(IS_WINDOWS ? "" : "#!/bin/sh").append(System.lineSeparator());
		
		invernoGrpcProtocPluginCommand.append(JAVA);
		invernoGrpcProtocPluginCommand.append(" --class-path ").append(CLASSES_PATH.toString()).append(File.pathSeparator).append(System.getProperty("grpc.protoc.plugin.classpath"));
		invernoGrpcProtocPluginCommand.append(" ").append(InvernoGrpcProtocPlugin.class.getCanonicalName());
		
		invernoGrpcProtocPluginCommand.append(IS_WINDOWS ? " %*" : " \"$@\"").append(System.lineSeparator());
		
		try {
			Path invernoGrpcProtocPluginPath = PROTOC_PATH.resolve(IS_WINDOWS ? "invernoGrpcProtocPlugin.bat" : "invernoGrpcProtocPlugin.sh");
			Files.write(invernoGrpcProtocPluginPath, invernoGrpcProtocPluginCommand.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.setPosixFilePermissions(invernoGrpcProtocPluginPath, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
			INVERNO_GRPC_PROTOC_PLUGIN = invernoGrpcProtocPluginPath.toString();
		}
		catch(IOException e) {
			throw new RuntimeException("Can't create invernoGrpcProtocPlugin.sh", e);
		}
	}
	
	private ProtocGrpcRunner() {
	}

	/**
	 * <p>
	 * Runs {@code protoc.exe} with the {@link InvernoGrpcProtocPlugin} and generates files to {@code target/test-grpc-out}.
	 * </p>
	 *
	 * {@code protoc --grpc_out=target/test-grpc-out --plugin=protoc-gen-grpc="java --class-path <classpath> io.inverno.tool.grpc.InvernoGrpcProtocPlugin <args>" <protoPath>}
	 *
	 * @param protoPath            the path where to look for import
	 * @param grpcProtocPluginArgs arguments to pass to the Inverno gRPC protoc plugin
	 * @param protoFilePaths       paths to {@code .proto} files to compile
	 *
	 * @return protoc exit code
	 */
	public static int runProtocGrpc(Path protoPath, String grpcProtocPluginArgs, Path... protoFilePaths) throws IOException, InterruptedException {
		Files.createDirectories(GRPC_OUTPUT_PATH);

		Path invernoGrpcProtocPluginPath = Files.createTempFile("invernoGrpcProtocPlugin", ".sh", PosixFilePermissions.asFileAttribute(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)));
		try {
			StringBuilder invernoGrpcProtocPluginCommand = new StringBuilder();
			invernoGrpcProtocPluginCommand.append(IS_WINDOWS ? "" : "#!/bin/sh").append(System.lineSeparator());
			invernoGrpcProtocPluginCommand.append(INVERNO_GRPC_PROTOC_PLUGIN).append(" ").append(grpcProtocPluginArgs);
			Files.write(invernoGrpcProtocPluginPath, invernoGrpcProtocPluginCommand.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

			List<String> protoc_command = new LinkedList<>();

			protoc_command.add(PROTOC);
			protoc_command.add("--proto_path=" + protoPath.toAbsolutePath().toString());
			protoc_command.add("--plugin=protoc-gen-grpc=" + invernoGrpcProtocPluginPath.toAbsolutePath().toString());
			protoc_command.add("--grpc_out=" + GRPC_OUTPUT_PATH.toString());
			Arrays.stream(protoFilePaths).map(path -> path.toAbsolutePath().toString()).forEach(protoc_command::add);

			ProcessBuilder pb = new ProcessBuilder(protoc_command);

			return pb.start().waitFor();
		}
		finally {
			Files.deleteIfExists(invernoGrpcProtocPluginPath);
		}
	}
}
