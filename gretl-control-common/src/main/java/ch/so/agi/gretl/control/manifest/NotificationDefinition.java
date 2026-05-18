package ch.so.agi.gretl.control.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationDefinition(
        List<String> on,
        String email,
        String webhook) {

    public NotificationDefinition {
        on = on == null ? List.of() : List.copyOf(on);
    }
}
