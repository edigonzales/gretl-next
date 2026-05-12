package ch.so.agi.gretl.geotools.worker.logging;

public interface GretlLogger {

    void info(String msg);

    void debug(String msg);

    void error(String msg, Throwable thrown);

    void lifecycle(String msg);
}
