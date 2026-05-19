package ch.so.agi.gretl.control.server.manifest;

import java.time.Instant;
import java.util.List;

public record ManifestStatusResponse(
        String path,
        Instant loadedAt,
        Instant lastReloadAttemptAt,
        String lastReloadError,
        boolean watchEnabled,
        List<String> jobIds) {
}
