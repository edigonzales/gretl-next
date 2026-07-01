package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MigrationMetadata(
        @JsonProperty("from") List<String> from,
        @JsonProperty("to") String to,
        @JsonProperty("codeActionTitle") String codeActionTitle) {

    public MigrationMetadata {
        from = from != null ? List.copyOf(from) : List.of();
    }
}
