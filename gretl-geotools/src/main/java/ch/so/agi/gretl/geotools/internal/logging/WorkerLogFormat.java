package ch.so.agi.gretl.geotools.internal.logging;

public final class WorkerLogFormat {

    public static final String PREFIX = "GRETL_WORKER";
    private static final String SEPARATOR = "|";

    private WorkerLogFormat() {
    }

    public static String format(WorkerLogLevel level, String message) {
        WorkerLogLevel safeLevel = level == null ? WorkerLogLevel.LIFECYCLE : level;
        return PREFIX + SEPARATOR + safeLevel.name() + SEPARATOR + (message == null ? "" : message);
    }

    public static WorkerLogMessage parse(String line) {
        if (line == null) {
            return null;
        }
        String prefix = PREFIX + SEPARATOR;
        if (!line.startsWith(prefix)) {
            return null;
        }
        int levelStart = prefix.length();
        int levelEnd = line.indexOf(SEPARATOR, levelStart);
        if (levelEnd < 0) {
            return null;
        }
        WorkerLogLevel level = WorkerLogLevel.fromToken(line.substring(levelStart, levelEnd));
        if (level == null) {
            return null;
        }
        return new WorkerLogMessage(level, line.substring(levelEnd + 1));
    }
}
