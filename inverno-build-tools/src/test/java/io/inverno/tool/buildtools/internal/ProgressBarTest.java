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
package io.inverno.tool.buildtools.internal;

import io.inverno.tool.buildtools.internal.ProgressBar.Step;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ProgressBarTest {

	@Test
	public void test() throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ProgressBar bar = new ProgressBar("Test Progress Bar", new PrintStream(bout));
		
		Step step1 = bar.addStep(50, "step1");
		
		Step step11 = step1.addStep(30, "step11");
		Step step12 = step1.addStep(30, "step12");
//		Step step13 = step1.addStep(40, "step13");
		
		Step step2 = bar.addStep(50, "step2");
		
		Step step21 = step2.addStep(50, "step21");
		Step step22 = step2.addStep(50, "step22");
		
		bar.display();
		
//		Thread.sleep(10);
		step11.done();
		
//		Thread.sleep(10);
		step12.done();
		
//		step13.done();
		
//		Thread.sleep(10);
		step21.done();
		
//		Thread.sleep(10);
		step22.done();
		
		String expected = "\u001B[2K [>                                                0\u00A0%                                               ] step11\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                       25\u00A0%                                               ] step12\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550  50\u00A0%                                               ] step21\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550  75\u00A0% \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                        ] step22\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550 100\u00A0% \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550] Test Progress Bar\n";

		Assertions.assertEquals(expected, new String(bout.toByteArray()));
	}
	
	@Test
	public void testActualTasks() throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ProgressBar bar = new ProgressBar("Project application complete", new PrintStream(bout));
		
		Step modularizeDepsStep = bar.addStep(25, "");
		Step jmodStep = bar.addStep(25, "");
		Step runtimeStep = bar.addStep(25, "");
		Step applicationStep = bar.addStep(25, "");
		
		bar.display();
		
		modularizeDepsStep.setDescription("Modularizing, compiling and repackaging project dependencies...");
		
		ProgressBar.Step modularizeStep = modularizeDepsStep.addStep(80, "Modularizing project dependencies...");
		ProgressBar.Step compileStep = modularizeDepsStep.addStep(10, "Compiling project dependencies...");
		ProgressBar.Step repackageStep = modularizeDepsStep.addStep(10, "Repackaging project dependencies...");
		
		modularizeStep.done();
//		Thread.sleep(10);
		
		compileStep.done();
//		Thread.sleep(10);

		repackageStep.done();
//		Thread.sleep(10);
		
		jmodStep.setDescription("Creating project jmod...");
		jmodStep.done();
//		Thread.sleep(10);
		
		runtimeStep.setDescription("Creating runtime...");
		runtimeStep.done();
//		Thread.sleep(10);
		
		applicationStep.setDescription("Creating project application...");
		applicationStep.done();
		
		String expected = "\u001B[2K [>                                                0\u00A0%                                               ] \r"
			+ "\u001B[2K [>                                                0\u00A0%                                               ] Modularizing, compiling and repackaging project dependencies...\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                            20\u00A0%                                               ] Compiling project dependencies...\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                          22\u00A0%                                               ] Repackaging project dependencies...\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                       25\u00A0%                                               ] \r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                       25\u00A0%                                               ] Creating project jmod...\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550  50\u00A0%                                               ] \r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550  50\u00A0%                                               ] Creating runtime...\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550  75\u00A0% \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                        ] \r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550  75\u00A0% \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550>                        ] Creating project application...\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550 100\u00A0% \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550] Project application complete\n";

		Assertions.assertEquals(expected, new String(bout.toByteArray()));
	}
	
	@Test
	public void testWithZeroWeights() throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ProgressBar bar = new ProgressBar("Zero Weight", new PrintStream(bout));
		
		Step step1 = bar.addStep(0, "Step 1");
		Step step2 = bar.addStep(0, "Step 2");
		
		bar.display();
		
		step1.done();
//		Thread.sleep(10);
		
		step2.done();
//		Thread.sleep(10);
		
		String expected = "\u001B[2K [>                                                0\u00A0%                                               ] Step 1\r"
			+ "\u001B[2K [>                                                0\u00A0%                                               ] Step 2\r"
			+ "\u001B[2K [\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550 100\u00A0% \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550] Zero Weight\n";
		
		Assertions.assertEquals(expected, new String(bout.toByteArray()));
	}
}
