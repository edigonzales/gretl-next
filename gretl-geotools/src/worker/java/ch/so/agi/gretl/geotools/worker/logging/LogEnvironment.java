package ch.so.agi.gretl.geotools.worker.logging;

public final class LogEnvironment {

    private LogEnvironment() {
    }

    public static GretlLogger getLogger(Class<?> logSource) {
        return new StdoutLogger(logSource);
    }
}
