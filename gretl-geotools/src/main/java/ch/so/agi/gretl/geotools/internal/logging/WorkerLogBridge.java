package ch.so.agi.gretl.geotools.internal.logging;

import org.gradle.api.logging.Logger;

public final class WorkerLogBridge {

    private WorkerLogBridge() {
    }

    public static void log(Logger logger, String levelToken, String message) {
        WorkerLogLevel level = WorkerLogLevel.fromToken(levelToken);
        log(logger, level == null ? WorkerLogLevel.LIFECYCLE : level, message);
    }

    public static void logLine(Logger logger, String line, boolean errorStream) {
        if (line == null || line.isBlank()) {
            return;
        }
        WorkerLogMessage parsed = WorkerLogFormat.parse(line);
        if (parsed != null) {
            log(logger, parsed.getLevel(), parsed.getMessage());
            return;
        }
        if (errorStream) {
            logger.error(line);
        } else {
            logger.lifecycle(line);
        }
    }

    static void log(Logger logger, WorkerLogLevel level, String message) {
        String text = message == null ? "" : message;
        switch (level) {
            case DEBUG:
                logger.debug(text);
                break;
            case INFO:
                logger.info(text);
                break;
            case ERROR:
                logger.error(text);
                break;
            case LIFECYCLE:
            default:
                logger.lifecycle(text);
                break;
        }
    }
}
