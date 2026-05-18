package ch.so.agi.gretl.control.api;

import java.util.List;
import java.util.Map;

public record ClaimedRun(
        String runId,
        String jobId,
        String projectDir,
        List<String> tasks,
        Map<String, Object> parameters,
        Map<String, String> secrets,
        String jvmMaxHeap,
        List<String> jvmArgs,
        long timeoutSeconds) {
}
