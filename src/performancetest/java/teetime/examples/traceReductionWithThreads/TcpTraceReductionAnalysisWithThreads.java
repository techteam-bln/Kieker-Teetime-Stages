package teetime.examples.traceReductionWithThreads;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import teetime.framework.OldHeadPipeline;
import teetime.framework.RunnableStage;
import teetime.framework.Stage;
import teetime.framework.pipe.SingleElementPipe;
import teetime.framework.pipe.SpScPipe;
import teetime.stage.Clock;
import teetime.stage.Counter;
import teetime.stage.ElementDelayMeasuringStage;
import teetime.stage.ElementThroughputMeasuringStage;
import teetime.stage.InstanceCounter;
import teetime.stage.InstanceOfFilter;
import teetime.stage.Relay;
import teetime.stage.basic.Sink;
import teetime.stage.basic.distributor.Distributor;
import teetime.stage.io.network.TcpReader;
import teetime.stage.trace.traceReconstruction.TraceReconstructionFilter;
import teetime.stage.trace.traceReduction.TraceAggregationBuffer;
import teetime.stage.trace.traceReduction.TraceComperator;
import teetime.stage.trace.traceReduction.TraceReductionFilter;
import teetime.util.concurrent.hashmap.ConcurrentHashMapWithDefault;
import teetime.util.concurrent.hashmap.TraceBuffer;

import kieker.analysis.plugin.filter.flow.TraceEventRecords;
import kieker.common.record.IMonitoringRecord;
import kieker.common.record.flow.IFlowRecord;
import kieker.common.record.flow.trace.TraceMetadata;

public class TcpTraceReductionAnalysisWithThreads {

	private static final int NUM_VIRTUAL_CORES = Runtime.getRuntime().availableProcessors();
	private static final int MIO = 1000000;
	private static final int TCP_RELAY_MAX_SIZE = (int) (0.5 * MIO);

	private final List<TraceEventRecords> elementCollection = new LinkedList<TraceEventRecords>();

	private Thread tcpThread;
	private Thread clockThread;
	private Thread clock2Thread;
	private Thread[] workerThreads;

	private SpScPipe tcpRelayPipe;
	private int numWorkerThreads;

	public void init() {
		OldHeadPipeline<TcpReader, Distributor<IMonitoringRecord>> tcpPipeline = this.buildTcpPipeline();
		this.tcpThread = new Thread(new RunnableStage(tcpPipeline));

		OldHeadPipeline<Clock, Distributor<Long>> clockStage = this.buildClockPipeline(1000);
		this.clockThread = new Thread(new RunnableStage(clockStage));

		OldHeadPipeline<Clock, Distributor<Long>> clock2Stage = this.buildClockPipeline(5000);
		this.clock2Thread = new Thread(new RunnableStage(clock2Stage));

		this.numWorkerThreads = Math.min(NUM_VIRTUAL_CORES, this.numWorkerThreads);
		this.workerThreads = new Thread[this.numWorkerThreads];

		for (int i = 0; i < this.workerThreads.length; i++) {
			OldHeadPipeline<Relay<IMonitoringRecord>, Sink<TraceEventRecords>> pipeline = this.buildPipeline(tcpPipeline, clockStage, clock2Stage);
			this.workerThreads[i] = new Thread(new RunnableStage(pipeline));
		}
	}

	private OldHeadPipeline<TcpReader, Distributor<IMonitoringRecord>> buildTcpPipeline() {
		TcpReader tcpReader = new TcpReader();
		Distributor<IMonitoringRecord> distributor = new Distributor<IMonitoringRecord>();

		SingleElementPipe.connect(tcpReader.getOutputPort(), distributor.getInputPort());

		// create and configure pipeline
		OldHeadPipeline<TcpReader, Distributor<IMonitoringRecord>> pipeline = new OldHeadPipeline<TcpReader, Distributor<IMonitoringRecord>>();
		pipeline.setFirstStage(tcpReader);
		pipeline.setLastStage(distributor);
		return pipeline;
	}

