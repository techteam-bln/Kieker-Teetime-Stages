package teetime.examples.throughput.methodcall.stage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import teetime.examples.throughput.methodcall.InputPort;
import teetime.examples.throughput.methodcall.OnDisableListener;
import teetime.examples.throughput.methodcall.OutputPort;
import teetime.examples.throughput.methodcall.SchedulingInformation;
import teetime.examples.throughput.methodcall.Stage;
import teetime.examples.throughput.methodcall.StageWithPort;
import teetime.util.list.CommittableQueue;

public class Pipeline<I, O> implements StageWithPort<I, O>, OnDisableListener {

	private StageWithPort firstStage;
	private final List<StageWithPort> intermediateStages = new LinkedList<StageWithPort>();
	private StageWithPort lastStage;

	private final SchedulingInformation schedulingInformation = new SchedulingInformation();

	private StageWithPort[] stages;
	private Stage parentStage;
	private int index;
	private int startIndex;
	private OnDisableListener listener;

	private boolean reschedulable;
	private int firstStageIndex;

	public void setFirstStage(final StageWithPort<I, ?> stage) {
		this.firstStage = stage;
	}

	public void addIntermediateStages(final StageWithPort... stages) {
		this.intermediateStages.addAll(Arrays.asList(stages));
	}

	public void addIntermediateStage(final StageWithPort stage) {
		this.intermediateStages.add(stage);
	}

	public void setLastStage(final StageWithPort<?, O> stage) {
		this.lastStage = stage;
	}

	@Override
	public CommittableQueue<O> execute2(final CommittableQueue<I> elements) {
		// CommittableQueue queue = this.firstStage.execute2(elements);
		// for (Stage<?, ?> stage : this.intermediateStages) {
		// queue = stage.execute2(queue);
		// }
		// return this.lastStage.execute2(queue);

		// below is faster than above (probably because of the instantiation of a list iterator in each (!) execution)
		CommittableQueue queue = elements;

		// for (int i = this.startIndex; i < this.stages.length; i++) {
		// Stage<?, ?> stage = this.stages[i];
		// queue = stage.execute2(queue);
		// }

		this.stages[0].execute2(elements);
		this.setReschedulable(this.stages[0].isReschedulable());
		return queue;
	}

	@Override
	public void executeWithPorts() {
		StageWithPort firstStage = this.stages[this.firstStageIndex];
		firstStage.executeWithPorts();

		this.updateRescheduable(firstStage);
		// this.setReschedulable(stage.isReschedulable());
	}

	private final void updateRescheduable(Stage stage) {
		while (!stage.isReschedulable()) {
			this.firstStageIndex++;
			stage = stage.next();
			if (stage == null) { // loop reaches the last stage
				this.setReschedulable(false);
				return;
			}
			stage.onIsPipelineHead();
			// System.out.println("firstStageIndex: " + this.firstStageIndex + ", class:" + stage.getClass().getSimpleName());
		}
		this.setReschedulable(true);
	}

	@Override
	public void onIsPipelineHead() {
		// do nothing
	}

	@Override
	public Object executeRecursively(final Object element) {
		return this.stages[0].executeRecursively(element);
	}

	public void onStart() {
		// Pipe pipe = new Pipe();
		// this.outputPort.pipe = pipe;
		// this.firstStage.getInputPort().pipe = pipe;

		// Pipe pipe = new Pipe();
		// this.firstStage.getOutputPort().pipe = pipe;
		// this.intermediateStages.get(0).getInputPort().pipe = pipe;
		//
		// for (int i = 0; i < this.intermediateStages.size() - 1; i++) {
		// Stage left = this.intermediateStages.get(i);
		// Stage right = this.intermediateStages.get(i + 1);
		//
		// pipe = new Pipe();
		// left.getOutputPort().pipe = pipe;
		// right.getInputPort().pipe = pipe;
		// }
		//
		// pipe = new Pipe();
		// this.intermediateStages.get(this.intermediateStages.size() - 1).getOutputPort().pipe = pipe;
		// this.lastStage.getInputPort().pipe = pipe;

		int size = 1 + this.intermediateStages.size() + 1;
		this.stages = new StageWithPort[size];
		this.stages[0] = this.firstStage;
		for (int i = 0; i < this.intermediateStages.size(); i++) {
			StageWithPort<?, ?> stage = this.intermediateStages.get(i);
			this.stages[1 + i] = stage;
		}
		this.stages[this.stages.length - 1] = this.lastStage;

		for (int i = 0; i < this.stages.length; i++) {
			StageWithPort<?, ?> stage = this.stages[i];
			stage.setParentStage(this, i);
			stage.setListener(this);
		}

		for (int i = 0; i < this.stages.length - 1; i++) {
			StageWithPort<?, ?> stage = this.stages[i];
			stage.setSuccessor(this.stages[i + 1]);
		}
		this.stages[this.stages.length - 1].setSuccessor(new EndStage<Object>());
	}

	//
	// @Override
	// public InputPort getInputPort() {
	// return this.firstStage.getInputPort();
	// }

	@Override
	public SchedulingInformation getSchedulingInformation() {
		return this.schedulingInformation;
	}

	@Override
	public Stage getParentStage() {
		return this.parentStage;
	}

	@Override
	public void setParentStage(final Stage parentStage, final int index) {
		this.index = index;
		this.parentStage = parentStage;
	}

	@Override
	public void onDisable(final Stage stage, final int index) {
		this.startIndex = index + 1;
		if (this.startIndex == this.stages.length) {
			this.disable();
		}
	}

	public void disable() {
		this.schedulingInformation.setActive(false);
		this.fireOnDisable();
	}

	private void fireOnDisable() {
		if (this.listener != null) {
			this.listener.onDisable(this, this.index);
		}
	}

	public OnDisableListener getListener() {
		return this.listener;
	}

	@Override
	public void setListener(final OnDisableListener listener) {
		this.listener = listener;
	}

	@Override
	public O execute(final Object element) {
		throw new IllegalStateException();
	}

	@Override
	public Stage next() {
		throw new IllegalStateException();
	}

	@Override
	public void setSuccessor(final Stage<?, ?> successor) {
		throw new IllegalStateException();
	}

	@Override
	public void setSuccessor(final StageWithPort<?, ?> successor) {
		throw new IllegalStateException();
	}

	@Override
	public boolean isReschedulable() {
		return this.reschedulable;
	}

	public void setReschedulable(final boolean reschedulable) {
		this.reschedulable = reschedulable;
	}

	@Override
	public InputPort<I> getInputPort() {
		return this.firstStage.getInputPort();
	}

	@Override
	public OutputPort<O> getOutputPort() {
		return this.lastStage.getOutputPort();
	}

	// @Override
	// public OutputPort getOutputPort() {
	// return this.lastStage.getOutputPort();
	// }

}
