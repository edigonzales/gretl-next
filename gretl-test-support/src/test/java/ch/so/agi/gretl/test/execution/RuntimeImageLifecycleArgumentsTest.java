package ch.so.agi.gretl.test.execution;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageLifecycleArgumentsTest {
    private final RuntimeImageLifecycleArguments arguments = new RuntimeImageLifecycleArguments();

    @Test
    void oneShotAddsNoDaemon() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.ONE_SHOT, List.of("gzip"));

        assertTrue(result.contains("--no-daemon"));
        assertTrue(result.contains("--console=plain"));
    }

    @Test
    void serviceAddsDaemon() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.SERVICE, List.of("tasks"));

        assertTrue(result.contains("--daemon"));
        assertTrue(result.contains("--console=plain"));
    }

    @Test
    void bothModesAddPlainConsole() {
        assertTrue(arguments.arguments(RuntimeExecutionMode.ONE_SHOT, List.of()).contains("--console=plain"));
        assertTrue(arguments.arguments(RuntimeExecutionMode.SERVICE, List.of()).contains("--console=plain"));
    }

    @Test
    void lifecycleArgumentsDoNotAddOffline() {
        assertTrue(arguments.arguments(RuntimeExecutionMode.ONE_SHOT, List.of("tasks")).stream()
                .noneMatch("--offline"::equals));
    }

    @Test
    void preservesExplicitOfflineWithoutDuplicatingIt() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.ONE_SHOT,
                List.of("--offline", "-Panswer=42", "gzip"));

        assertEquals(1, result.stream().filter("--offline"::equals).count());
        assertTrue(result.containsAll(List.of("-Panswer=42", "gzip")));
    }

    @Test
    void oneShotRejectsDaemon() {
        assertThrows(IllegalArgumentException.class,
                () -> arguments.arguments(RuntimeExecutionMode.ONE_SHOT, List.of("--daemon", "tasks")));
    }

    @Test
    void serviceRejectsNoDaemon() {
        assertThrows(IllegalArgumentException.class,
                () -> arguments.arguments(RuntimeExecutionMode.SERVICE, List.of("--no-daemon", "tasks")));
    }

    @Test
    void rejectsRefreshDependencies() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> arguments.arguments(RuntimeExecutionMode.ONE_SHOT,
                        List.of("--refresh-dependencies", "tasks")));

        assertTrue(error.getMessage().contains("local-only"));
        assertTrue(error.getMessage().contains("--refresh-dependencies"));
    }

    @Test
    void preservesProjectProperties() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.ONE_SHOT,
                List.of("-PprojectValue=42", "tasks"));

        assertTrue(result.contains("-PprojectValue=42"));
    }

    @Test
    void preservesSystemProperties() {
        List<String> result = arguments.arguments(RuntimeExecutionMode.ONE_SHOT,
                List.of("-DsystemValue=42", "tasks"));

        assertTrue(result.contains("-DsystemValue=42"));
    }

    @Test
    void preservesTaskNamesAndOrdering() {
        List<String> requested = List.of("-Pvalue=1", ":first", ":second");
        List<String> result = arguments.arguments(RuntimeExecutionMode.ONE_SHOT, requested);

        assertEquals(List.of("-Pvalue=1", ":first", ":second"), result.subList(0, requested.size()));
    }
}
