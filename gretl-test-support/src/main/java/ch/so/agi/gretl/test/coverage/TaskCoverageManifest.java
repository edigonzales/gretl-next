package ch.so.agi.gretl.test.coverage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TaskCoverageManifest(int schemaVersion, Map<String, TaskCoverageEntry> tasks) {
    public TaskCoverageManifest {
        tasks = Map.copyOf(Objects.requireNonNull(tasks, "tasks must not be null"));
    }

    public Optional<TaskCoverageEntry> findByClassName(String className) {
        return tasks.values().stream().filter(entry -> entry.className().equals(className)).findFirst();
    }

    public List<TaskCoverageEntry> entries() {
        return tasks.values().stream().sorted(java.util.Comparator.comparing(TaskCoverageEntry::name)).toList();
    }
}
