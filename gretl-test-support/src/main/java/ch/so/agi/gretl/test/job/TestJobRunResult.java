package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;

public record TestJobRunResult(MaterializedTestJob job, GretlBuildResult buildResult, TaskExecutionTrace trace) { }
