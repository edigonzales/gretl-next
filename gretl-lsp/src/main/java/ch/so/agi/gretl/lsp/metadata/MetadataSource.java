package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetadataSource(
        @JsonProperty("repository") String repository,
        @JsonProperty("doclet") String doclet,
        @JsonProperty("commit") String commit) {
}
