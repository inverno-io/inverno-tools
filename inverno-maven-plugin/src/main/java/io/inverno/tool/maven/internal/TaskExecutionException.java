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

/**
 * <p>
 * Thrown to indicate an error during the execution of a task.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class TaskExecutionException extends Exception {

	private static final long serialVersionUID = -3209213874339409208L;

	/**
	 * <p>
	 * Creates a task execution exception.
	 * </p>
	 */
	public TaskExecutionException() {
	}

	/**
	 * <p>
	 * Creates a task execution exception.
	 * </p>
	 * 
	 * @param message a message
	 */
	public TaskExecutionException(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Creates a task execution exception.
	 * </p>
	 * 
	 * @param cause a cause
	 */
	public TaskExecutionException(Throwable cause) {
		super(cause);
	}

	/**
	 * <p>
	 * Creates a task execution exception.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   a cause
	 */
	public TaskExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}
