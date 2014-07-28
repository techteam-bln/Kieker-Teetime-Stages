package teetime.variant.methodcallWithPorts.stage;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import teetime.util.StopWatch;
import teetime.variant.methodcallWithPorts.framework.core.ConsumerStage;
import teetime.variant.methodcallWithPorts.framework.core.OutputPort;

public class Cache<T> extends ConsumerStage<T> {

	private final OutputPort<T> outputPort = this.createOutputPort();

	private final List<T> cachedObjects = new LinkedList<T>();

	@Override
	protected void execute(final T element) {
		this.cachedObjects.add(element);
	}

	@Override
	public void onIsPipelineHead() {
		this.logger.debug("Emitting " + this.cachedObjects.size() + " cached elements...");
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (T cachedElement : this.cachedObjects) {
			this.send(this.outputPort, cachedElement);
		}
		stopWatch.end();
		this.logger.debug("Emitting took " + TimeUnit.NANOSECONDS.toMillis(stopWatch.getDurationInNs()) + " ms");
		super.onIsPipelineHead();
	}

	public OutputPort<T> getOutputPort() {
		return this.outputPort;
	}

}
