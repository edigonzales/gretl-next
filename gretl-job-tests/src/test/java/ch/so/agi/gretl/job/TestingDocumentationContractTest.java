package ch.so.agi.gretl.job;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingDocumentationContractTest {
    @Test
    void centralTestingGuideDescribesTheExecutableGates() throws Exception {
        Path root = Path.of(System.getProperty("gretl.test.repositoryRoot", ".."));
        String guide = Files.readString(root.resolve("docs/testing/testing.adoc"));
        assertTrue(guide.contains("./gradlew clean check"));
        assertTrue(guide.contains("sourceIntegrationTest"));
        assertTrue(guide.contains("runtimeImageTest"));
        assertTrue(guide.contains("extendedRuntimeImageTest"));
        assertTrue(guide.contains("canonicalJobOptionalServiceTest"));
        assertFalse(guide.contains("includeOptional"));
        assertTrue(Files.isRegularFile(root.resolve("docs/testing/task-coverage.yaml")));
    }

    @Test
    void repositoryEntryPointsLinkToTheCentralGuide() throws Exception {
        Path root = Path.of(System.getProperty("gretl.test.repositoryRoot", ".."));
        assertTrue(Files.readString(root.resolve("README.md")).contains("docs/testing/testing.adoc"));
        assertTrue(Files.readString(root.resolve("docs/index.md")).contains("testing/testing.adoc"));
    }
}
