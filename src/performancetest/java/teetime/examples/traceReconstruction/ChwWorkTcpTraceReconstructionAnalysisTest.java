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
package teetime.examples.traceReconstruction;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import teetime.framework.Analysis;
import teetime.util.StopWatch;
import util.test.eval.StatisticsUtil;

/**
 * @author Christian Wulf
 *
 * @since 1.10
 */
public class ChwWorkTcpTraceReconstructionAnalysisTest {

	private static final int MIO = 1000000;
	private static final int EXPECTED_NUM_TRACES = 10 * MIO;
	private static final int EXPECTED_NUM_RECORDS = 21 * EXPECTED_NUM_TRACES + 1;

	private StopWatch stopWatch;

	@Before
	public void before() {
		this.stopWatch = new StopWatch();
	}

	@After
	public void after() {
		long overallDurationInNs = this.stopWatch.getDurationInNs();
		System.out.println("Duration: " + TimeUnit.NANOSECONDS.toMillis(overallDurationInNs) + " ms");
	}

	@Test
	public void performAnalysis() {
		final TcpTraceReconstructionConf configuration = new TcpTraceReconstructionConf();

		Analysis analysis = new Analysis(configuration);
		analysis.init();

		this.stopWatch.start();
		try {
			analysis.start();
		} finally {
			this.stopWatch.end();
		}

		Map<Double, Long> quintiles = StatisticsUtil.calculateQuintiles(configuration.getTraceThroughputs());
		System.out.println("Median throughput: " + quintiles.get(0.5) + " elements/time unit");

		assertEquals(EXPECTED_NUM_RECORDS, configuration.getNumRecords());
		assertEquals(EXPECTED_NUM_TRACES, configuration.getNumTraces());

		// TraceEventRecords trace6884 = analysis.getElementCollection().get(0);
		// assertEquals(6884, trace6884.getTraceMetadata().getTraceId());
		//
		// TraceEventRecords trace6886 = analysis.getElementCollection().get(1);
		// assertEquals(6886, trace6886.getTraceMetadata().getTraceId());

		// until 04.07.2014 (inkl.)
		// Median throughput: 74 elements/time unit
		// Duration: 17445 ms
		// Median throughput: 78 elements/time unit
		// Duration: 16608 ms
	}

}
