package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionMetadata(
        @JsonProperty("label") String label,
        @JsonProperty("detail") String detail,
        @JsonProperty("sortText") String sortText) {
}
