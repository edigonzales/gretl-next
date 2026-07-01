package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AcceptedForm(
        @JsonProperty("style") String style,
        @JsonProperty("signature") String signature,
        @JsonProperty("insertText") String insertText,
        @JsonProperty("argumentCount") Integer argumentCount,
        @JsonProperty("legacy") boolean legacy) {
}
