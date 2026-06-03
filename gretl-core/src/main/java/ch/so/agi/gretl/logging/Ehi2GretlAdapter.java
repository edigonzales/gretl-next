package ch.so.agi.gretl.logging;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.LogEvent;
import ch.ehi.basics.logging.LogListener;
import ch.ehi.basics.logging.StdListener;
import ch.interlis.iox.IoxLogEvent;

public final class Ehi2GretlAdapter implements LogListener {

    private static Ehi2GretlAdapter instance;

    private final GretlLogger logger;

    private Ehi2GretlAdapter() {
        logger = LogEnvironment.getLogger(EhiLogger.class);
    }

    public static synchronized void init() {
        if (instance != null) {
            return;
        }
        instance = new Ehi2GretlAdapter();
        EhiLogger.getInstance().addListener(instance);
        EhiLogger.getInstance().removeListener(StdListener.getInstance());
    }

    @Override
    public void logEvent(LogEvent event) {
        String prefix = formatPrefix(event);
        String message = formatMessage(event);

        switch (event.getEventKind()) {
            case LogEvent.DEBUG_TRACE, LogEvent.STATE_TRACE -> logger.debug(prefix + message);
            case LogEvent.UNUSUAL_STATE_TRACE, LogEvent.BACKEND_CMD, LogEvent.STATE, LogEvent.ADAPTION ->
                    logger.info(prefix + message);
            case LogEvent.ERROR -> logger.error(prefix + message, null);
            default -> logger.info(prefix + message);
        }
    }

    private static String formatPrefix(LogEvent event) {
        if (!(event instanceof IoxLogEvent ioxEvent)) {
            return "";
        }

        StringBuilder prefix = new StringBuilder();
        if (ioxEvent.getSourceLineNr() != null) {
            prefix.append("line ").append(ioxEvent.getSourceLineNr()).append(": ");
        }
        if (ioxEvent.getSourceObjectTag() != null) {
            prefix.append(ioxEvent.getSourceObjectTag()).append(": ");
        }
        if (ioxEvent.getSourceObjectTechId() != null) {
            prefix.append(ioxEvent.getSourceObjectTechId()).append(": ");
        }
        if (ioxEvent.getSourceObjectXtfId() != null) {
            prefix.append("tid ").append(ioxEvent.getSourceObjectXtfId()).append(": ");
        }
        if (ioxEvent.getSourceObjectUsrId() != null) {
            prefix.append(ioxEvent.getSourceObjectUsrId()).append(": ");
        }
        return prefix.toString();
    }

    private static String formatMessage(LogEvent event) {
        String message = trimToNull(event.getEventMsg());
        if (message != null) {
            return message;
        }

        Throwable exception = event.getException();
        if (exception == null) {
            return "";
        }

        message = trimToNull(exception.getLocalizedMessage());
        return message != null ? message : exception.getClass().getName();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
