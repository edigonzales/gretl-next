package ch.so.agi.gretl.test.job;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record TestJobRunRequest(
        TestJobDescriptor descriptor,
        TestJobBuildVariant buildVariant,
        TestJobExecutionTarget target,
        Path destinationRoot,
        Map<String, String> gradleProperties,
        Map<String, String> environment,
        Set<String> secrets,
        Optional<String> dockerNetwork,
        boolean traceEnabled) {
    public TestJobRunRequest {
        gradleProperties = Map.copyOf(gradleProperties == null ? Map.of() : gradleProperties);
        environment = Map.copyOf(environment == null ? Map.of() : environment);
        secrets = Set.copyOf(secrets == null ? Set.of() : secrets);
        dockerNetwork = dockerNetwork == null ? Optional.empty() : dockerNetwork;
    }
}
