package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;

public interface TestJobExecutionBackend extends AutoCloseable {
    TestJobExecutionTarget target();
    GretlBuildResult execute(TestJobExecutionRequest request);
    GretlBuildResult executeAndExpectFailure(TestJobExecutionRequest request);
    @Override default void close() { }
}
