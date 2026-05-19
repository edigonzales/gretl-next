package ch.so.agi.gretl.control.server.manifest;

import java.time.Instant;
import java.util.List;

public record ManifestReloadResponse(
        boolean success,
        String path,
        Instant loadedAt,
        Instant attemptedAt,
        List<String> addedJobs,
        List<String> removedJobs,
        List<String> updatedJobs,
        List<String> unchangedJobs,
        List<String> skippedQueuedRuns,
        String error) {
}
