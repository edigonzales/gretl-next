package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class GzipTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "core-gzip"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        var output = context.job().resolve("build/output/data.txt.gz");
        assertTrue(Files.isRegularFile(output), "GZIP output missing: " + output);
        try (var input = new GZIPInputStream(Files.newInputStream(output))) {
            assertArrayEquals(Files.readAllBytes(context.job().resolveExpected("payload.txt")), input.readAllBytes());
        }
    }
}
