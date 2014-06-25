package teetime.variant.methodcallWithPorts.stage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import teetime.variant.methodcallWithPorts.framework.core.ConsumerStage;
import teetime.variant.methodcallWithPorts.framework.core.OutputPort;

import com.google.common.io.Files;

public class FileExtensionSwitch extends ConsumerStage<File, File> {

	private final Map<String, OutputPort<File>> fileExtensions = new HashMap<String, OutputPort<File>>();

	@Override
	protected void execute5(final File file) {
		String fileExtension = Files.getFileExtension(file.getAbsolutePath());
		this.logger.debug("fileExtension: " + fileExtension);
		OutputPort<File> outputPort = this.fileExtensions.get(fileExtension);
		if (outputPort != null) {
			this.send(outputPort, file);
		}
	}

	public OutputPort<File> addFileExtension(String fileExtension) {
		if (fileExtension.startsWith(".")) {
			fileExtension = fileExtension.substring(1);
		}
		OutputPort<File> outputPort = new OutputPort<File>();
		this.fileExtensions.put(fileExtension, outputPort);
		this.logger.debug("SUCCESS: Registered output port for '" + fileExtension + "'");
		return outputPort;
	}

}
