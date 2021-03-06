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
package teetime.examples.recordReader;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import kieker.common.record.IMonitoringRecord;

import teetime.framework.AbstractStage;
import teetime.framework.Configuration;
import teetime.stage.CollectorSink;
import teetime.stage.InitialElementProducer;
import teetime.stage.className.ClassNameRegistryRepository;
import teetime.stage.io.filesystem.Dir2RecordsFilter;

/**
 * @author Christian Wulf
 *
 * @since 1.0
 */
public class RecordReaderConfiguration extends Configuration {

	private final List<IMonitoringRecord> elementCollection = new LinkedList<IMonitoringRecord>();

	public RecordReaderConfiguration() {
		this.buildConfiguration();
	}

	private AbstractStage buildConfiguration() {
		ClassNameRegistryRepository classNameRegistryRepository = new ClassNameRegistryRepository();
		File logDir = new File("src/test/data/bookstore-logs");
		// create stages
		InitialElementProducer<File> initialElementProducer = new InitialElementProducer<File>(logDir);
		Dir2RecordsFilter dir2RecordsFilter = new Dir2RecordsFilter(classNameRegistryRepository);
		CollectorSink<IMonitoringRecord> collector = new CollectorSink<IMonitoringRecord>(this.elementCollection);

		// connect stages
		connectPorts(initialElementProducer.getOutputPort(), dir2RecordsFilter.getInputPort());
		connectPorts(dir2RecordsFilter.getOutputPort(), collector.getInputPort());

		return initialElementProducer;
	}

	public List<IMonitoringRecord> getElementCollection() {
		return this.elementCollection;
	}

}
