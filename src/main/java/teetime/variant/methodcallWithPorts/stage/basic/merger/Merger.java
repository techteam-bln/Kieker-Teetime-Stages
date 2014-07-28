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

package teetime.variant.methodcallWithPorts.stage.basic.merger;

import teetime.variant.methodcallWithPorts.framework.core.AbstractStage;
import teetime.variant.methodcallWithPorts.framework.core.InputPort;
import teetime.variant.methodcallWithPorts.framework.core.OutputPort;
import teetime.variant.methodcallWithPorts.framework.core.Signal;

import kieker.common.record.IMonitoringRecord;

/**
 * 
 * This stage merges data from the input ports, by taking elements according to the chosen merge strategy and by putting them to the output port.
 * 
 * @author Christian Wulf
 * 
 * @since 1.10
 * 
 * @param <T>
 *            the type of the input ports and the output port
 */
public class Merger<T> extends AbstractStage {

	private final OutputPort<T> outputPort = this.createOutputPort();

	private int finishedInputPorts;

	private IMergerStrategy<T> strategy = new RoundRobinStrategy<T>();

	@Override
	public void executeWithPorts() {
		final T token = this.strategy.getNextInput(this);
		if (token == null) {
			return;
		}

		this.send(this.outputPort, token);

		boolean isReschedulable = false;
		for (InputPort<?> inputPort : this.getInputPorts()) {
			if (!inputPort.getPipe().isEmpty()) {
				isReschedulable = true;
				break;
			}
		}
		this.setReschedulable(isReschedulable);
	}

	@Override
	public void onSignal(final Signal signal, final InputPort<?> inputPort) {
		this.logger.debug("Got signal: " + signal + " from input port: " + inputPort);

		switch (signal) {
		case FINISHED:
			this.onFinished();
			break;
		default:
			this.logger.warn("Aborted sending signal " + signal + ". Reason: Unknown signal.");
			break;
		}

		if (this.finishedInputPorts == this.getInputPorts().length) {
			this.outputPort.sendSignal(signal);
		}
	}

	@Override
	public void onIsPipelineHead() {
		this.finishedInputPorts++;
	}

	public IMergerStrategy<T> getStrategy() {
		return this.strategy;
	}

	public void setStrategy(final IMergerStrategy<T> strategy) {
		this.strategy = strategy;
	}

	@Override
	public InputPort<?>[] getInputPorts() {
		return super.getInputPorts();
	}

	public InputPort<IMonitoringRecord> getNewInputPort() {
		return this.createInputPort();
	}

	public OutputPort<T> getOutputPort() {
		return this.outputPort;
	}

}
