package ch.so.agi.gretl.lsp.server;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ServerLogger {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final PrintStream ERR = System.err;

    private final String level;

    public ServerLogger(String level) {
        this.level = level != null ? level : "INFO";
    }

    public void debug(String message) {
        if (shouldLog("DEBUG")) {
            log("DEBUG", message);
        }
    }

    public void info(String message) {
        if (shouldLog("INFO")) {
            log("INFO", message);
        }
    }

    public void warn(String message) {
        if (shouldLog("WARN")) {
            log("WARN", message);
        }
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void error(String message, Throwable t) {
        log("ERROR", message);
        t.printStackTrace(ERR);
    }

    private boolean shouldLog(String messageLevel) {
        return ordinal(messageLevel) >= ordinal(level);
    }

    private static int ordinal(String lvl) {
        switch (lvl) {
            case "DEBUG":
                return 0;
            case "INFO":
                return 1;
            case "WARN":
                return 2;
            case "ERROR":
                return 3;
            default:
                return 1;
        }
    }

    private void log(String lvl, String message) {
        ERR.printf("[%s] [%s] [gretl-lsp] %s%n", LocalTime.now().format(TIME_FMT), lvl, message);
    }
}
