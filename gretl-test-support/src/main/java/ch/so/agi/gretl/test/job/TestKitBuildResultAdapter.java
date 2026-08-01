package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.GretlTaskOutcome;
import ch.so.agi.gretl.test.process.SecretMasker;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestKitBuildResultAdapter {
    public GretlBuildResult adapt(BuildResult result, int exitCode, Duration duration,
                                  List<String> arguments, Set<String> secrets) {
        Map<String, GretlTaskOutcome> outcomes = new LinkedHashMap<>();
        result.getTasks().forEach(task -> outcomes.put(task.getPath(), map(task.getOutcome())));
        List<String> sanitized = new SecretMasker().maskArguments(arguments, secrets);
        return new GretlBuildResult(exitCode, result.getOutput(), "", duration, sanitized, outcomes);
    }

    public GretlTaskOutcome map(TaskOutcome outcome) {
        if (outcome == null) return GretlTaskOutcome.UNKNOWN;
        return switch (outcome) {
            case SUCCESS -> GretlTaskOutcome.SUCCESS;
            case FAILED -> GretlTaskOutcome.FAILED;
            case SKIPPED -> GretlTaskOutcome.SKIPPED;
            case UP_TO_DATE -> GretlTaskOutcome.UP_TO_DATE;
            case FROM_CACHE -> GretlTaskOutcome.FROM_CACHE;
            case NO_SOURCE -> GretlTaskOutcome.NO_SOURCE;
        };
    }
}
