/**
 * Copyright (C) 2015 Christian Wulf, Nelson Tavares de Sousa (http://teetime-framework.github.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kieker.analysis.examples.throughput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;

import teetime.util.StatisticsUtil;
import teetime.util.StopWatch;
import teetime.util.TimestampObject;
import util.test.PerformanceTest;

import kieker.analysis.examples.ThroughputTimestampAnalysis;
import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * @author Nils Christian Ehmke
 *
 * @since 1.10
 */
public class ThroughputTimestampAnalysisTest extends PerformanceTest {

	@Test
	public void testWithManyObjects() throws IllegalStateException, AnalysisConfigurationException {
		System.out.println("Testing kieker with NUM_OBJECTS_TO_CREATE=" + NUM_OBJECTS_TO_CREATE + ", NUM_NOOP_FILTERS="
				+ NUM_NOOP_FILTERS + "...");
		final StopWatch stopWatch = new StopWatch();
		final List<TimestampObject> timestampObjects = new ArrayList<TimestampObject>(NUM_OBJECTS_TO_CREATE);

		final ThroughputTimestampAnalysis analysis = new ThroughputTimestampAnalysis();
		analysis.setNumNoopFilters(NUM_NOOP_FILTERS);
		analysis.setTimestampObjects(timestampObjects);
		analysis.setInput(NUM_OBJECTS_TO_CREATE, new Callable<TimestampObject>() {
			@Override
			public TimestampObject call() throws Exception {
				return new TimestampObject();
			}
		});
		analysis.init();

		stopWatch.start();
		try {
			analysis.start();
		} finally {
			stopWatch.end();
		}

		StatisticsUtil.computeStatistics(stopWatch.getDurationInNs(), timestampObjects);
	}

}
