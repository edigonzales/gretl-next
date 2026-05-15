package ch.so.agi.gretl.geotools.internal.logging;

import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkerLogBridgeTest {

    @Test
    void formatsAndParsesWorkerLines() {
        for (WorkerLogLevel level : WorkerLogLevel.values()) {
            String line = WorkerLogFormat.format(level, "message for " + level.name());

            WorkerLogMessage parsed = WorkerLogFormat.parse(line);

            assertEquals(level, parsed.getLevel());
            assertEquals("message for " + level.name(), parsed.getMessage());
        }
    }

    @Test
    void rejectsNonWorkerLines() {
        assertNull(WorkerLogFormat.parse("plain console output"));
        assertNull(WorkerLogFormat.parse("GRETL_WORKER|TRACE|message"));
    }

    @Test
    void mapsWorkerLevelsToGradleLoggerMethods() {
        RecordingLogger logger = new RecordingLogger();

        WorkerLogBridge.log(logger.proxy(), "LIFECYCLE", "life");
        WorkerLogBridge.log(logger.proxy(), "INFO", "info");
        WorkerLogBridge.log(logger.proxy(), "DEBUG", "debug");
        WorkerLogBridge.log(logger.proxy(), "ERROR", "error");

        assertEquals(List.of(
                "lifecycle:life",
                "info:info",
                "debug:debug",
                "error:error"
        ), logger.calls);
    }

    @Test
    void mapsFallbackLinesForFutureProcessIsolation() {
        RecordingLogger logger = new RecordingLogger();

        WorkerLogBridge.logLine(logger.proxy(), "GRETL_WORKER|INFO|bridged", false);
        WorkerLogBridge.logLine(logger.proxy(), "stderr fallback", true);
        WorkerLogBridge.logLine(logger.proxy(), "stdout fallback", false);

        assertEquals(List.of(
                "info:bridged",
                "error:stderr fallback",
                "lifecycle:stdout fallback"
        ), logger.calls);
    }

    private static final class RecordingLogger implements InvocationHandler {
        private final List<String> calls = new ArrayList<>();
        private final Logger proxy = (Logger) Proxy.newProxyInstance(
                Logger.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                this
        );

        Logger proxy() {
            return proxy;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            String name = method.getName();
            if (List.of("lifecycle", "info", "debug", "error").contains(name)
                    && args != null
                    && args.length == 1
                    && args[0] instanceof String) {
                calls.add(name + ":" + args[0]);
                return null;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        }
    }
}
