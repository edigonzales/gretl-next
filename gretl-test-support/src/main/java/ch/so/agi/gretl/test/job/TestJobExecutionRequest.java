package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.process.SecretMasker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record TestJobExecutionRequest(
        MaterializedTestJob job,
        List<String> arguments,
        Map<String, String> environment,
        Map<String, String> gradleProperties,
        Set<String> secretValues,
        Duration timeout,
        Optional<String> dockerNetwork) {
    public TestJobExecutionRequest {
        arguments = List.copyOf(arguments == null ? List.of() : arguments);
        environment = Map.copyOf(environment == null ? Map.of() : environment);
        gradleProperties = Map.copyOf(gradleProperties == null ? Map.of() : gradleProperties);
        secretValues = Set.copyOf(secretValues == null ? Set.of() : secretValues);
        timeout = timeout == null ? job.descriptor().timeout() : timeout;
        dockerNetwork = dockerNetwork == null ? Optional.empty() : dockerNetwork;
    }

    public List<String> effectiveArguments() {
        List<String> result = new ArrayList<>();
        gradleProperties.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.add("-P" + entry.getKey() + "=" + entry.getValue()));
        result.addAll(arguments);
        boolean hasTask = false;
        boolean optionValue = false;
        for (String argument : result) {
            if (optionValue) {
                optionValue = false;
                continue;
            }
            if (argument.equals("--init-script") || argument.equals("--project-dir")
                    || argument.equals("--include-build")) {
                optionValue = true;
                continue;
            }
            if (!argument.startsWith("-")) {
                hasTask = true;
                break;
            }
        }
        if (!hasTask) result.addAll(job.descriptor().entryTasks());
        return List.copyOf(result);
    }

    public List<String> sanitizedArguments() {
        return new SecretMasker().maskArguments(effectiveArguments(), secretValues);
    }
}
