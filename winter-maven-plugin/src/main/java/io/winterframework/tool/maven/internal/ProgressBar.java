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

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Optional;

/**
 * <p>
 * A command line progress bar used to display build progress.
 * </p>
 * 
 * <p>
 * The work to complete is divided into steps with different weight that must
 * all be done for the progress bar to complete.
 * </p>
 * 
 * <p>
 * These steps can themselves be subdivided into steps to refine the progress
 * information.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class ProgressBar {

	private boolean enabled = true;

	private Step rootStep = new Step(null, 1, null);

	private float currentProgress;

	/**
	 * <p>
	 * Displays the progress bar regardless of whether it is enabled.
	 * </p>
	 */
	public void display() {
		System.out.print("\033[2K");
		if (this.currentProgress < 1) {
			StringBuilder progressBar = new StringBuilder();
			progressBar.append(" [");

			for (int i = 0; i < 47; i++) {
				if (i < Math.round(this.currentProgress * 100)) {
					progressBar.append("\u2550");
				} else if (i == Math.round(this.currentProgress * 100)) {
					progressBar.append(">");
				} else {
					progressBar.append(" ");
				}
			}
			progressBar.append("  ").append(NumberFormat.getPercentInstance().format(this.currentProgress)).append(" ");
			for (int i = 54; i < 100; i++) {
				if (i < Math.round(this.currentProgress * 100)) {
					progressBar.append("\u2550");
				} else if (i == Math.round(this.currentProgress * 100)) {
					progressBar.append(">");
				} else {
					progressBar.append(" ");
				}
			}
			progressBar.append("] ");
			this.rootStep.getActiveDescription().ifPresent(progressBar::append);
			progressBar.append("\r");
			System.out.print(progressBar.toString());
		} else {
			StringBuilder progressBar = new StringBuilder();
			progressBar.append(" [");
			for (int i = 0; i < 47; i++) {
				progressBar.append("\u2550");
			}
			progressBar.append(" ").append(NumberFormat.getPercentInstance().format(this.currentProgress)).append(" ");
			for (int i = 54; i < 100; i++) {
				progressBar.append("\u2550");
			}
			progressBar.append("] ");
			this.rootStep.getDescription().ifPresent(progressBar::append);
			System.out.println(progressBar.toString());
		}
	}

	/**
	 * <p>
	 * Advances the progress bar of the specified delta.
	 * </p>
	 * 
	 * @param delta the amount of work completed
	 */
	protected void progress(float delta) {
		if (this.isEnabled() && this.currentProgress < 1) {
			this.currentProgress += delta;
			if (this.currentProgress < 1) {
				this.display();
			} else {
				this.currentProgress = 1;
				this.display();
			}
		}
	}

	/**
	 * <p>
	 * Completes the progress bar.
	 * </p>
	 */
	public void complete() {
		if (!this.isComplete()) {
			this.currentProgress = 1;
			this.display();
		}
	}

	/**
	 * <p>
	 * Determines whether the progress bar is enabled.
	 * </p>
	 * 
	 * <p>
	 * When disabled, the progress bar can only be displayed explicitly using
	 * {@link #display()}.
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
		return this.currentProgress >= 1;
	}

	/**
	 * <p>
	 * Adds a step with the specified unit of work to the progress bar.
	 * </p>
	 * 
	 * @param weight the weight of the step between 0 and 1 in the work to complete
	 * 
	 * @return a new step
	 */
	public Step addStep(float weight) {
		return this.addStep(weight, null);
	}

	/**
	 * <p>
	 * Adds a step with the specified unit of work and description to the progress
	 * bar.
	 * </p>
	 * 
	 * @param weight      the weight of the step between 0 and 1 in the work to
	 *                    complete
	 * @param description the description of the step
	 * 
	 * @return a new step
	 */
	public Step addStep(float weight, String description) {
		return this.rootStep.addStep(weight, description);
	}

	/**
	 * <p>
	 * Adds a step with the specified unit of work to the progress bar.
	 * </p>
	 * 
	 * @param weight the share of the step in the total work to complete
	 * @param total  the total work to complete
	 * 
	 * @return a new step
	 */
	public Step addStep(int weight, int total) {
		return this.addStep((float) weight / total);
	}

	/**
	 * <p>
	 * Adds a step with the specified unit of work and description to the progress
	 * bar.
	 * </p>
	 * 
	 * @param weight      the share of the step in the total work to complete
	 * @param total       the total work to complete
	 * @param description the description of the step
	 * 
	 * @return a new step
	 */
	public Step addStep(int weight, int total, String description) {
		return this.addStep((float) weight / total);
	}

	/**
	 * <p>
	 * A step represents a share of work to complete in a progress bar.
	 * </p>
	 * 
	 * <p>
	 * It can be subdivided into steps to refine the progress information in such
	 * cases the actual amount of work completed when a child step complete is
	 * calculated by multiplying the step share by the child step share.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public class Step {

		private final Step parent;

		private final float weight;

		private float progress;

		private Optional<String> description;

		private final LinkedList<Step> children;

		private boolean done;

		/**
		 * <p>
		 * Creates a step with the specified parent step, weight and description.
		 * </p>
		 * 
		 * @param parent      the parent step
		 * @param weight      the weight of the step between 0 and 1 in the work to
		 *                    complete represented by the parent step
		 * @param description the description of the step
		 */
		private Step(Step parent, float weight, String description) {
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
		 * The active description corresponds to the description of the first deepest
		 * child step in the graph of steps.
		 * </p>
		 * 
		 * @return an optional returning the active step description or an empty
		 *         optional if the active step has no description
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
		 * @return an optional returning the description or an empty optional if the
		 *         step has no description
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
		 * Adds a step with the specified unit of work to the step.
		 * </p>
		 * 
		 * @param weight the weight of the step between 0 and 1 in the work to complete
		 * 
		 * @return a new step
		 */
		public Step addStep(float weight) {
			return this.addStep(weight, null);
		}
		
		/**
		 * <p>
		 * Adds a step with the specified unit of work and description to the step.
		 * </p>
		 * 
		 * @param weight      the weight of the step between 0 and 1 in the work to
		 *                    complete
		 * @param description the description of the step
		 * 
		 * @return a new step
		 */
		public Step addStep(float weight, String description) {
			Step child = new Step(this, weight, description);
			this.children.add(child);
			ProgressBar.this.progress(0);
			return child;
		}

		/**
		 * <p>
		 * Adds a step with the specified unit of work to the step.
		 * </p>
		 * 
		 * @param weight the share of the step in the total work to complete
		 * @param total  the total work to complete
		 * 
		 * @return a new step
		 */
		public Step addStep(int weight, int total) {
			return this.addStep((float) weight / total, null);
		}

		/**
		 * <p>
		 * Adds a step with the specified unit of work and description to the step.
		 * </p>
		 * 
		 * @param weight      the share of the step in the total work to complete
		 * @param total       the total work to complete
		 * @param description the description of the step
		 * 
		 * @return a new step
		 */
		public Step addStep(int weight, int total, String description) {
			return this.addStep((float) weight / total, description);
		}

		/**
		 * <p>
		 * Advances the progress of the specified delta.
		 * </p>
		 * 
		 * @param delta the amount of work completed
		 */
		protected void progress(float delta) {
			if (this.parent != null) {
				this.parent.progress(this.weight * delta);
				this.progress += this.weight * delta;
			} else {
				ProgressBar.this.progress(delta);
			}
		}

		/**
		 * <p>
		 * Indicates that the specified child step has completed.
		 * </p>
		 * 
		 * @param child the completed child step
		 */
		protected void childDone(Step child) {
			this.children.remove(child);
			this.progress(child.weight - child.progress);
			if (this.children.isEmpty()) {
				this.done();
			}
		}

		/**
		 * <p>
		 * Completes the child step and displays the progress in the parent progress
		 * bar.
		 * </p>
		 */
		public void done() {
			if (!this.done) {
				this.done = true;
				if (this.parent != null) {
					this.parent.childDone(this);
				} else {
					ProgressBar.this.complete();
				}
			}
		}
	}
}
