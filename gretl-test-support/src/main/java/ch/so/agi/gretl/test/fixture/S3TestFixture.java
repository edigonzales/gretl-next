package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import io.floci.testcontainers.FlociContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.Network;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class S3TestFixture implements TestFixture {
    public static final String IMAGE = "floci/floci:latest";
    public static final String IMAGE_DESCRIPTION = "floci/floci:latest via io.floci:testcontainers-floci:1.10.0";
    private final FlociContainer container = new FlociContainer(DockerImageName.parse(IMAGE))
            .withRegion("us-east-1").withNetworkAliases("s3");
    private TestFixtureNetwork network;
    private boolean closed;

    @Override public TestFixtureType type() { return TestFixtureType.S3; }
    @Override public synchronized void start(TestFixtureStartContext context) {
        if (closed) throw new IllegalStateException("S3 fixture is closed");
        if (this.network != null) return;
        TestFixtureNetwork network = context.requireNetwork(type());
        this.network = network;
        container.withNetwork(network.testcontainersNetwork()).start();
    }
    @Override public boolean isRunning() { return container.isRunning(); }
    @Override public synchronized TestFixtureLease acquire(TestJobExecutionIdentity identity) {
        if (!isRunning()) throw new IllegalStateException("S3 fixture is not running");
        String token = identity.shortToken();
        return new S3TestFixtureLease(this, identity.namespace(),
                "gretl-source-" + token + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                "gretl-target-" + token + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                container.getAccessKey(), container.getSecretKey());
    }

    TestFixtureEndpointView endpoint(String sourceBucket, String targetBucket, String accessKey,
                                     String secretKey, TestJobExecutionTarget target) {
        boolean runtime = target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE;
        return new TestFixtureEndpointView(Map.of(
                "endpoint", TestFixtureValue.publicValue(runtime ? "http://s3:4566" : container.getEndpoint()),
                "region", TestFixtureValue.publicValue(container.getRegion()),
                "accessKey", TestFixtureValue.secretValue(accessKey),
                "secretKey", TestFixtureValue.secretValue(secretKey),
                "sourceBucket", TestFixtureValue.publicValue(sourceBucket),
                "targetBucket", TestFixtureValue.publicValue(targetBucket)),
                runtime ? Optional.of(network.dockerNetworkId()) : Optional.empty());
    }

    void cleanup(String sourceBucket, String targetBucket, String accessKey, String secretKey) {
        RuntimeException failure = null;
        try (var client = client(accessKey, secretKey)) {
            for (String bucket : new String[]{sourceBucket, targetBucket}) {
                try {
                    client.listObjectsV2Paginator(builder -> builder.bucket(bucket)).stream()
                            .flatMap(response -> response.contents().stream())
                            .forEach(object -> client.deleteObject(builder -> builder.bucket(bucket).key(object.key())));
                    client.deleteBucket(builder -> builder.bucket(bucket));
                } catch (RuntimeException cleanup) {
                    if (failure == null) failure = new IllegalStateException("Could not clean S3 fixture lease");
                    failure.addSuppressed(cleanup);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    boolean objectExists(String bucket, String key, String accessKey, String secretKey) {
        try (var client = client(accessKey, secretKey)) {
            try { client.headObject(builder -> builder.bucket(bucket).key(key)); return true; }
            catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) { return false; }
            catch (software.amazon.awssdk.services.s3.model.S3Exception e) { if (e.statusCode() == 404) return false; throw e; }
        }
    }

    byte[] readObject(String bucket, String key, String accessKey, String secretKey) {
        try (var client = client(accessKey, secretKey)) { return client.getObjectAsBytes(builder -> builder.bucket(bucket).key(key)).asByteArray(); }
    }

    java.util.List<String> listKeys(String bucket, String accessKey, String secretKey) {
        try (var client = client(accessKey, secretKey)) {
            return client.listObjectsV2Paginator(builder -> builder.bucket(bucket)).stream()
                    .flatMap(response -> response.contents().stream()).map(software.amazon.awssdk.services.s3.model.S3Object::key).sorted().toList();
        }
    }

    software.amazon.awssdk.services.s3.S3Client client(String accessKey, String secretKey) {
        return software.amazon.awssdk.services.s3.S3Client.builder()
                .endpointOverride(URI.create(container.getEndpoint()))
                .region(software.amazon.awssdk.regions.Region.of(container.getRegion()))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (container.isRunning()) container.stop();
    }
}
