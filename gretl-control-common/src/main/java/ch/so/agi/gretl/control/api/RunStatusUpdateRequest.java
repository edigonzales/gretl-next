package ch.so.agi.gretl.control.api;

public record RunStatusUpdateRequest(
        String workerId,
        RunStatus status,
        Integer exitCode,
        String message) {
}
