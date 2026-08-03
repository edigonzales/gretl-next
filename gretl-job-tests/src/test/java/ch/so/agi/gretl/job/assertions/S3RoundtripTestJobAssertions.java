package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.fixture.S3TestFixtureLease;
import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class S3RoundtripTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "network-s3-roundtrip"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        S3TestFixtureLease lease = context.requireFixture("s3", S3TestFixtureLease.class);
        byte[] expected = Files.readAllBytes(context.job().resolve("input/one.txt"));
        assertEquals(java.util.List.of("one.txt"), lease.listKeys(lease.sourceBucket()));
        assertTrue(lease.objectExists(lease.sourceBucket(), "one.txt"));
        assertArrayEquals(expected, lease.readObject(lease.sourceBucket(), "one.txt"));
        assertArrayEquals(expected, Files.readAllBytes(context.job().resolve("build/download/one.txt")));
        assertEquals(java.util.List.of(), lease.listKeys(lease.targetBucket()));
        assertTrue(!lease.objectExists(lease.targetBucket(), "one.txt"));
    }
}
