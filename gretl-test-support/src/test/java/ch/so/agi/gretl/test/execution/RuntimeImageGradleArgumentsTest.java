package ch.so.agi.gretl.test.execution;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageGradleArgumentsTest {
    private final RuntimeImageGradleArguments arguments = new RuntimeImageGradleArguments();

    @Test
    void oneShotAlwaysAddsOfflineAndNoDaemon() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.ONE_SHOT, List.of("gzip"));

        assertTrue(result.contains("--offline"));
        assertTrue(result.contains("--no-daemon"));
        assertTrue(result.contains("--console=plain"));
    }

    @Test
    void serviceAlwaysAddsOfflineAndDaemon() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.SERVICE, List.of("tasks"));

        assertTrue(result.contains("--offline"));
        assertTrue(result.contains("--daemon"));
    }

    @Test
    void rejectsConflictingLifecycleFlags() {
        assertThrows(IllegalArgumentException.class,
                () -> arguments.arguments(RuntimeExecutionMode.ONE_SHOT, List.of("--daemon", "tasks")));
        assertThrows(IllegalArgumentException.class,
                () -> arguments.arguments(RuntimeExecutionMode.SERVICE, List.of("--no-daemon", "tasks")));
    }

    @Test
    void doesNotDuplicatePolicyArguments() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.ONE_SHOT,
                List.of("--offline", "--console=plain", "--no-daemon", "-Panswer=42", "gzip"));

        assertEquals(1, result.stream().filter("--offline"::equals).count());
        assertEquals(1, result.stream().filter("--console=plain"::equals).count());
        assertEquals(1, result.stream().filter("--no-daemon"::equals).count());
        assertTrue(result.containsAll(List.of("-Panswer=42", "gzip")));
    }

    @Test
    void rejectsRefreshDependencies() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> arguments.arguments(RuntimeExecutionMode.ONE_SHOT,
                        List.of("--refresh-dependencies", "tasks")));

        assertTrue(error.getMessage().contains("bundled-only dependency resolution"));
        assertTrue(error.getMessage().contains("--refresh-dependencies"));
    }
}
