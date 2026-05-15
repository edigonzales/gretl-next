package ch.so.agi.gretl.geotools.worker.logging;

public final class WorkerLogFormat {

    public static final String PREFIX = "GRETL_WORKER";
    private static final String SEPARATOR = "|";

    private WorkerLogFormat() {
    }

    public static String format(WorkerLogLevel level, String message) {
        WorkerLogLevel safeLevel = level == null ? WorkerLogLevel.LIFECYCLE : level;
        return PREFIX + SEPARATOR + safeLevel.name() + SEPARATOR + (message == null ? "" : message);
    }
}
