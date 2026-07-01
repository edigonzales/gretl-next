package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskMetadata(
        @JsonProperty("name") String name,
        @JsonProperty("qualifiedClassName") String qualifiedClassName,
        @JsonProperty("simpleClassName") String simpleClassName,
        @JsonProperty("category") String category,
        @JsonProperty("status") String status,
        @JsonProperty("description") String description,
        @JsonProperty("longDescription") String longDescription,
        @JsonProperty("examples") List<Example> examples,
        @JsonProperty("properties") List<PropertyMetadata> properties) {

    public TaskMetadata {
        examples = examples != null ? List.copyOf(examples) : List.of();
        properties = properties != null ? List.copyOf(properties) : List.of();
    }

    public Optional<PropertyMetadata> findProperty(String name) {
        return properties.stream()
                .filter(p -> p.name().equals(name))
                .findFirst();
    }

    public List<PropertyMetadata> requiredProperties() {
        return properties.stream()
                .filter(PropertyMetadata::required)
                .toList();
    }

    public List<PropertyMetadata> completionProperties() {
        return properties.stream()
                .filter(p -> !p.deprecated())
                .toList();
    }
}
