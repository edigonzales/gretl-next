package ch.so.agi.gretl.control.api;

public record RunLogAppendRequest(
        String workerId,
        String stream,
        String line) {
}
