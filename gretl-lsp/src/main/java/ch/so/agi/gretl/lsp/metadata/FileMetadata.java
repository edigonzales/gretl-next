package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileMetadata(
        @JsonProperty("role") String role,
        @JsonProperty("extensions") List<String> extensions,
        @JsonProperty("multiple") boolean multiple,
        @JsonProperty("mustExist") boolean mustExist) {

    public FileMetadata {
        extensions = extensions != null ? List.copyOf(extensions) : List.of();
    }
}
