package ch.so.agi.gretl.test.job;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public record TestJobSettingsRequest(
        String projectName,
        TestJobExecutionTarget target,
        Optional<URI> publishedRepository,
        Optional<String> pluginVersion) {
    public TestJobSettingsRequest {
        Objects.requireNonNull(projectName, "projectName must not be null");
        Objects.requireNonNull(target, "target must not be null");
        publishedRepository = publishedRepository == null ? Optional.empty() : publishedRepository;
        pluginVersion = pluginVersion == null ? Optional.empty() : pluginVersion;
    }
}
