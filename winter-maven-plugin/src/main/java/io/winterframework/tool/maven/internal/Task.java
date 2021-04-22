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
package io.winterframework.tool.maven.internal;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;

/**
 * <p>
 * Represents a particular task in a winter plugin build process.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <V> the type of the task execution result
 */
public abstract class Task<V> implements Callable<V> {
	
	private final Log log;
	private final PrintStream outStream;
	private final PrintStream errStream;
	
	private Optional<ProgressBar.Step> step = Optional.empty();
	
	protected boolean verbose;

	/**
	 * <p>
	 * Creates a task
	 * </p>
	 * 
	 * @param mojo the parent mojo
	 */
	public Task(AbstractMojo mojo) {
		this.log = mojo.getLog();
		this.outStream = new PrintStream(new LogOutputStream(this.log::info));
		this.errStream = new PrintStream(new LogOutputStream(this.log::error));
	}
	
	@Override
	public V call() throws TaskExecutionException {
		try {
			return this.execute();
		}
		finally {
			this.step.ifPresent(ProgressBar.Step::done);
		}
	}
	
	/**
	 * <p>
	 * Executes the task.
	 * </p>
	 * 
	 * @return the execution result
	 * 
	 * @throws TaskExecutionException if there was an error during the execution of
	 *                                the task
	 */
	protected abstract V execute() throws TaskExecutionException;

	/**
	 * <p>
	 * Sanitizes the specified command line arguments.
	 * </p>
	 * 
	 * @param arguments the command line arguments to sanitize
	 * 
	 * @return sanitized command line arguments
	 */
	protected String sanitizeArguments(String arguments) {
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
	protected List<String> translateArguments(String arguments) throws IllegalArgumentException {
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
	
	/**
	 * <p>
	 * Returns the task logger.
	 * </p>
	 * 
	 * @return the task logger
	 */
	public Log getLog() {
		return log;
	}
	
	/**
	 * <p>
	 * Set the progress bar step associated to the task.
	 * </p>
	 * 
	 * @param step the task progress step
	 */
	public void setStep(ProgressBar.Step step) {
		this.step = Optional.ofNullable(step);
	}
	
	/**
	 * <p>
	 * Returns the progress step associated to the task.
	 * </p>
	 * 
	 * @return an optional returning the progress step or an empty optional if
	 *         there is none
	 */
	public Optional<ProgressBar.Step> getStep() {
		return this.step;
	}

	/**
	 * <p>
	 * Returns the task output stream.
	 * </p>
	 * 
	 * <p>
	 * The returned stream outputs lines as info logs using the task logger.
	 * </p>
	 * 
	 * @return the task output stream
	 */
	public PrintStream getOutStream() {
		return this.outStream;
	}

	/**
	 * <p>
	 * Returns the task error stream.
	 * </p>
	 * 
	 * <p>
	 * The returned stream outputs lines as error logs using the task logger.
	 * </p>
	 * 
	 * @return the task error stream
	 */
	public PrintStream getErrStream() {
		return this.errStream;
	}

	/**
	 * <p>
	 * Determines whether verbose logging is enabled.
	 * </p>
	 * 
	 * @return true if verbose logging is enabled, false otherwise
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * <p>
	 * Enables/disables verbose logging.
	 * </p>
	 * 
	 * @param verbose true to enable verbose logging, false otherwise
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
