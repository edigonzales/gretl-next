package ch.so.agi.gretl.control.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParameterDefinition(
        String name,
        ParameterType type,
        Object defaultValue,
        Boolean required) {

    public boolean isRequired() {
        return required != null && required;
    }
}
