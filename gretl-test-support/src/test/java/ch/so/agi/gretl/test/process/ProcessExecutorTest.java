package ch.so.agi.gretl.test.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessExecutorTest {
    @TempDir
    Path temp;

    @Test
    void capturesBothStreamsAndPreservesArguments() {
        ProcessResult result = new ProcessExecutor().execute(new ProcessRequest(
                List.of("sh", "-c", "printf '%s' \"$1\"; printf '%s' stderr >&2", "shell", "argument with spaces"),
                temp, Map.of(), Duration.ofSeconds(10), Set.of("stderr")));
        assertEquals(0, result.exitCode());
        assertEquals("argument with spaces", result.standardOutput());
        assertEquals("***", result.standardError());
    }

    @Test
    void terminatesTimedOutProcess() {
        ProcessResult result = new ProcessExecutor().execute(new ProcessRequest(
                List.of("sh", "-c", "sleep 10"), temp, Map.of(), Duration.ofMillis(100), Set.of()));
        assertNotEquals(0, result.exitCode());
        assertTrue(result.standardError().contains("timed out"));
    }
}
