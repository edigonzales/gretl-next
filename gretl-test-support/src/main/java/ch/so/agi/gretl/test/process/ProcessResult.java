package ch.so.agi.gretl.test.process;

import java.time.Duration;
import java.util.List;

public record ProcessResult(
        int exitCode,
        String standardOutput,
        String standardError,
        Duration duration,
        List<String> sanitizedCommand) {

    public ProcessResult {
        standardOutput = standardOutput == null ? "" : standardOutput;
        standardError = standardError == null ? "" : standardError;
        sanitizedCommand = List.copyOf(sanitizedCommand);
    }

    public boolean successful() {
        return exitCode == 0;
    }

    public String output() {
        return standardOutput + standardError;
    }
}
