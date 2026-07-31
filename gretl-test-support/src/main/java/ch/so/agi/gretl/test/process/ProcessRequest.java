package ch.so.agi.gretl.test.process;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ProcessRequest(
        List<String> command,
        Path workingDirectory,
        Map<String, String> environment,
        Duration timeout,
        Set<String> secretValues) {

    public ProcessRequest {
        command = List.copyOf(Objects.requireNonNull(command, "command must not be null"));
        if (command.isEmpty() || command.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("command must contain at least one non-null argument");
        }
        workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory must not be null")
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(workingDirectory)) {
            throw new IllegalArgumentException("workingDirectory is not a directory: " + workingDirectory);
        }
        environment = Map.copyOf(Objects.requireNonNull(environment, "environment must not be null"));
        timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        secretValues = Set.copyOf(Objects.requireNonNull(secretValues, "secretValues must not be null"));
    }
}
