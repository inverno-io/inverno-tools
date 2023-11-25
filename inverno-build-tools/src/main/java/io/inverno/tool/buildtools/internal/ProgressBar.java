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

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Optional;

/**
 * <p>
 * A command line progress bar used to display build progress.
 * </p>
 *
 * <p>
 * The work to complete is divided into steps with different weight that must all be done for the progress bar to complete.
 * </p>
 *
 * <p>
 * These steps can themselves be subdivided into steps to refine the progress information.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class ProgressBar {
	
	private final Step rootStep;
	
	private final PrintStream output;
	
	private boolean enabled = true;

	private float remaining = 1f;
	
	/**
	 * <p>
	 * Creates a progress bar with no description.
	 * </p>
	 */
	public ProgressBar() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates a progress bar with the specified description.
	 * </p>
	 * 
	 * @param description a description
	 */
	public ProgressBar(String description) {
		this(description, System.out);
	}
	
	/**
	 * <p>
	 * Creates a progress bar with the specified description and writing to the specified output.
	 * </p>
	 * 
	 * @param description a description
	 * @param output      the output stream where to write progress
	 */
	public ProgressBar(String description, PrintStream output) {
		this.rootStep = new Step(null, 1, description);
		this.output = output;
	}

	/**
	 * <p>
	 * Displays the progress bar explicitly regardless of whether it is enabled or not.
	 * </p>
	 */
	public void display() {
		this.output.print("\033[2K");
		if(this.remaining > 0) {
			StringBuilder progressBar = new StringBuilder();
			progressBar.append(" [");

			for (int i = 0; i < 47; i++) {
				if (i < Math.round((1 - this.remaining) * 100)) {
					progressBar.append("\u2550");
				} else if (i == Math.round((1 - this.remaining) * 100)) {
					progressBar.append(">");
				} else {
					progressBar.append(" ");
				}
			}
			progressBar.append("  ").append(NumberFormat.getPercentInstance().format(1 - this.remaining)).append(" ");
			for (int i = 54; i < 100; i++) {
				if (i < Math.round((1 - this.remaining) * 100)) {
					progressBar.append("\u2550");
				} else if (i == Math.round((1 - this.remaining) * 100)) {
					progressBar.append(">");
				} else {
					progressBar.append(" ");
				}
			}
			progressBar.append("] ");
			this.rootStep.getActiveDescription().ifPresent(progressBar::append);
			progressBar.append("\r");
			this.output.print(progressBar.toString());
		} 
		else {
			StringBuilder progressBar = new StringBuilder();
			progressBar.append(" [");
			for (int i = 0; i < 47; i++) {
				progressBar.append("\u2550");
			}
			progressBar.append(" ").append(NumberFormat.getPercentInstance().format(1 - this.remaining)).append(" ");
			for (int i = 54; i < 100; i++) {
				progressBar.append("\u2550");
			}
			progressBar.append("] ");
			this.rootStep.getDescription().ifPresent(progressBar::append);
			this.output.println(progressBar.toString());
		}
	}

	/**
	 * <p>
	 * Advances the progress bar of the specified delta.
	 * </p>
	 * 
	 * @param progress the amount of work completed
	 */
	protected void progress(float progress) {
		if (this.isEnabled() && this.remaining > 0) {
			this.remaining -= progress;
			if (this.remaining <= 0) {
				this.remaining = 0;
			} 
			this.display();
		}
	}

	/**
	 * <p>
	 * Completes the progress bar.
	 * </p>
	 */
	public void complete() {
		if (!this.isComplete()) {
			this.remaining = 0;
			this.display();
		}
	}

	/**
	 * <p>
	 * Determines whether the progress bar is enabled.
	 * </p>
	 *
	 * <p>
	 * When disabled, the progress bar can only be displayed explicitly using {@link #display()}.
	 * </p>
	 *
	 * @return true if the progress bar is enabled, false otherwise
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * <p>
	 * Enables/Disables the progress bar.
	 * </p>
	 * 
	 * @param enabled true to enable the progress bar
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * <p>
	 * Determines whether the progress bar is completed.
	 * </p>
	 * 
	 * <p>
	 * A progress bar is complete when 100% of the work has been completed.
	 * </p>
	 * 
	 * @return true if the progress bar is completed
	 */
	public boolean isComplete() {
		return this.remaining == 0;
	}

	/**
	 * <p>
	 * Adds a step with the specified unit of work at the beginning of the progress bar.
	 * </p>
	 * 
	 * @param weight the weight of the step in the work to complete
	 * 
	 * @return a new step
	 */
	public Step addStepFirst(int weight) {
		return this.addStepFirst(weight, null);
	}
	
	/**
	 * <p>
	 * Adds a step with the specified unit of work at the end of the progress bar.
	 * </p>
	 * 
	 * @param weight the weight of the step in the work to complete
	 * 
	 * @return a new step
	 */
	public Step addStep(int weight) {
		return this.addStep(weight, null);
	}
	
	/**
	 * <p>
	 * Adds a step with the specified unit of work and description at the beginning of the progress bar.
	 * </p>
	 *
	 * @param weight      the weight of the step in the work to complete
	 * @param description the description of the step
	 *
	 * @return a new step
	 */
	public Step addStepFirst(int weight, String description) {
		return this.rootStep.addStepFirst(weight, description);
	}

	/**
	 * <p>
	 * Adds a step with the specified unit of work and description at the end of the progress bar.
	 * </p>
	 *
	 * @param weight      the weight of the step in the work to complete
	 * @param description the description of the step
	 *
	 * @return a new step
	 */
	public Step addStep(int weight, String description) {
		return this.rootStep.addStep(weight, description);
	}
	
	/**
	 * <p>
	 * A step represents a share of work to complete in a progress bar.
	 * </p>
	 *
	 * <p>
	 * It can be subdivided into steps to refine the progress information in such cases the actual amount of work completed when a child step complete is calculated by multiplying the step share by
	 * the child step share.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public class Step {

		private final Step parent;

		private final int weight;

		private float remaining = 1f;

		private Optional<String> description;

		private final LinkedList<Step> children;

		/**
		 * <p>
		 * Creates a step with the specified parent step, weight and description.
		 * </p>
		 *
		 * @param parent      the parent step
		 * @param weight      the weight of the step in the work to complete represented by the parent step
		 * @param description the description of the step
		 */
		private Step(Step parent, int weight, String description) {
			this.parent = parent;
			this.weight = weight;
			this.children = new LinkedList<>();
			this.description = Optional.ofNullable(description);
		}

		/**
		 * <p>
		 * Returns the current active description.
		 * </p>
		 *
		 * <p>
		 * The active description corresponds to the description of the first deepest child step in the graph of steps.
		 * </p>
		 *
		 * @return an optional returning the active step description or an empty optional if the active step has no description
		 */
		private Optional<String> getActiveDescription() {
			if (!this.children.isEmpty()) {
				return this.children.peek().getActiveDescription();
			} else {
				return this.description;
			}
		}

		/**
		 * <p>
		 * Returns the description of the step.
		 * </p>
		 *
		 * @return an optional returning the description or an empty optional if the step has no description
		 */
		public Optional<String> getDescription() {
			return description;
		}

		/**
		 * <p>
		 * Sets the step description.
		 * </p>
		 * 
		 * @param description the description to set
		 */
		public void setDescription(String description) {
			this.description = Optional.ofNullable(description);
			ProgressBar.this.progress(0);
		}

		/**
		 * <p>
		 * Adds a step with the specified unit of work at the beginning of the step.
		 * </p>
		 * 
		 * @param weight the weight of the step in the work to complete
		 * 
		 * @return a new step
		 */
		public Step addStepFirst(int weight) {
			return this.addStepFirst(weight, null);
		}
		
		/**
		 * <p>
		 * Adds a step with the specified unit of work at the end of the step.
		 * </p>
		 * 
		 * @param weight the weight of the step in the work to complete
		 * 
		 * @return a new step
		 */
		public Step addStep(int weight) {
			return this.addStep(weight, null);
		}
		
		/**
		 * <p>
		 * Adds a step with the specified unit of work and description at the beginning of the step.
		 * </p>
		 *
		 * @param weight      the weight of the step in the work to complete
		 * @param description the description of the step
		 *
		 * @return a new step
		 */
		public Step addStepFirst(int weight, String description) {
			Step child = new Step(this, weight, description);
			this.children.addFirst(child);
			return child;
		}
		
		/**
		 * <p>
		 * Adds a step with the specified unit of work and description at the end of the step.
		 * </p>
		 *
		 * @param weight      the weight of the step in the work to complete
		 * @param description the description of the step
		 *
		 * @return a new step
		 */
		public Step addStep(int weight, String description) {
			Step child = new Step(this, weight, description);
			this.children.add(child);
			return child;
		}
		
		/**
		 * <p>
		 * Advances the current child step of the specified progress.
		 * </p>
		 * 
		 * @param child         the child step
		 * @param childProgress the amount of work completed in the child step
		 */
		protected void childProgress(ProgressBar.Step child, float childProgress) {
			if(this.children.contains(child)) {
				float progress;
				if(child.weight == 0) {
					progress = 0;
				}
				else {
					progress = this.remaining * (childProgress * (float)child.weight) / ((float)this.children.stream().filter(s -> !s.equals(child)).mapToDouble(s -> s.remaining * (float)s.weight).sum() + ((child.remaining + childProgress) * (float)child.weight));
				}

				if(child.isDone()) {
					this.children.remove(child);
				}

				if(this.children.isEmpty()) {
					progress = this.remaining;
				}
				this.progress(progress);
			}
		}

		/**
		 * <p>
		 * Advances the step of the specified progress.
		 * </p>
		 * 
		 * @param progress the amount of work completed
		 */
		protected void progress(float progress) {
			if(!this.isDone()) {
				this.remaining = Math.max(0, this.remaining - progress);
				if(this.parent != null) {
					this.parent.childProgress(this, progress);
				} 
				else {
					ProgressBar.this.progress(progress);
				}
			}
		}

		/**
		 * <p>
		 * Completes the child step and displays the progress in the parent progress bar.
		 * </p>
		 */
		public void done() {
			this.progress(this.remaining);
		}
		
		/**
		 * <p>
		 * Determines whether this step is done.
		 * </p>
		 * 
		 * @return true if the step is done, false otherwise
		 */
		public boolean isDone() {
			return this.remaining == 0;
		}
	}
}
