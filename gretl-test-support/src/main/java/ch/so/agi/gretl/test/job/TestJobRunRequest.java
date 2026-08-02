package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.fixture.TestJobEnvironment;

import java.nio.file.Path;
import java.util.Objects;

public record TestJobRunRequest(
        TestJobDescriptor descriptor,
        TestJobBuildVariant buildVariant,
        TestJobExecutionTarget target,
        TestJobExecutionId executionId,
        Path destinationRoot,
        TestJobEnvironment callerOverrides,
        boolean traceEnabled,
        MaterializedJobRetentionPolicy retentionPolicy) {
    public TestJobRunRequest {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(buildVariant, "buildVariant must not be null");
        Objects.requireNonNull(target, "target must not be null");
        executionId = executionId == null ? TestJobExecutionId.create(descriptor, buildVariant, target) : executionId;
        Objects.requireNonNull(destinationRoot, "destinationRoot must not be null");
        callerOverrides = callerOverrides == null ? TestJobEnvironment.empty() : callerOverrides;
        retentionPolicy = retentionPolicy == null ? MaterializedJobRetentionPolicy.DELETE_ON_SUCCESS : retentionPolicy;
    }

    public TestJobRunRequest(TestJobDescriptor descriptor, TestJobBuildVariant buildVariant,
                             TestJobExecutionTarget target, Path destinationRoot,
                             java.util.Map<String, String> gradleProperties,
                             java.util.Map<String, String> environment,
                             java.util.Set<String> secrets,
                             java.util.Optional<String> dockerNetwork,
                             boolean traceEnabled) {
        this(descriptor, buildVariant, target, null, destinationRoot,
                new TestJobEnvironment(gradleProperties == null ? java.util.Map.of() : gradleProperties,
                        environment == null ? java.util.Map.of() : environment,
                        secrets == null ? java.util.Set.of() : secrets,
                        dockerNetwork == null ? java.util.Optional.empty() : dockerNetwork),
                traceEnabled, MaterializedJobRetentionPolicy.DELETE_ON_SUCCESS);
    }
}
