package ch.so.agi.gretl.control.api;

import java.util.List;

public record RunClaimRequest(
        String workerId,
        List<String> labels,
        int availableSlots) {
}
