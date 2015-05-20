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
package teetime.util;

import java.util.concurrent.ConcurrentHashMap;

public final class ConcurrentHashMapWithDefault<K, V> extends ConcurrentHashMap<K, V> {

	private static final long serialVersionUID = 199185976241037967L;

	private final ValueFactory<V> valueFactory;

	private int maxElements;

	public ConcurrentHashMapWithDefault(final ValueFactory<V> valueFactory) {
		this.valueFactory = valueFactory;
	}

	public V getOrCreate(final K key) {
		V value = this.get(key);
		if (value == null) {
			synchronized (this) {
				value = this.get(key);
				if (value == null) { // NOCS (DCL)
					value = this.valueFactory.create();
					this.put(key, value);
					this.maxElements++;
				}
			}
		}
		return value;
	}

	public int getMaxElements() {
		return this.maxElements;
	}
}
