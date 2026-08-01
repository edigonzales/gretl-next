package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;

public interface TestJobAssertions {
    String id();
    void verify(MaterializedTestJob job, GretlBuildResult result, TaskExecutionTrace trace) throws Exception;
}
