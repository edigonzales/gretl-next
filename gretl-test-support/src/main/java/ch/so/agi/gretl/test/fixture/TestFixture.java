package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;

public interface TestFixture extends AutoCloseable {
    TestFixtureType type();

    default boolean requiresDockerNetwork() {
        return true;
    }

    void start(TestFixtureStartContext context);

    boolean isRunning();

    TestFixtureLease acquire(TestJobExecutionIdentity identity);

    @Override
    void close();
}