	private OldHeadPipeline<Clock, Distributor<Long>> buildClockPipeline(final long intervalDelayInMs) {
		Clock clock = new Clock();
		clock.setInitialDelayInMs(intervalDelayInMs);
		clock.setIntervalDelayInMs(intervalDelayInMs);
		Distributor<Long> distributor = new Distributor<Long>();

		SingleElementPipe.connect(clock.getOutputPort(), distributor.getInputPort());

		// create and configure pipeline
		OldHeadPipeline<Clock, Distributor<Long>> pipeline = new OldHeadPipeline<Clock, Distributor<Long>>();
		pipeline.setFirstStage(clock);
		pipeline.setLastStage(distributor);
		return pipeline;
	}

	private static class StageFactory<T extends Stage> {

		private final Constructor<T> constructor;
		private final List<T> stages = new ArrayList<T>();

		public StageFactory(final Constructor<T> constructor) {
			this.constructor = constructor;
		}

		public T create(final Object... initargs) {
			try {
				T stage = this.constructor.newInstance(initargs);
				this.stages.add(stage);
				return stage;
			} catch (InstantiationException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}

		public List<T> getStages() {
			return this.stages;
		}
	}

	private final ConcurrentHashMapWithDefault<Long, TraceBuffer> traceId2trace = new ConcurrentHashMapWithDefault<Long, TraceBuffer>(new TraceBuffer());
	private final Map<TraceEventRecords, TraceAggregationBuffer> trace2buffer = new TreeMap<TraceEventRecords, TraceAggregationBuffer>(new TraceComperator());

