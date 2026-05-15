package ch.so.agi.gretl.geotools.worker.logging;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerLoggingFallbackTest {

    @Test
    void writesErrorsToStderrAndOtherLevelsToStdout() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));

            GretlLogger logger = LogEnvironment.getLogger(WorkerLoggingFallbackTest.class);
            logger.lifecycle("visible");
            logger.info("diagnostic");
            logger.debug("verbose");
            logger.error("failed", null);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            LogEnvironment.clearLogSink();
        }

        String out = stdout.toString(StandardCharsets.UTF_8);
        String err = stderr.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("GRETL_WORKER|LIFECYCLE|WorkerLoggingFallbackTest: visible"));
        assertTrue(out.contains("GRETL_WORKER|INFO|WorkerLoggingFallbackTest: diagnostic"));
        assertTrue(out.contains("GRETL_WORKER|DEBUG|WorkerLoggingFallbackTest: verbose"));
        assertTrue(err.contains("GRETL_WORKER|ERROR|WorkerLoggingFallbackTest: failed"));
    }
}
