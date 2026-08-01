package ch.so.agi.gretl.test.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.so.agi.gretl.test.execution.GretlTaskOutcome;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TaskExecutionTraceReader {
    private static final ObjectMapper JSON = new ObjectMapper();

    public TaskExecutionTrace read(Path traceFile) {
        List<TaskExecutionTraceEntry> entries = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(traceFile);
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                try {
                    JsonNode node = JSON.readTree(lines.get(i));
                    entries.add(new TaskExecutionTraceEntry(
                            required(node, "job", traceFile, i),
                            required(node, "build", traceFile, i),
                            TestJobExecutionTarget.valueOf(required(node, "backend", traceFile, i)),
                            required(node, "path", traceFile, i),
                            required(node, "className", traceFile, i),
                            GretlTaskOutcome.valueOf(required(node, "outcome", traceFile, i))));
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("Malformed task trace line " + (i + 1) + " in " + traceFile, e);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read task trace " + traceFile, e);
        }
        return new TaskExecutionTrace(entries);
    }

    private String required(JsonNode node, String field, Path file, int line) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing field '" + field + "' at " + file + ":" + (line + 1));
        }
        return value.asText();
    }
}
