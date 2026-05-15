package ch.so.agi.gretl.geotools.internal.logging;

public final class WorkerLogMessage {

    private final WorkerLogLevel level;
    private final String message;

    public WorkerLogMessage(WorkerLogLevel level, String message) {
        this.level = level;
        this.message = message;
    }

    public WorkerLogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
