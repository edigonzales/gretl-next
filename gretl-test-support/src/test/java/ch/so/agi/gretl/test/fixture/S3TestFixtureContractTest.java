package ch.so.agi.gretl.test.fixture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3TestFixtureContractTest {
    @Test
    void recordsTheFlociVersionAndImageOverrideUsedByTheFixture() {
        assertEquals("floci/floci:latest", S3TestFixture.IMAGE);
        assertTrue(S3TestFixture.IMAGE_DESCRIPTION.contains("1.10.0"));
        assertTrue(S3TestFixture.IMAGE_DESCRIPTION.contains(S3TestFixture.IMAGE));
    }
}