	private final StageFactory<Counter<IMonitoringRecord>> recordCounterFactory;
	private final StageFactory<ElementDelayMeasuringStage<IMonitoringRecord>> recordThroughputFilterFactory;
	private final StageFactory<InstanceCounter<IMonitoringRecord, TraceMetadata>> traceMetadataCounterFactory;
	private final StageFactory<Counter<TraceEventRecords>> traceCounterFactory;
	private final StageFactory<ElementThroughputMeasuringStage<TraceEventRecords>> traceThroughputFilterFactory;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TcpTraceReductionAnalysisWithThreads() {
		try {
			this.recordCounterFactory = new StageFactory(Counter.class.getConstructor());
			this.recordThroughputFilterFactory = new StageFactory(ElementDelayMeasuringStage.class.getConstructor());
			this.traceMetadataCounterFactory = new StageFactory(InstanceCounter.class.getConstructor(Class.class));
			this.traceCounterFactory = new StageFactory(Counter.class.getConstructor());
			this.traceThroughputFilterFactory = new StageFactory(ElementThroughputMeasuringStage.class.getConstructor());
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private OldHeadPipeline<Relay<IMonitoringRecord>, Sink<TraceEventRecords>> buildPipeline(
			final OldHeadPipeline<TcpReader, Distributor<IMonitoringRecord>> tcpReaderPipeline,
			final OldHeadPipeline<Clock, Distributor<Long>> clockStage,
			final OldHeadPipeline<Clock, Distributor<Long>> clock2Stage) {
		// create stages
		Relay<IMonitoringRecord> relay = new Relay<IMonitoringRecord>();
		Counter<IMonitoringRecord> recordCounter = this.recordCounterFactory.create();
		InstanceCounter<IMonitoringRecord, TraceMetadata> traceMetadataCounter = this.traceMetadataCounterFactory.create(TraceMetadata.class);
		final InstanceOfFilter<IMonitoringRecord, IFlowRecord> instanceOfFilter = new InstanceOfFilter<IMonitoringRecord, IFlowRecord>(
				IFlowRecord.class);
		// ElementDelayMeasuringStage<IMonitoringRecord> recordThroughputFilter = this.recordThroughputFilterFactory.create();
		final TraceReconstructionFilter traceReconstructionFilter = new TraceReconstructionFilter(this.traceId2trace);
		TraceReductionFilter traceReductionFilter = new TraceReductionFilter(this.trace2buffer);
		Counter<TraceEventRecords> traceCounter = this.traceCounterFactory.create();
		ElementThroughputMeasuringStage<TraceEventRecords> traceThroughputFilter = this.traceThroughputFilterFactory.create();
		Sink<TraceEventRecords> endStage = new Sink<TraceEventRecords>();

		// connect stages
		this.tcpRelayPipe = SpScPipe.connect(tcpReaderPipeline.getLastStage().getNewOutputPort(), relay.getInputPort(), TCP_RELAY_MAX_SIZE);

		SingleElementPipe.connect(relay.getOutputPort(), recordCounter.getInputPort());
		SingleElementPipe.connect(recordCounter.getOutputPort(), traceMetadataCounter.getInputPort());
		SingleElementPipe.connect(traceMetadataCounter.getOutputPort(), instanceOfFilter.getInputPort());
		SingleElementPipe.connect(instanceOfFilter.getOutputPort(), traceReconstructionFilter.getInputPort());
		SingleElementPipe.connect(traceReconstructionFilter.getTraceValidOutputPort(), traceReductionFilter.getInputPort());
		SingleElementPipe.connect(traceReductionFilter.getOutputPort(), traceCounter.getInputPort());
		SingleElementPipe.connect(traceCounter.getOutputPort(), traceThroughputFilter.getInputPort());
		SingleElementPipe.connect(traceThroughputFilter.getOutputPort(), endStage.getInputPort());

		// SingleElementPipe.connect(traceReconstructionFilter.getOutputPort(), traceThroughputFilter.getInputPort());
		// SingleElementPipe.connect(traceThroughputFilter.getOutputPort(), endStage.getInputPort());

		SpScPipe.connect(clock2Stage.getLastStage().getNewOutputPort(), traceReductionFilter.getTriggerInputPort(), 10);
		SpScPipe.connect(clockStage.getLastStage().getNewOutputPort(), traceThroughputFilter.getTriggerInputPort(), 10);

		// create and configure pipeline
		OldHeadPipeline<Relay<IMonitoringRecord>, Sink<TraceEventRecords>> pipeline = new OldHeadPipeline<Relay<IMonitoringRecord>, Sink<TraceEventRecords>>();
		pipeline.setFirstStage(relay);
		pipeline.setLastStage(endStage);
		return pipeline;
	}

	public void start() {

		this.tcpThread.start();
		this.clockThread.start();
		// this.clock2Thread.start();

		for (Thread workerThread : this.workerThreads) {
			workerThread.start();
		}

		try {
			this.tcpThread.join();

			for (Thread workerThread : this.workerThreads) {
				workerThread.join();
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		this.clockThread.interrupt();
		this.clock2Thread.interrupt();
	}

	public List<TraceEventRecords> getElementCollection() {
		return this.elementCollection;
	}

	public int getNumRecords() {
		int sum = 0;
		for (Counter<IMonitoringRecord> stage : this.recordCounterFactory.getStages()) {
			sum += stage.getNumElementsPassed();
		}
		return sum;
	}

	public int getNumTraces() {
		int sum = 0;
		for (Counter<TraceEventRecords> stage : this.traceCounterFactory.getStages()) {
			sum += stage.getNumElementsPassed();
		}
		return sum;
	}

	public List<Long> getRecordDelays() {
		List<Long> throughputs = new LinkedList<Long>();
		for (ElementDelayMeasuringStage<IMonitoringRecord> stage : this.recordThroughputFilterFactory.getStages()) {
			throughputs.addAll(stage.getDelays());
		}
		return throughputs;
	}

	public List<Long> getTraceThroughputs() {
		List<Long> throughputs = new LinkedList<Long>();
		for (ElementThroughputMeasuringStage<TraceEventRecords> stage : this.traceThroughputFilterFactory.getStages()) {
			throughputs.addAll(stage.getThroughputs());
		}
		return throughputs;
	}

	public SpScPipe getTcpRelayPipe() {
		return this.tcpRelayPipe;
	}

	public int getNumWorkerThreads() {
		return this.numWorkerThreads;
	}

	public void setNumWorkerThreads(final int numWorkerThreads) {
		this.numWorkerThreads = numWorkerThreads;
	}

	public List<Integer> getNumTraceMetadatas() {
		List<Integer> numTraceMetadatas = new LinkedList<Integer>();
		for (InstanceCounter<IMonitoringRecord, TraceMetadata> stage : this.traceMetadataCounterFactory.getStages()) {
			numTraceMetadatas.add(stage.getCounter());
		}
		return numTraceMetadatas;
	}

}
