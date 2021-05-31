/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.tool.maven.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * <p>
 * An {@link OutputStream} implementation that writes lines of output in a Maven
 * mojo logger.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class LogOutputStream extends OutputStream {

	private final Consumer<CharSequence> logger;
	
	private ByteArrayOutputStream buffer;
	
	/**
	 * <p>
	 * Creates a Log output stream using the specified logger.
	 * </p>
	 * 
	 * @param logger the logger to use
	 */
	public LogOutputStream(Consumer<CharSequence> logger) {
		this.logger = logger;
		this.buffer = new ByteArrayOutputStream(256);
	}

	@Override
	public void write(int b) throws IOException {
		if(b == '\n') {
			this.logger.accept(new String(this.buffer.toByteArray()));
			this.buffer.reset();
		}
		else if(b != '\r') {
			this.buffer.write(b);
		}
	}

}
