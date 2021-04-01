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
package io.winterframework.tools.maven.internal;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;

/**
 * @author jkuhn
 *
 */
public abstract class Task<V> implements Callable<V> {
	
	private final Log log;
	private final PrintStream outStream;
	private final PrintStream errStream;
	private final Map<Object, Object> pluginContext;
	
	protected boolean verbose;

	@SuppressWarnings("unchecked")
	public Task(AbstractMojo mojo) {
		this.log = mojo.getLog();
		this.outStream = new PrintStream(new LogOutputStream(this.log::info));
		this.errStream = new PrintStream(new LogOutputStream(this.log::error));
		this.pluginContext = mojo.getPluginContext();
	}
	
	@Override
	public abstract V call() throws TaskExecutionException;

	public Log getLog() {
		return log;
	}

	public PrintStream getOutStream() {
		return this.outStream;
	}

	public PrintStream getErrStream() {
		return this.errStream;
	}

	public Map<Object, Object> getPluginContext() {
		return this.pluginContext;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
