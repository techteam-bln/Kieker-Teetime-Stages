package teetime.util.concurrent.workstealing.alternative;

import org.hamcrest.number.OrderingComparison;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import teetime.util.StopWatch;

public class CircularWorkStealingDequeWithSentinelTest {
	private StopWatch stopWatch;

	@Before
	public void before() {
		this.stopWatch = new StopWatch();
	}

	@Test
	public void measureManyEmptyPulls() {
		final CircularWorkStealingDequeWithSentinel<Object> deque = new CircularWorkStealingDequeWithSentinel<Object>();

		final int numIterations = UntypedCircularWorkStealingDequeTest.NUM_ITERATIONS;
		this.stopWatch.start();
		for (int i = 0; i < numIterations; i++) {
			deque.popBottom();
			// if (returnValue.getState() != State.EMPTY) {
			// returnValue.getValue();
			// }
		}
		this.stopWatch.end();

		Assert.assertThat(this.stopWatch.getDurationInNs(), OrderingComparison.lessThan(UntypedCircularWorkStealingDequeTest.EXPECTED_DURATION_IN_NS));
	}
}
