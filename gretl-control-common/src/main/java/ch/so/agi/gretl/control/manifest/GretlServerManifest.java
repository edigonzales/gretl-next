package ch.so.agi.gretl.control.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GretlServerManifest(List<JobDefinition> jobs) {
    public GretlServerManifest {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
