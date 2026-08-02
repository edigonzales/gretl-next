package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

public final class FtpTestFixtureLease implements TestFixtureLease {
    private final FtpTestFixture fixture;
    private final String id;
    private final String directory;
    private final String username;
    private final String password;
    private boolean closed;

    FtpTestFixtureLease(FtpTestFixture fixture, String id, String directory, String username, String password) {
        this.fixture = fixture; this.id = id; this.directory = directory;
        this.username = username; this.password = password;
    }
    @Override public String id() { return id; }
    @Override public TestFixtureType type() { return TestFixtureType.FTP; }
    @Override public synchronized TestFixtureEndpointView endpointView(TestJobExecutionTarget target) {
        if (closed) throw new IllegalStateException("FTP fixture lease is closed");
        return fixture.endpoint(directory, username, password, target);
    }
    @Override public boolean isHealthy() { return !closed && fixture.isRunning(); }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        fixture.deleteDirectory(directory);
    }
}
