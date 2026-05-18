package ch.so.agi.gretl.control.api;

public enum RunStatus {
    QUEUED,
    CLAIMED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TIMED_OUT,
    SKIPPED
}
