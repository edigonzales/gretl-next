package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import java.util.List;

public final class HttpTestFixtureLease implements TestFixtureLease {
    private final RecordingHttpTestFixture fixture;
    private final String token;
    private final String id;
    private final String username;
    private final String password;
    private boolean closed;

    HttpTestFixtureLease(RecordingHttpTestFixture fixture, String token, String id, String username, String password) {
        this.fixture = fixture; this.token = token; this.id = id; this.username = username; this.password = password;
    }
    String username() { return username; }
    String password() { return password; }
    @Override public String id() { return id; }
    @Override public TestFixtureType type() { return TestFixtureType.HTTP; }
    @Override public synchronized TestFixtureEndpointView endpointView(TestJobExecutionTarget target) {
        if (closed) throw new IllegalStateException("HTTP fixture lease is closed");
        return fixture.endpoint(token, username, password, target);
    }
    @Override public boolean isHealthy() { return !closed && fixture.isRunning(); }
    public synchronized List<HttpRecordedRequest> requests() { if (closed) throw new IllegalStateException("HTTP fixture lease is closed"); return fixture.requests(token); }
    public synchronized void reset() { if (closed) throw new IllegalStateException("HTTP fixture lease is closed"); fixture.reset(token); }
    public synchronized boolean isConfigured() { return !closed; }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        fixture.reset(token);
        fixture.deleteConfiguration(token);
    }
}
