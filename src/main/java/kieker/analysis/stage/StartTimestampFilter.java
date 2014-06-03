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
package kieker.analysis.stage;

import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import teetime.examples.throughput.TimestampObject;

@Plugin(outputPorts = @OutputPort(name = StartTimestampFilter.OUTPUT_PORT_NAME))
public class StartTimestampFilter extends AbstractFilterPlugin {

	public static final String INPUT_PORT_NAME = "input";
	public static final String OUTPUT_PORT_NAME = "output";

	public StartTimestampFilter(final Configuration configuration, final IProjectContext projectContext) {
		super(configuration, projectContext);
	}

	@InputPort(name = StartTimestampFilter.INPUT_PORT_NAME)
	public void execute(final TimestampObject inputObject) {
		inputObject.setStartTimestamp(System.nanoTime());
		super.deliver(StartTimestampFilter.OUTPUT_PORT_NAME, inputObject);
	}

	@Override
	public Configuration getCurrentConfiguration() {
		return new Configuration();
	}

}
