package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CombinedBuildResultAssertions {
    public static void assertSuccess(BuildResult result) {
        assertNotNull(result, "Build result must not be null");
    }

    public static void assertFailureContains(BuildResult result, String... fragments) {
        String output = result == null ? "" : result.getOutput();
        for (String fragment : fragments) {
            assertTrue(output.contains(fragment), "Expected build output to contain '" + fragment + "'\n" + output);
        }
    }

    public static void assertOutcome(BuildResult result, String taskPath, TaskOutcome expected) {
        BuildTask task = result.task(taskPath);
        assertNotNull(task, "Task " + taskPath + " was not present in the build output:\n" + result.getOutput());
        assertEquals(expected, task.getOutcome(), "Unexpected outcome for " + taskPath);
    }

    public static void assertNotExecuted(BuildResult result, String taskPath) {
        BuildTask task = result.task(taskPath);
        assertTrue(task == null || task.getOutcome() == TaskOutcome.SKIPPED,
                "Task " + taskPath + " unexpectedly executed: " + task);
    }

    public static void assertNoClassloaderFailure(BuildResult result) {
        String output = result.getOutput();
        for (String forbidden : new String[] {
                "NoClassDefFoundError", "ClassNotFoundException", "LinkageError",
                "ServiceConfigurationError", "Could not create service", "already registered",
                "Multiple SLF4J providers", "SLF4J: Class path contains multiple"
        }) {
            assertFalse(output.contains(forbidden), "Unexpected classloader/service warning: " + forbidden);
        }
    }

    public static void assertNoWorkerProtocolLeak(BuildResult result) {
        assertFalse(result.getOutput().contains("GRETL_WORKER|"),
                "Worker protocol frames must be bridged before reaching normal build output.");
    }

    public static void assertConfigurationCacheStored(BuildResult result) {
        assertTrue(result.getOutput().contains("Configuration cache entry stored")
                        || result.getOutput().contains("Configuration cache entry reused"),
                "No configuration-cache storage/reuse marker found:\n" + result.getOutput());
    }

    public static void assertConfigurationCacheReused(BuildResult result) {
        assertTrue(result.getOutput().contains("Configuration cache entry reused"),
                "No configuration-cache reuse marker found:\n" + result.getOutput());
    }

    private CombinedBuildResultAssertions() {
    }
}
