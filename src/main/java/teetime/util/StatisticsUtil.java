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
package teetime.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * @author Christian Wulf
 *
 * @since 1.10
 */
public final class StatisticsUtil {

	/**
	 * @since 1.10
	 */
	private StatisticsUtil() {
		// utility class
	}

	public static PerformanceResult computeStatistics(final long overallDurationInNs, final List<TimestampObject> timestampObjects) {
		final PerformanceResult performanceResult = new PerformanceResult();

		performanceResult.overallDurationInNs = overallDurationInNs;

		final List<Long> sortedDurationsInNs = new ArrayList<Long>(timestampObjects.size() / 2);
		long sumInNs = 0;
		for (int i = timestampObjects.size() / 2; i < timestampObjects.size(); i++) {
			final TimestampObject timestampObject = timestampObjects.get(i);
			final long durationInNs = timestampObject.getStopTimestamp() - timestampObject.getStartTimestamp();
			// sortedDurationsInNs.set(i - (timestampObjects.size() / 2), durationInNs);
			sortedDurationsInNs.add(durationInNs);
			sumInNs += durationInNs;
		}

		performanceResult.sumInNs = sumInNs;

		final Map<Double, Long> quintileValues = StatisticsUtil.calculateQuintiles(sortedDurationsInNs);
		performanceResult.quantiles = quintileValues;

		final long avgDurInNs = sumInNs / (timestampObjects.size() / 2);
		performanceResult.avgDurInNs = avgDurInNs;

		final long confidenceWidthInNs = StatisticsUtil.calculateConfidenceWidth(sortedDurationsInNs, avgDurInNs);
		performanceResult.confidenceWidthInNs = confidenceWidthInNs;

		return performanceResult;
	}

	public static String getQuantilesString(final Map<Double, Long> quantilesValues) {
		final StringBuilder builder = new StringBuilder();
		for (final Entry<Double, Long> entry : quantilesValues.entrySet()) {
			final String quantile = (entry.getKey() * 100) + " % : " + TimeUnit.NANOSECONDS.toNanos(entry.getValue()) + " ns";
			builder.append(quantile);
			builder.append("\n");
		}
		return builder.toString();
	}

	public static long calculateConfidenceWidth(final List<Long> durations, final long avgDurInNs) {
		final double z = 1.96; // for alpha = 0.05
		final double variance = MathUtil.getVariance(durations, avgDurInNs);
		final long confidenceWidthInNs = (long) MathUtil.getConfidenceWidth(z, variance, durations.size());
		return confidenceWidthInNs;
	}

	public static long calculateConfidenceWidth(final List<Long> durations) {
		return StatisticsUtil.calculateConfidenceWidth(durations, StatisticsUtil.calculateAverage(durations));
	}

	public static long calculateAverage(final List<Long> durations) {
		long sumNs = 0;
		for (final Long value : durations) {
			sumNs += value;
		}

		return sumNs / durations.size();
	}

	public static Map<Double, Long> calculateQuintiles(final List<Long> durationsInNs) {
		Collections.sort(durationsInNs);

		final Map<Double, Long> quintileValues = new LinkedHashMap<Double, Long>();
		final double[] quintiles = { 0.00, 0.25, 0.50, 0.75, 1.00 };
		for (final double quintile : quintiles) {
			final int index = (int) ((durationsInNs.size() - 1) * quintile);
			quintileValues.put(quintile, durationsInNs.get(index));
		}
		return quintileValues;
	}

	public static void removeLeadingZeroThroughputs(final List<Long> throughputs) {
		final Iterator<Long> iterator = throughputs.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == 0) {
				iterator.remove();
			} else {
				break;
			}
		}
	}

}
