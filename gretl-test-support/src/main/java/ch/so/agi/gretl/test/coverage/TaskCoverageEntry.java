package ch.so.agi.gretl.test.coverage;

import java.util.List;
import java.util.Objects;

public record TaskCoverageEntry(
        String name,
        String className,
        String module,
        TaskCoverageClassification classification,
        String reason,
        List<TaskCoverageScenario> scenarios) {
    public TaskCoverageEntry {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(module, "module must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        scenarios = List.copyOf(Objects.requireNonNull(scenarios, "scenarios must not be null"));
    }
}
