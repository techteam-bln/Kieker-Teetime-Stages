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
package teetime.stage.io.filesystem;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import teetime.framework.CompositeStage;
import teetime.framework.InputPort;
import teetime.framework.OutputPort;
import teetime.framework.Stage;
import teetime.framework.pipe.IPipeFactory;
import teetime.framework.pipe.PipeFactoryRegistry;
import teetime.framework.pipe.PipeFactoryRegistry.PipeOrdering;
import teetime.framework.pipe.PipeFactoryRegistry.ThreadCommunication;
import teetime.stage.FileExtensionSwitch;
import teetime.stage.basic.merger.Merger;
import teetime.stage.className.ClassNameRegistryCreationFilter;
import teetime.stage.className.ClassNameRegistryRepository;
import teetime.stage.io.Directory2FilesFilter;
import teetime.stage.io.filesystem.format.binary.file.BinaryFile2RecordFilter;
import teetime.stage.io.filesystem.format.text.file.DatFile2RecordFilter;

import kieker.common.record.IMonitoringRecord;
import kieker.common.util.filesystem.BinaryCompressionMethod;
import kieker.common.util.filesystem.FSUtil;

/**
 * @author Christian Wulf
 *
 * @since 1.0
 */
public final class Dir2RecordsFilter extends CompositeStage {

	private final PipeFactoryRegistry pipeFactoryRegistry = PipeFactoryRegistry.INSTANCE;
	private final ClassNameRegistryCreationFilter classNameRegistryCreationFilter;
	private final Merger<IMonitoringRecord> recordMerger;

	private ClassNameRegistryRepository classNameRegistryRepository;

	public Dir2RecordsFilter(final ClassNameRegistryRepository classNameRegistryRepository) {
		this.classNameRegistryRepository = classNameRegistryRepository;

		// FIXME does not yet work with more than one thread due to classNameRegistryRepository: classNameRegistryRepository is set after the ctor
		// create stages
		final ClassNameRegistryCreationFilter classNameRegistryCreationFilter = new ClassNameRegistryCreationFilter(this.classNameRegistryRepository);
		final Directory2FilesFilter directory2FilesFilter = new Directory2FilesFilter();

		final FileExtensionSwitch fileExtensionSwitch = new FileExtensionSwitch();

		final DatFile2RecordFilter datFile2RecordFilter = new DatFile2RecordFilter(this.classNameRegistryRepository);
		final BinaryFile2RecordFilter binaryFile2RecordFilter = new BinaryFile2RecordFilter(this.classNameRegistryRepository);

		final Merger<IMonitoringRecord> recordMerger = new Merger<IMonitoringRecord>();

		// store ports due to readability reasons
		final OutputPort<File> normalFileOutputPort = fileExtensionSwitch.addFileExtension(FSUtil.NORMAL_FILE_EXTENSION);
		final OutputPort<File> binFileOutputPort = fileExtensionSwitch.addFileExtension(BinaryCompressionMethod.NONE.getFileExtension());

		// connect ports by pipes
		IPipeFactory pipeFactory = pipeFactoryRegistry.getPipeFactory(ThreadCommunication.INTRA, PipeOrdering.ARBITRARY, false);
		pipeFactory.create(classNameRegistryCreationFilter.getOutputPort(), directory2FilesFilter.getInputPort());
		pipeFactory.create(directory2FilesFilter.getOutputPort(), fileExtensionSwitch.getInputPort());

		pipeFactory.create(normalFileOutputPort, datFile2RecordFilter.getInputPort());
		pipeFactory.create(binFileOutputPort, binaryFile2RecordFilter.getInputPort());

		pipeFactory.create(datFile2RecordFilter.getOutputPort(), recordMerger.getNewInputPort());
		pipeFactory.create(binaryFile2RecordFilter.getOutputPort(), recordMerger.getNewInputPort());

		// prepare pipeline
		this.classNameRegistryCreationFilter = classNameRegistryCreationFilter;
		this.recordMerger = recordMerger;
	}

	@Override
	protected Stage getFirstStage() {
		return classNameRegistryCreationFilter;
	}

	@Override
	protected Collection<? extends Stage> getLastStages() {
		return Arrays.asList((Stage) recordMerger);
	}

	public InputPort<File> getInputPort() {
		return classNameRegistryCreationFilter.getInputPort();
	}

	public OutputPort<IMonitoringRecord> getOutputPort() {
		return recordMerger.getOutputPort();
	}

	public ClassNameRegistryRepository getClassNameRegistryRepository() {
		return this.classNameRegistryRepository;
	}

	public void setClassNameRegistryRepository(final ClassNameRegistryRepository classNameRegistryRepository) {
		this.classNameRegistryRepository = classNameRegistryRepository;
	}

}
