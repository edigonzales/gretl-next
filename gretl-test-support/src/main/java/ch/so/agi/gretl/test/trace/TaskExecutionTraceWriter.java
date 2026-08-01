package ch.so.agi.gretl.test.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TaskExecutionTraceWriter {
    private static final ObjectMapper JSON = new ObjectMapper();

    public void write(Path traceFile, TaskExecutionTrace trace) {
        try {
            Path file = traceFile.toAbsolutePath().normalize();
            Files.createDirectories(file.getParent());
            StringBuilder output = new StringBuilder();
            for (TaskExecutionTraceEntry entry : trace.entries()) {
                ObjectNode node = JSON.createObjectNode();
                node.put("job", entry.jobId());
                node.put("build", entry.buildVariant());
                node.put("backend", entry.backend().name());
                node.put("path", entry.taskPath());
                node.put("className", entry.taskClassName());
                node.put("outcome", entry.outcome().name());
                output.append(JSON.writeValueAsString(node)).append(System.lineSeparator());
            }
            Files.writeString(file, output.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write task execution trace " + traceFile, e);
        }
    }
}
