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

import java.io.FileOutputStream;

/**
 * <p>
 * A stupid and simple protoc plugin to create a protoc dump:
 * </p>
 * 
 * <pre>{@code 
 * $ protoc.exe \ 
 * --plugin=protoc-gen-dump=protocdumpplugin \ 
 * --include_source_info \
 * --descriptor_set_out=./desc.out \
 * --dump_out=./ helloworld.proto 
 * }</pre>
 * 
 * <p>
 * where {@code loggingprotocplugin} is the native executable compiled with GraalVM. Above command generates file {@code protoc.dump} that can be used to test {@link InvernoGrpcGenerator}.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ProtocDumpPlugin {

	public static void main(String[] args) throws Exception {
		try(FileOutputStream fout = new FileOutputStream("protoc.dump")) {
			int b;
			while( (b = System.in.read()) != -1 ) {
				fout.write(b);
			}
			fout.flush();
		}
	}
}
