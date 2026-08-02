package ch.so.agi.gretl.test.job;

import java.util.Objects;

public record TestJobExecutionIdentity(
        String jobId,
        String buildVariant,
        TestJobExecutionTarget target,
        TestJobExecutionId executionId) {
    public TestJobExecutionIdentity {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(buildVariant, "buildVariant must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(executionId, "executionId must not be null");
    }

    public String namespace() {
        return (jobId + "-" + buildVariant + "-" + target.name() + "-" + executionId.shortToken())
                .replaceAll("[^A-Za-z0-9_.-]", "-").toLowerCase();
    }

    public String shortToken() {
        return executionId.shortToken();
    }
}
