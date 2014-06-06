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
package teetime.framework.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 */
public abstract class CompositeFilter implements IBaseStage {

	protected final List<IBaseStage> schedulableStages = new ArrayList<IBaseStage>();

	private final Map<IOutputPort<?, ?>, IInputPort<?, ?>> connections = new HashMap<IOutputPort<?, ?>, IInputPort<?, ?>>();

	public List<IBaseStage> getSchedulableStages() {
		return this.schedulableStages;
	}

	protected <T, S1 extends ISink<S1>, S0 extends ISource> void connectWithPipe(final IOutputPort<S0, T> sourcePort, final IInputPort<S1, T> targetPort) {
		this.connections.put(sourcePort, targetPort);
	}

	public Map<IOutputPort<?, ?>, IInputPort<?, ?>> getConnections() {
		return this.connections;
	}
}
