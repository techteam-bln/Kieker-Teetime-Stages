package teetime.util.concurrent.workstealing.alternative;

import org.hamcrest.number.OrderingComparison;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import teetime.util.StopWatch;
import teetime.util.concurrent.workstealing.exception.DequeIsEmptyException;

public class UntypedExceptionalCircularWorkStealingDequeTest {

	private StopWatch stopWatch;

	@Before
	public void before() {
		this.stopWatch = new StopWatch();
	}

	@Test
	public void measureManyEmptyPulls() {
		final UntypedExceptionalCircularWorkStealingDeque deque = new UntypedExceptionalCircularWorkStealingDeque();
		System.out.println(UntypedExceptionalCircularWorkStealingDeque.DEQUE_IS_EMPTY_EXCEPTION);
		System.out.println(UntypedExceptionalCircularWorkStealingDeque.OPERATION_ABORTED_EXCEPTION);

		int counter = 0;
		final int numIterations = UntypedCircularWorkStealingDequeTest.NUM_ITERATIONS;
		this.stopWatch.start();
		for (int i = 0; i < numIterations; i++) {
			try {
				deque.popBottom();
			} catch (final DequeIsEmptyException e) {
				// do not handle; we just want to compare the performance of throwing a preallocated exception vs. returning special values
				counter++;
			}
		}
		this.stopWatch.end();

		Assert.assertThat(this.stopWatch.getDurationInNs(), OrderingComparison.lessThan(UntypedCircularWorkStealingDequeTest.EXPECTED_DURATION_IN_NS));
		Assert.assertThat(counter, OrderingComparison.comparesEqualTo(UntypedCircularWorkStealingDequeTest.NUM_ITERATIONS));
	}
}
