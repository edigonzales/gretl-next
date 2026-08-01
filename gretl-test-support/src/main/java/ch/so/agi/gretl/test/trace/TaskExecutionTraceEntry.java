package ch.so.agi.gretl.test.trace;

import ch.so.agi.gretl.test.execution.GretlTaskOutcome;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

import java.util.Objects;

public record TaskExecutionTraceEntry(
        String jobId,
        String buildVariant,
        TestJobExecutionTarget backend,
        String taskPath,
        String taskClassName,
        GretlTaskOutcome outcome) {
    public TaskExecutionTraceEntry {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(buildVariant);
        Objects.requireNonNull(backend);
        Objects.requireNonNull(taskPath);
        Objects.requireNonNull(taskClassName);
        Objects.requireNonNull(outcome);
    }
}
