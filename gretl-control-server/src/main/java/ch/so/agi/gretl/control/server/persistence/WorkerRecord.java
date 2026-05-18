package ch.so.agi.gretl.control.server.persistence;

import ch.so.agi.gretl.control.api.WorkerStatus;

import java.time.Instant;
import java.util.List;

public record WorkerRecord(
        String id,
        String displayName,
        List<String> labels,
        int capacity,
        int activeRuns,
        Instant lastHeartbeat,
        WorkerStatus status) {
}
