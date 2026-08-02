package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

public interface TestFixtureLease extends AutoCloseable {
    String id();

    TestFixtureType type();

    TestFixtureEndpointView endpointView(TestJobExecutionTarget target);

    boolean isHealthy();

    @Override
    void close();
}
