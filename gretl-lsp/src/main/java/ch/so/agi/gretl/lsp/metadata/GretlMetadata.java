package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GretlMetadata(
        @JsonProperty("schemaVersion") String schemaVersion,
        @JsonProperty("generatedAt") String generatedAt,
        @JsonProperty("gretlVersion") String gretlVersion,
        @JsonProperty("source") MetadataSource source,
        @JsonProperty("tasks") List<TaskMetadata> tasks) {

    public GretlMetadata {
        tasks = tasks != null ? List.copyOf(tasks) : List.of();
    }

    public static GretlMetadata empty() {
        return new GretlMetadata("1.0.0", null, "unknown", null, List.of());
    }

    public Optional<TaskMetadata> findTask(String simpleName) {
        return tasks.stream()
                .filter(t -> t.name().equals(simpleName))
                .findFirst();
    }

    public List<TaskMetadata> tasksSortedByName() {
        return tasks.stream()
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }
}
