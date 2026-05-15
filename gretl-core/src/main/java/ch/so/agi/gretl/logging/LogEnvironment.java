package ch.so.agi.gretl.logging;

/**
 * Holds the logging factory used by steps and helper classes.
 *
 * <p>Gradle integration is activated explicitly by the plugin. Code that runs
 * without the plugin, for example in standalone tests or future command-line
 * wrappers, falls back to a console-oriented java.util.logging implementation.</p>
 */
public class LogEnvironment {

    private static LogFactory currentLogFactory = null;

    public static void setLogFactory(LogFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        currentLogFactory = factory;
    }

    public static void initGradleIntegrated() {
        setLogFactory(new GradleLogFactory());
    }

    public static void initStandalone() {
        initStandalone(Level.DEBUG);
    }

    public static void initStandalone(Level logLevel) {
        if (currentLogFactory == null) {
            setLogFactory(new CoreJavaLogFactory(logLevel));
        }
    }

    public static GretlLogger getLogger(Class logSource) {
        if (currentLogFactory == null) {
            setLogFactory(new CoreJavaLogFactory(Level.DEBUG));
        }
        if (logSource == null)
            throw new IllegalArgumentException("The logSource must not be null");

        return currentLogFactory.getLogger(logSource);
    }
}
