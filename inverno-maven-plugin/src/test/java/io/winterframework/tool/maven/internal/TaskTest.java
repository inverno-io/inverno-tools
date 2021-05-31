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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.inverno.tool.maven.internal.Task;
import io.inverno.tool.maven.internal.TaskExecutionException;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class TaskTest {

	@Test
	public void testTranslateArguments() {
		Log log = Mockito.mock(Log.class);
		
		AbstractMojo mojo = Mockito.mock(AbstractMojo.class);
		Mockito.when(mojo.getLog()).thenReturn(log);
		
		Task<Void> task = new Task<Void>(mojo) {
			@Override
			protected Void execute() throws TaskExecutionException {
				return null;
			}
		};
		
		String arguments = "--property=\\\"hello\\\" a b c";
		List<String> command = task.translateArguments(arguments);
		Iterator<String> commandIterator = command.iterator();
		
		Assertions.assertEquals(4, command.size());
		Assertions.assertEquals("--property=\"hello\"", commandIterator.next());
		Assertions.assertEquals("a", commandIterator.next());
		Assertions.assertEquals("b", commandIterator.next());
		Assertions.assertEquals("c", commandIterator.next());
		
		arguments = "--property=\\\"hello\\ world\\\" a b c";
		command = task.translateArguments(arguments);
		commandIterator = command.iterator();
		
		Assertions.assertEquals(4, command.size());
		Assertions.assertEquals("--property=\"hello world\"", commandIterator.next());
		Assertions.assertEquals("a", commandIterator.next());
		Assertions.assertEquals("b", commandIterator.next());
		Assertions.assertEquals("c", commandIterator.next());
		
		arguments = "'--property=\"hello world\"' a b c";
		command = task.translateArguments(arguments);
		commandIterator = command.iterator();
		
		Assertions.assertEquals(4, command.size());
		Assertions.assertEquals("--property=\"hello world\"", commandIterator.next());
		Assertions.assertEquals("a", commandIterator.next());
		Assertions.assertEquals("b", commandIterator.next());
		Assertions.assertEquals("c", commandIterator.next());
		
		arguments = "\"--property=\\\"hello world\\\"\" a b c";
		command = task.translateArguments(arguments);
		commandIterator = command.iterator();
		
		Assertions.assertEquals(4, command.size());
		Assertions.assertEquals("--property=\"hello world\"", commandIterator.next());
		Assertions.assertEquals("a", commandIterator.next());
		Assertions.assertEquals("b", commandIterator.next());
		Assertions.assertEquals("c", commandIterator.next());
		
		arguments = "--property=\\\"hello\\\" a\\ b c";
		command = task.translateArguments(arguments);
		commandIterator = command.iterator();
		
		Assertions.assertEquals(3, command.size());
		Assertions.assertEquals("--property=\"hello\"", commandIterator.next());
		Assertions.assertEquals("a b", commandIterator.next());
		Assertions.assertEquals("c", commandIterator.next());
		
		arguments = "'--property=\"hello\"' \"a\\\" b\" c";
		command = task.translateArguments(arguments);
		commandIterator = command.iterator();
		
		Assertions.assertEquals(3, command.size());
		Assertions.assertEquals("--property=\"hello\"", commandIterator.next());
		Assertions.assertEquals("a\" b", commandIterator.next());
		Assertions.assertEquals("c", commandIterator.next());
		
		arguments = "'--property=\"hello\"' 'a\\' b' c";
		command = task.translateArguments(arguments);
		commandIterator = command.iterator();
		
		Assertions.assertEquals(3, command.size());
		Assertions.assertEquals("--property=\"hello\"", commandIterator.next());
		Assertions.assertEquals("a' b", commandIterator.next());
		Assertions.assertEquals("c", commandIterator.next());
	}

}
