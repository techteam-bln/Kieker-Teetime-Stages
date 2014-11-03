package teetime.examples.kiekerdays;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import teetime.framework.HeadPipeline;
import teetime.framework.HeadStage;
import teetime.framework.RunnableStage;
import teetime.framework.pipe.SingleElementPipe;
import teetime.framework.pipe.SpScPipe;
import teetime.stage.InstanceOfFilter;
import teetime.stage.Relay;
import teetime.stage.basic.Sink;
import teetime.stage.basic.distributor.Distributor;
import teetime.stage.io.network.TcpReader;
import teetime.stage.trace.traceReconstruction.TraceReconstructionFilter;
import teetime.util.concurrent.hashmap.ConcurrentHashMapWithDefault;
import teetime.util.concurrent.hashmap.TraceBuffer;

import kieker.analysis.plugin.filter.flow.TraceEventRecords;
import kieker.common.record.IMonitoringRecord;
import kieker.common.record.flow.IFlowRecord;

public class TcpTraceReconstruction {

	private static final int NUM_VIRTUAL_CORES = Runtime.getRuntime().availableProcessors();
	private static final int MIO = 1000000;
	private static final int TCP_RELAY_MAX_SIZE = 2 * MIO;

	private final List<TraceEventRecords> elementCollection = new LinkedList<TraceEventRecords>();
	private final ConcurrentHashMapWithDefault<Long, TraceBuffer> traceId2trace = new ConcurrentHashMapWithDefault<Long, TraceBuffer>(new TraceBuffer());
	private final List<SpScPipe> tcpRelayPipes = new ArrayList<SpScPipe>();

	private Thread tcpThread;
	private Thread[] workerThreads;

	private int numWorkerThreads;

	public void init() {
		HeadPipeline<TcpReader, Distributor<IMonitoringRecord>> tcpPipeline = this.buildTcpPipeline();
		this.tcpThread = new Thread(new RunnableStage(tcpPipeline));

		this.numWorkerThreads = Math.min(NUM_VIRTUAL_CORES, this.numWorkerThreads);
		this.workerThreads = new Thread[this.numWorkerThreads];

		for (int i = 0; i < this.workerThreads.length; i++) {
			HeadStage pipeline = this.buildPipeline(tcpPipeline.getLastStage());
			this.workerThreads[i] = new Thread(new RunnableStage(pipeline));
		}
	}

	private HeadPipeline<TcpReader, Distributor<IMonitoringRecord>> buildTcpPipeline() {
		TcpReader tcpReader = new TcpReader();
		Distributor<IMonitoringRecord> distributor = new Distributor<IMonitoringRecord>();

		SingleElementPipe.connect(tcpReader.getOutputPort(), distributor.getInputPort());

		// create and configure pipeline
		HeadPipeline<TcpReader, Distributor<IMonitoringRecord>> pipeline = new HeadPipeline<TcpReader, Distributor<IMonitoringRecord>>();
		pipeline.setFirstStage(tcpReader);
		pipeline.setLastStage(distributor);
		return pipeline;
	}

	private HeadStage buildPipeline(final Distributor<IMonitoringRecord> tcpReaderPipeline) {
		// create stages
		Relay<IMonitoringRecord> relay = new Relay<IMonitoringRecord>();
		final InstanceOfFilter<IMonitoringRecord, IFlowRecord> instanceOfFilter = new InstanceOfFilter<IMonitoringRecord, IFlowRecord>(
				IFlowRecord.class);
		final TraceReconstructionFilter traceReconstructionFilter = new TraceReconstructionFilter(this.traceId2trace);
		Sink<TraceEventRecords> endStage = new Sink<TraceEventRecords>();

		// connect stages
		SpScPipe tcpRelayPipe = SpScPipe.connect(tcpReaderPipeline.getNewOutputPort(), relay.getInputPort(), TCP_RELAY_MAX_SIZE);
		this.tcpRelayPipes.add(tcpRelayPipe);

		SingleElementPipe.connect(relay.getOutputPort(), instanceOfFilter.getInputPort());
		SingleElementPipe.connect(instanceOfFilter.getOutputPort(), traceReconstructionFilter.getInputPort());
		SingleElementPipe.connect(traceReconstructionFilter.getTraceValidOutputPort(), endStage.getInputPort());

		// create and configure pipeline
		HeadPipeline<Relay<IMonitoringRecord>, Sink<TraceEventRecords>> pipeline = new HeadPipeline<Relay<IMonitoringRecord>, Sink<TraceEventRecords>>();
		pipeline.setFirstStage(relay);
		pipeline.setLastStage(endStage);
		return pipeline;
	}

	public void start() {

		this.tcpThread.start();

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
	}

	public void onTerminate() {
		int maxNumWaits = 0;
		for (SpScPipe pipe : this.tcpRelayPipes) {
			maxNumWaits = Math.max(maxNumWaits, pipe.getNumWaits());
		}
		System.out.println("max #waits of TcpRelayPipes: " + maxNumWaits);
	}

	public List<TraceEventRecords> getElementCollection() {
		return this.elementCollection;
	}

	public int getNumWorkerThreads() {
		return this.numWorkerThreads;
	}

	public void setNumWorkerThreads(final int numWorkerThreads) {
		this.numWorkerThreads = numWorkerThreads;
	}

	public static void main(final String[] args) {
		int numWorkerThreads = Integer.valueOf(args[0]);

		final TcpTraceReconstruction analysis = new TcpTraceReconstruction();
		analysis.setNumWorkerThreads(numWorkerThreads);

		analysis.init();
		try {
			analysis.start();
		} finally {
			analysis.onTerminate();
		}
	}

}
