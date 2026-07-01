package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PropertyMetadata(
        @JsonProperty("name") String name,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("kind") String kind,
        @JsonProperty("valueType") String valueType,
        @JsonProperty("javaType") String javaType,
        @JsonProperty("required") boolean required,
        @JsonProperty("deprecated") boolean deprecated,
        @JsonProperty("description") String description,
        @JsonProperty("file") FileMetadata file,
        @JsonProperty("acceptedForms") List<AcceptedForm> acceptedForms,
        @JsonProperty("migration") MigrationMetadata migration,
        @JsonProperty("sqlParameterProvider") boolean sqlParameterProvider,
        @JsonProperty("completion") CompletionMetadata completion) {

    public PropertyMetadata {
        acceptedForms = acceptedForms != null ? List.copyOf(acceptedForms) : List.of();
    }
}
