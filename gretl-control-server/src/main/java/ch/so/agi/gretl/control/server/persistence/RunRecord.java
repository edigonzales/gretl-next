package ch.so.agi.gretl.control.server.persistence;

import ch.so.agi.gretl.control.api.RunStatus;
import ch.so.agi.gretl.control.api.RunTriggerType;

import java.time.Instant;
import java.util.Map;

public record RunRecord(
        String id,
        String jobId,
        RunStatus status,
        RunTriggerType triggerType,
        String triggeredBy,
        String workerId,
        Instant queuedAt,
        Instant claimedAt,
        Instant startedAt,
        Instant finishedAt,
        Integer exitCode,
        boolean cancelRequested,
        Map<String, Object> parameters,
        String message,
        String logPath) {
}
