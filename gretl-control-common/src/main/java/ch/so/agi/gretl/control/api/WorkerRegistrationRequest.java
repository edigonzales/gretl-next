package ch.so.agi.gretl.control.api;

import java.util.List;

public record WorkerRegistrationRequest(
        String workerId,
        String displayName,
        List<String> labels,
        int capacity) {
}
