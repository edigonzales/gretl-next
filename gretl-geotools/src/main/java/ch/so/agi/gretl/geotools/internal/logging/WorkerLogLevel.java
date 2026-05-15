package ch.so.agi.gretl.geotools.internal.logging;

public enum WorkerLogLevel {
    LIFECYCLE,
    INFO,
    DEBUG,
    ERROR;

    public static WorkerLogLevel fromToken(String token) {
        if (token == null) {
            return null;
        }
        for (WorkerLogLevel level : values()) {
            if (level.name().equalsIgnoreCase(token)) {
                return level;
            }
        }
        return null;
    }
}
