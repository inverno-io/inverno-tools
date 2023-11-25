/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.tool.buildtools.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

/**
 * <p>
 * A utilty class holding Java tools references as well as utility methods to manipulate command line arguments.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public final class JavaTools {

	/**
	 * {@code javac} tool provider.
	 */
	public static final ToolProvider JAVAC;
	/**
	 * {@code jar} tool provider.
	 */
	public static final ToolProvider JAR;
	/**
	 * {@code jdeps} tool provider.
	 */
	public static final ToolProvider JDEPS;
	/**
	 * {@code jmod} tool provider.
	 */
	public static final ToolProvider JMOD;
	/**
	 * {@code jlink} tool provider.
	 */
	public static final ToolProvider JLINK;
	/**
	 * {@code jpackage} tool provider.
	 */
	public static final ToolProvider JPACKAGE;
	
	static {
		ToolProvider javac = null;
		ToolProvider jar = null;
		ToolProvider jdeps = null;
		ToolProvider jmod = null;
		ToolProvider jlink = null;
		ToolProvider jpackage = null;
		
		for(ToolProvider toolProvider : ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader())) {
			switch(toolProvider.name()) {
				case "javac":
					javac = toolProvider;
					break;
				case "jar":
					jar = toolProvider;
					break;
				case "jdeps":
					jdeps = toolProvider;
					break;
				case "jmod":
					jmod = toolProvider;
					break;
				case "jlink":
					jlink = toolProvider;
					break;
				case "jpackage":
					jpackage = toolProvider;
					break;
			}
		}
		
		JAVAC = javac;
		JAR = jar;
		JDEPS = jdeps;
		JMOD = jmod;
		JLINK = jlink;
		JPACKAGE = jpackage;
	}
	
	/**
	 * <p>
	 * Sanitizes the specified command line arguments.
	 * </p>
	 * 
	 * @param arguments the command line arguments to sanitize
	 * 
	 * @return sanitized command line arguments
	 */
	public static String sanitizeArguments(String arguments) {
		if(arguments == null) {
			return null;
		}
		return arguments.replace('\n', ' ').replace('\t', ' ');
	}
	
	/**
	 * <p>
	 * Translates the specified command line arguments into a list of command
	 * arguments.
	 * </p>
	 * 
	 * <p>
	 * The specified arguments string is split around spaces. In order to define an
	 * arguments with spaces, it must be quoted using single or double quotes:
	 * {@code 'Hello world'} or {@code "Hello world"}; or spaces can also be
	 * escaped: {@code Hello\ world}.
	 * </p>
	 * 
	 * <p>
	 * Since quotes and double quotes are also used as delimiters, they might need
	 * to be escaped to define arguments which contain some: {@code "I'm happy"} and
	 * {@code 'I\'m happy'}
	 * </p>
	 * 
	 * @param arguments a command line arguments string
	 * 
	 * @return a list of command arguments
	 * @throws IllegalArgumentException if the specified arguments string is invalid 
	 */
	public static List<String> translateArguments(String arguments) throws IllegalArgumentException {
		List<String> command = new LinkedList<>();
		
		final int state_default = 0;
		final int state_inquote = 1;
		final int state_indoublequote = 2;
		boolean escape = false;
		
		char[] chars = arguments.toCharArray();
		
		int state = state_default;
		StringBuilder current = new StringBuilder();
		
		for(int i=0;i<chars.length;i++) {
			char nextChar = chars[i];
			if(nextChar == '\\') {
				if(escape) {
					current.append(nextChar);
				}
				escape = true;
				continue;
			}
			
			switch(state) {
				case state_inquote: {
					if(nextChar == '\'' && !escape) {
						state = state_default;
					}
					else {
						escape = false;
						current.append(nextChar);
					}
					break;
				}
				case state_indoublequote: {
					if(nextChar == '"' && !escape) {
						state = state_default;
					}
					else {
						escape = false;
						current.append(nextChar);
					}
					break;
				}
				default: {
					if(nextChar == '\'') {
						if(!escape) {
							state = state_inquote;
						}
						else {
							current.append(nextChar);
							escape = false;
						}
					}
					else if(nextChar == '"') {
						if(!escape) {
							state = state_indoublequote;
						}
						else {
							current.append(nextChar);
							escape = false;
						}
					}
					else if(nextChar == ' ') {
						if(!escape) {
							if(current.length() > 0) {
								command.add(current.toString());
	                            current.setLength(0);
	                        }
						}
						else {
							escape = false;
							current.append( nextChar );
						}
					}
					else {
						if(escape) {
							escape = false;
							current.append('\\');
						}
                        current.append( nextChar );
                    }
				}
			}
		}

		if(escape) {
			current.append('\\');
		}
		
		if (current.length() > 0) {
			command.add(current.toString());
		}

		if ((state == state_inquote) || (state == state_indoublequote)) {
			throw new IllegalArgumentException("Unterminated quote");
		}
		return command;
	}
}
