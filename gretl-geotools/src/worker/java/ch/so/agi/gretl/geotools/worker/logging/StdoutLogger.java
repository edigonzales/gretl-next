package ch.so.agi.gretl.geotools.worker.logging;

final class StdoutLogger implements GretlLogger {

    private final String name;

    StdoutLogger(Class<?> logSource) {
        this.name = logSource == null ? "GeoToolsWorker" : logSource.getSimpleName();
    }

    @Override
    public void info(String msg) {
        System.out.println(name + " INFO: " + msg);
    }

    @Override
    public void debug(String msg) {
        System.out.println(name + " DEBUG: " + msg);
    }

    @Override
    public void error(String msg, Throwable thrown) {
        System.err.println(name + " ERROR: " + msg);
        if (thrown != null) {
            thrown.printStackTrace(System.err);
        }
    }

    @Override
    public void lifecycle(String msg) {
        System.out.println(msg);
    }
}
