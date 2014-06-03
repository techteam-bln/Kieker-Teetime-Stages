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
package teetime.stage.predicate;

import java.util.Set;

import teetime.common.record.controlflow.OperationExecutionRecord;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 */
public class IsOperationExecutionRecordTraceIdPredicate extends IsTraceIdPredicate<OperationExecutionRecord> {

	/**
	 * @since 1.10
	 */
	public IsOperationExecutionRecordTraceIdPredicate(final boolean acceptAllTraces, final Set<Long> selectedTraceIds) {
		super(acceptAllTraces, selectedTraceIds);
	}

	public boolean apply(final OperationExecutionRecord record) {
		final Long traceId = record.getTraceId();
		return this.acceptId(traceId);
	}

}
