package ch.so.agi.gretl.test.execution;

public enum GretlTaskOutcome {
    SUCCESS,
    FAILED,
    SKIPPED,
    UP_TO_DATE,
    FROM_CACHE,
    NO_SOURCE,
    UNKNOWN
}
