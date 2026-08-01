package ch.so.agi.gretl.test.coverage;

import ch.so.agi.gretl.test.job.TestJobExecutionRequirement;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

import java.util.Map;
import java.util.Objects;

public record TaskCoverageScenario(
        String jobId,
        String taskPath,
        Map<TestJobExecutionTarget, TestJobExecutionRequirement> targets) {
    public TaskCoverageScenario {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(taskPath, "taskPath must not be null");
        targets = Map.copyOf(Objects.requireNonNull(targets, "targets must not be null"));
    }

    public TestJobExecutionRequirement requirementFor(TestJobExecutionTarget target) {
        return targets.getOrDefault(target, TestJobExecutionRequirement.NOT_APPLICABLE);
    }
}
