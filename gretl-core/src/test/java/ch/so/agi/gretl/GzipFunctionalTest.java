package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GzipFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void compressesPlanregisterFixtureAndCreatesParentDirectory() throws Exception {
        writeSettings();
        Path input = copyResource("fixtures/gzip/planregister.xml", "input/planregister.xml");
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Gzip

                tasks.register('compressFile', Gzip) {
                    dataFile 'input/planregister.xml'
                    gzipFile layout.buildDirectory.file('nested/out/planregister.xml.gz')
                }
                """);

        run("compressFile");

        Path output = projectDir.resolve("build/nested/out/planregister.xml.gz");
        assertTrue(Files.exists(output));
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(output))) {
            assertEquals(Files.readString(input, StandardCharsets.UTF_8),
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void failsClearlyWhenInputFileIsMissing() throws Exception {
        writeSettings();
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Gzip

                tasks.register('compressFile', Gzip) {
                    dataFile 'missing.xml'
                    gzipFile layout.buildDirectory.file('out/missing.xml.gz')
                }
                """);

        BuildResult result = runAndFail("compressFile");

        assertTrue(result.getOutput().contains("missing.xml"));
    }

    @Test
    void supportsKotlinDsl() throws Exception {
        writeSettings();
        Path input = copyResource("fixtures/gzip/planregister.xml", "input/planregister.xml");
        writeKotlinBuild("""
                import ch.so.agi.gretl.tasks.Gzip

                plugins { id("ch.so.agi.gretl") }

                tasks.register<Gzip>("compressFile") {
                    dataFile("input/planregister.xml")
                    gzipFile(layout.buildDirectory.file("nested/out/planregister.xml.gz"))
                }
                """);

        run("compressFile");

        Path output = projectDir.resolve("build/nested/out/planregister.xml.gz");
        assertTrue(Files.exists(output));
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(output))) {
            assertEquals(Files.readString(input, StandardCharsets.UTF_8),
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
