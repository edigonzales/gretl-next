package ch.so.agi.gretl.control.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JvmDefinition(String maxHeap, List<String> args) {
    public JvmDefinition {
        args = args == null ? List.of() : List.copyOf(args);
    }
}
