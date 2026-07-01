package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Example(
        @JsonProperty("title") String title,
        @JsonProperty("language") String language,
        @JsonProperty("body") String body) {
}
