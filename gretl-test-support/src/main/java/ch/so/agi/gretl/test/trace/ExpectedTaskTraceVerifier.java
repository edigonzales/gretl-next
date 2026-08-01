package ch.so.agi.gretl.test.trace;

import ch.so.agi.gretl.test.execution.GretlTaskOutcome;
import ch.so.agi.gretl.test.job.ExpectedTaskExecution;
import ch.so.agi.gretl.test.job.TestJobDescriptor;

import java.util.Set;

public final class ExpectedTaskTraceVerifier {
    private static final Set<GretlTaskOutcome> ACCEPTED = Set.of(
            GretlTaskOutcome.SUCCESS, GretlTaskOutcome.UP_TO_DATE, GretlTaskOutcome.FROM_CACHE);

    public void verify(TestJobDescriptor descriptor, TaskExecutionTrace trace) {
        for (ExpectedTaskExecution expected : descriptor.expectedTasks()) {
            TaskExecutionTraceEntry actual = trace.find(expected.path()).orElseThrow(
                    () -> new AssertionError("Expected task missing from trace: " + descriptor.id() + " " + expected.path()));
            if (!actual.taskClassName().equals(expected.className())) {
                throw new AssertionError("Wrong task class for " + expected.path() + ": expected "
                        + expected.className() + ", got " + actual.taskClassName());
            }
            if (!ACCEPTED.contains(actual.outcome())) {
                throw new AssertionError("Expected task has non-positive outcome " + actual.outcome() + ": " + expected.path());
            }
            if (actual.outcome() == GretlTaskOutcome.UNKNOWN) {
                throw new AssertionError("Expected task has UNKNOWN outcome: " + expected.path());
            }
        }
    }
}
