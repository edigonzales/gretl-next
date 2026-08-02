package ch.so.agi.gretl.test.job;

import java.util.Set;

public interface TestJobExecutionBackendFactory extends AutoCloseable {
    /** Returns the session-owned backend for the target. */
    TestJobExecutionBackend require(TestJobExecutionTarget target);

    /**
     * Compatibility entry point for callers that still construct a factory
     * with a per-call context. New code must use {@link #require}.
     */
    default TestJobExecutionBackend create(TestJobExecutionTarget target, TestJobBackendContext context) {
        return require(target);
    }

    default Set<TestJobExecutionTarget> availableTargets() {
        return Set.of(TestJobExecutionTarget.values());
    }

    @Override default void close() { }
}
