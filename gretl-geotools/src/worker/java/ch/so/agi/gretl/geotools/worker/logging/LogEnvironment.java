package ch.so.agi.gretl.geotools.worker.logging;

import java.util.function.BiConsumer;

public final class LogEnvironment {

    private static final ThreadLocal<BiConsumer<String, String>> LOG_SINK = new ThreadLocal<>();

    private LogEnvironment() {
    }

    public static void setLogSink(BiConsumer<String, String> logSink) {
        LOG_SINK.set(logSink);
    }

    public static void clearLogSink() {
        LOG_SINK.remove();
    }

    public static GretlLogger getLogger(Class<?> logSource) {
        return new StructuredConsoleLogger(logSource);
    }

    static void emit(WorkerLogLevel level, String message) {
        BiConsumer<String, String> sink = LOG_SINK.get();
        WorkerLogLevel safeLevel = level == null ? WorkerLogLevel.LIFECYCLE : level;
        if (sink != null) {
            sink.accept(safeLevel.name(), message == null ? "" : message);
            return;
        }

        String line = WorkerLogFormat.format(safeLevel, message);
        if (safeLevel == WorkerLogLevel.ERROR) {
            System.err.println(line);
        } else {
            System.out.println(line);
        }
    }
}
