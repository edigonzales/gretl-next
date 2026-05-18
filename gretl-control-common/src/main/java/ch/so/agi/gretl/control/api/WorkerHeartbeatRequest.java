package ch.so.agi.gretl.control.api;

import java.util.List;

public record WorkerHeartbeatRequest(
        String workerId,
        List<String> labels,
        int capacity,
        int activeRuns) {
}
