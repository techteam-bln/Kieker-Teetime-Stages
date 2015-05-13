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
package teetime.stage.string.buffer.handler;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 */
public class StringHandler extends AbstractDataTypeHandler<String> {

	@Override
	public boolean canHandle(final Object object) {
		return object instanceof String;
	}

	@Override
	public String handle(final String object) {
		return this.stringRepository.get(object);
	}

}
