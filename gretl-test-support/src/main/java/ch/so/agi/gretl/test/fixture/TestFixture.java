package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;

public interface TestFixture extends AutoCloseable {
    TestFixtureType type();

    void start(TestFixtureNetwork network);

    boolean isRunning();

    TestFixtureLease acquire(TestJobExecutionIdentity identity);

    @Override
    void close();
}
