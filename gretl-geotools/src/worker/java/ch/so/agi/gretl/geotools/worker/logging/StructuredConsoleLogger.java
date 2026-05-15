package ch.so.agi.gretl.geotools.worker.logging;

final class StructuredConsoleLogger implements GretlLogger {

    private final String source;

    StructuredConsoleLogger(Class<?> logSource) {
        this.source = logSource == null ? "GeoToolsWorker" : logSource.getSimpleName();
    }

    @Override
    public void info(String msg) {
        LogEnvironment.emit(WorkerLogLevel.INFO, withSource(msg));
    }

    @Override
    public void debug(String msg) {
        LogEnvironment.emit(WorkerLogLevel.DEBUG, withSource(msg));
    }

    @Override
    public void error(String msg, Throwable thrown) {
        LogEnvironment.emit(WorkerLogLevel.ERROR, withSource(msg));
        if (thrown != null) {
            LogEnvironment.emit(WorkerLogLevel.ERROR, thrown.toString());
        }
    }

    @Override
    public void lifecycle(String msg) {
        LogEnvironment.emit(WorkerLogLevel.LIFECYCLE, withSource(msg));
    }

    private String withSource(String msg) {
        return source + ": " + (msg == null ? "" : msg);
    }
}
