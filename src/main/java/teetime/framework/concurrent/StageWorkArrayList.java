/***************************************************************************
 * Copyright 2014 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package teetime.framework.concurrent;

import java.util.Collection;

import teetime.framework.core.IOutputPort;
import teetime.framework.core.IPipeline;
import teetime.framework.core.IStage;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 */
public class StageWorkArrayList implements IStageWorkList {

	private static class SchedulableStage {
		public SchedulableStage() {}

		public IStage stage;
		public int numToBeExecuted;
	}

	private final int accessesDeviceId;

	/** sorted array where the last stage has highest priority */
	private final SchedulableStage[] stages;
	private int firstIndex = Integer.MAX_VALUE;
	private int lastIndex = -1;

	/**
	 * @since 1.10
	 */
	public StageWorkArrayList(final IPipeline pipeline, final int accessesDeviceId) {
		this.accessesDeviceId = accessesDeviceId;

		this.stages = new SchedulableStage[pipeline.getStages().size()];
		for (IStage stage : pipeline.getStages()) {
			final SchedulableStage schedulableStage = new SchedulableStage();
			schedulableStage.stage = stage;
			schedulableStage.numToBeExecuted = 0;
			this.stages[stage.getSchedulingIndex()] = schedulableStage;
		}
	}

	@Override
	public void pushAll(final Collection<? extends IStage> stages) {
		for (final IStage stage : stages) {
			this.push(stage);
		}
	}

	@Override
	public void pushAll(final IOutputPort<?, ?>[] outputPorts) {
		for (final IOutputPort<?, ?> outputPort : outputPorts) {
			if (outputPort != null) {
				final IStage targetStage = outputPort.getAssociatedPipe().getTargetPort().getOwningStage();
				this.push(targetStage);
			}
		}
	}

	private void push(final IStage stage) {
		if (stage.isSchedulable() && this.isValid(stage)) {
			this.firstIndex = Math.min(stage.getSchedulingIndex(), this.firstIndex);
			this.lastIndex = Math.max(stage.getSchedulingIndex(), this.lastIndex);
			this.stages[stage.getSchedulingIndex()].numToBeExecuted++;
		}
	}

	private boolean isValid(final IStage stage) {
		final boolean isValid = (stage.getAccessesDeviceId() == this.accessesDeviceId);
		if (!isValid) {
			// LOG.warn("Invalid stage: stage.accessesDeviceId = " + stage.getAccessesDeviceId() + ", accessesDeviceId = " + this.accessesDeviceId + ", stage = " +
			// stage);
		}
		return isValid;
	}

	@Override
	public IStage pop() {
		final SchedulableStage schedulableStage = this.stages[this.lastIndex];
		// schedulableStage.numToBeExecuted--;
		schedulableStage.numToBeExecuted = 0;
		cond: if (schedulableStage.numToBeExecuted == 0)
		{
			for (int i = this.lastIndex - 1; i >= this.firstIndex; i--) {
				if (this.stages[i].numToBeExecuted > 0) {
					this.lastIndex = i;
					break cond;
				}
			}
			this.firstIndex = Integer.MAX_VALUE;
			this.lastIndex = -1;
		}
		return schedulableStage.stage;
	}

	@Override
	public IStage read() {
		final SchedulableStage schedulableStage = this.stages[this.lastIndex];
		return schedulableStage.stage;
	}

	@Override
	public boolean isEmpty() {
		return this.lastIndex == -1;
	}

}
