package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.fixture.FtpTestFixtureLease;
import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FtpRoundtripTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "network-ftp-roundtrip"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        var job = context.job();
        assertEquals("payload.bin", Files.readString(job.resolve("build/verification/listing.txt")).trim());
        assertArrayEquals(Files.readAllBytes(job.resolve("input/payload.bin")), Files.readAllBytes(job.resolve("build/download/payload.bin")));
        FtpTestFixtureLease lease = context.requireFixture("ftp", FtpTestFixtureLease.class);
        assertTrue(lease.isHealthy());
        assertFalse(lease.remoteFileExists("payload.bin"));
        assertEquals(java.util.List.of(), lease.listRemoteFiles());
    }
}
