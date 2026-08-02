package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

public final class S3TestFixtureLease implements TestFixtureLease {
    private final S3TestFixture fixture;
    private final String id;
    private final String sourceBucket;
    private final String targetBucket;
    private final String accessKey;
    private final String secretKey;
    private boolean closed;

    S3TestFixtureLease(S3TestFixture fixture, String id, String sourceBucket, String targetBucket,
                       String accessKey, String secretKey) {
        this.fixture = fixture; this.id = id; this.sourceBucket = sourceBucket; this.targetBucket = targetBucket;
        this.accessKey = accessKey; this.secretKey = secretKey;
        try (var client = fixture.client(accessKey, secretKey)) {
            client.createBucket(builder -> builder.bucket(sourceBucket));
            client.createBucket(builder -> builder.bucket(targetBucket));
        }
    }
    @Override public String id() { return id; }
    @Override public TestFixtureType type() { return TestFixtureType.S3; }
    @Override public synchronized TestFixtureEndpointView endpointView(TestJobExecutionTarget target) {
        if (closed) throw new IllegalStateException("S3 fixture lease is closed");
        return fixture.endpoint(sourceBucket, targetBucket, accessKey, secretKey, target);
    }
    @Override public boolean isHealthy() { return !closed && fixture.isRunning(); }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        fixture.cleanup(sourceBucket, targetBucket, accessKey, secretKey);
    }
}
