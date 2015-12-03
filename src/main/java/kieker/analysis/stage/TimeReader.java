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
package kieker.analysis.stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.Property;
import kieker.analysis.plugin.reader.AbstractReaderPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.record.misc.TimestampRecord;

@Plugin(
		description = "Delivers the current (system) time in regular intervals",
		outputPorts = {
			@OutputPort(name = TimeReader.OUTPUT_PORT_NAME_TIMESTAMPS, eventTypes = Long.class),
			@OutputPort(name = TimeReader.OUTPUT_PORT_NAME_TIMESTAMP_RECORDS, eventTypes = TimestampRecord.class)
		},
		configuration = {
			@Property(name = TimeReader.CONFIG_PROPERTY_NAME_UPDATE_INTERVAL_NS, defaultValue = TimeReader.CONFIG_PROPERTY_VALUE_UPDATE_INTERVAL_NS,
					description = "Determines the update interval in nano seconds."),
			@Property(name = TimeReader.CONFIG_PROPERTY_NAME_DELAY_NS, defaultValue = TimeReader.CONFIG_PROPERTY_VALUE_DELAY_NS,
					description = "Determines the initial delay in nano seconds."),
			@Property(name = TimeReader.CONFIG_PROPERTY_NAME_NUMBER_IMPULSES, defaultValue = TimeReader.CONFIG_PROPERTY_VALUE_NUMBER_IMPULSES,
					description = "Determines the number of impulses to emit (0 = infinite).")
		})
public final class TimeReader extends AbstractReaderPlugin {

	/** The name of the output port for the timestamps. */
	public static final String OUTPUT_PORT_NAME_TIMESTAMPS = "timestamps";
	/** The name of the output port for the timestamp records. */
	public static final String OUTPUT_PORT_NAME_TIMESTAMP_RECORDS = "timestampRecords";

	/** The name of the property determining the update interval in nanoseconds. */
	public static final String CONFIG_PROPERTY_NAME_UPDATE_INTERVAL_NS = "updateIntervalNS";
	/** The default value for the update interval (1 second). */
	public static final String CONFIG_PROPERTY_VALUE_UPDATE_INTERVAL_NS = "1000000000";

	/** The name of the property determining the initial delay in nanoseconds. */
	public static final String CONFIG_PROPERTY_NAME_DELAY_NS = "delayNS";
	/** The default value for the initial delay (0 seconds). */
	public static final String CONFIG_PROPERTY_VALUE_DELAY_NS = "0";

	/** The name of the property determining the number of impulses to emit. */
	public static final String CONFIG_PROPERTY_NAME_NUMBER_IMPULSES = "numberImpulses";
	/** The default value for number of impulses (infinite). */
	public static final String CONFIG_PROPERTY_VALUE_NUMBER_IMPULSES = "0";

	/** A value for the number of impulses. It makes sure that the reader emits an infinite amount of signals. */
	public static final long INFINITE_EMITS = 0L;

	final CountDownLatch impulseEmitLatch = new CountDownLatch(1); // NOCS NOPMD (package visible)

	private volatile boolean terminated;

	private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
	private volatile ScheduledFuture<?> result;

	private final long initialDelay;
	private final long period;
	private final long numberImpulses;

	/**
	 * Creates a new timer using the given configuration.
	 * 
	 * @param configuration
	 *            The configuration containing the properties to initialize this timer.
	 * @param projectContext
	 *            The project context.
	 */
	public TimeReader(final Configuration configuration, final IProjectContext projectContext) {
		super(configuration, projectContext);
		this.initialDelay = configuration.getLongProperty(CONFIG_PROPERTY_NAME_DELAY_NS);
		this.period = configuration.getLongProperty(CONFIG_PROPERTY_NAME_UPDATE_INTERVAL_NS);
		this.numberImpulses = configuration.getLongProperty(CONFIG_PROPERTY_NAME_NUMBER_IMPULSES);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void terminate(final boolean error) {
		if (!this.terminated) {
			this.log.info("Shutdown of TimeReader requested.");
			this.executorService.shutdown();
			try {
				this.terminated = this.executorService.awaitTermination(5, TimeUnit.SECONDS);
			} catch (final InterruptedException ex) {
				// ignore
			}
			if (!this.terminated && (this.result != null)) {
				// problems shutting down
				this.result.cancel(true);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean read() {
		this.result = this.executorService.scheduleAtFixedRate(new TimestampEventTask(this.numberImpulses), this.initialDelay, this.period, TimeUnit.NANOSECONDS);

		return true;
	}

	@Override
	public Configuration getCurrentConfiguration() {
		final Configuration configuration = new Configuration();
		configuration.setProperty(CONFIG_PROPERTY_NAME_DELAY_NS, Long.toString(this.initialDelay));
		configuration.setProperty(CONFIG_PROPERTY_NAME_UPDATE_INTERVAL_NS, Long.toString(this.period));
		configuration.setProperty(CONFIG_PROPERTY_NAME_NUMBER_IMPULSES, Long.toString(this.numberImpulses));
		return configuration;
	}

	/**
	 * Sends the current system time as a new timestamp event.
	 */
	protected void sendTimestampEvent() {
		final long timestamp = super.recordsTimeUnitFromProjectContext.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
		super.deliver(OUTPUT_PORT_NAME_TIMESTAMPS, timestamp);
		super.deliver(OUTPUT_PORT_NAME_TIMESTAMP_RECORDS, new TimestampRecord(timestamp));
	}

	/**
	 * A simple helper class used to send the current system time.
	 * 
	 * @author Nils Christian Ehmke
	 * 
	 * @since 1.8
	 */
	protected class TimestampEventTask implements Runnable {
		private final boolean infinite;
		private volatile long numberImpulses;

		/**
		 * Creates a new task.
		 * 
		 * @param numberImpulses
		 *            0 = infinite
		 */
		public TimestampEventTask(final long numberImpulses) {
			this.numberImpulses = numberImpulses;
			if (numberImpulses == 0) {
				this.infinite = true;
			} else {
				this.infinite = false;
			}
		}

		/**
		 * Executes the task.
		 */
		@Override
		public void run() {
			if (this.infinite || (this.numberImpulses > 0)) {
				TimeReader.this.sendTimestampEvent();
				if (!this.infinite && (0 == --this.numberImpulses)) { // NOPMD
					TimeReader.this.impulseEmitLatch.countDown();
				}
			}
		}
	}
}
