package ch.so.agi.gretl.test.trace;

import java.util.List;
import java.util.Optional;

public record TaskExecutionTrace(List<TaskExecutionTraceEntry> entries) {
    public TaskExecutionTrace {
        entries = List.copyOf(entries);
    }

    public Optional<TaskExecutionTraceEntry> find(String taskPath) {
        return entries.stream().filter(entry -> entry.taskPath().equals(taskPath)).findFirst();
    }

    public boolean contains(String taskPath, String className) {
        return entries.stream().anyMatch(entry -> entry.taskPath().equals(taskPath)
                && entry.taskClassName().equals(className));
    }
}
