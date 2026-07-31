package ch.so.agi.gretl.test.execution;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record GretlBuildResult(
        int exitCode,
        String standardOutput,
        String standardError,
        Duration duration,
        List<String> sanitizedCommand,
        Map<String, GretlTaskOutcome> taskOutcomes) {

    public GretlBuildResult {
        standardOutput = standardOutput == null ? "" : standardOutput;
        standardError = standardError == null ? "" : standardError;
        sanitizedCommand = List.copyOf(sanitizedCommand);
        taskOutcomes = Map.copyOf(taskOutcomes);
    }

    public boolean successful() {
        return exitCode == 0;
    }

    public String output() {
        return standardOutput + standardError;
    }

    public Optional<GretlTaskOutcome> taskOutcome(String taskPath) {
        return Optional.ofNullable(taskOutcomes.get(taskPath));
    }
}
