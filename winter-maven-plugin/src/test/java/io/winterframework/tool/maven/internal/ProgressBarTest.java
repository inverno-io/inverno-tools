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

import org.junit.jupiter.api.Test;

import io.winterframework.tool.maven.internal.ProgressBar;
import io.winterframework.tool.maven.internal.ProgressBar.Step;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class ProgressBarTest {

	@Test
	void test() throws Exception {
		
		ProgressBar bar = new ProgressBar();
		
		Step step1 = bar.addStep(0.5f, "step1");
		
		Step step11 = step1.addStep(0.5f, "step11");
		Step step12 = step1.addStep(0.5f, "step12");
		
		Step step2 = bar.addStep(0.5f, "step2");
		
		Step step21 = step2.addStep(0.5f, "step21");
		Step step22 = step2.addStep(0.5f, "step22");
		
		bar.display();
		
		Thread.sleep(10);
		step11.done();
		
		Thread.sleep(10);
		step12.done();
		
		Thread.sleep(10);
		step21.done();
		
		Thread.sleep(10);
		step22.done();
	}

}
