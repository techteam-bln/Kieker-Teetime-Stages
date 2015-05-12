/**
 * Copyright (C) 2015 Christian Wulf, Nelson Tavares de Sousa (http://teetime.sourceforge.net)
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
package util.test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MooBenchStarter {

	private final File execDir;

	public MooBenchStarter() {
		this.execDir = new File("scripts/MooBench-cmd");
		System.out.println("execDir: " + this.execDir.getAbsolutePath());
	}

	public void start(final int runs, final long calls) throws IOException {
		final List<String> command = new LinkedList<String>();
		command.add("cmd");
		command.add("/c");
		command.add("start");
		command.add("/D");
		command.add(this.execDir.getAbsolutePath());
		command.add("Load Driver");
		command.add("startMooBench.cmd");
		command.add(String.valueOf(runs));
		command.add(String.valueOf(calls));

		new ProcessBuilder(command).start();
	}
}
