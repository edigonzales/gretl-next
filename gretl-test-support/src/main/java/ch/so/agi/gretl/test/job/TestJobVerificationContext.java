package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.fixture.PreparedTestJobEnvironment;
import ch.so.agi.gretl.test.fixture.TestFixtureLease;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;

import java.util.Objects;

public record TestJobVerificationContext(
        MaterializedTestJob job,
        GretlBuildResult result,
        TaskExecutionTrace trace,
        PreparedTestJobEnvironment environment) {
    public TestJobVerificationContext {
        Objects.requireNonNull(job, "job must not be null");
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(trace, "trace must not be null");
        environment = environment == null
                ? new PreparedTestJobEnvironment(ch.so.agi.gretl.test.fixture.TestJobEnvironment.empty(), java.util.Map.of())
                : environment;
    }

    public <T extends TestFixtureLease> T requireFixture(String fixtureId, Class<T> type) {
        return environment.requireLease(fixtureId, type);
    }
}
