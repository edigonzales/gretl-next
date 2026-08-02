package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;

public interface TestJobAssertions {
    String id();

    default void verify(TestJobVerificationContext context) throws Exception {
        verify(context.job(), context.result(), context.trace());
    }

    default void verify(MaterializedTestJob job, GretlBuildResult result, TaskExecutionTrace trace) throws Exception {
        throw new UnsupportedOperationException("Assertion must implement one verify overload");
    }
}
