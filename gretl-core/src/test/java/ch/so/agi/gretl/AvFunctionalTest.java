package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvFunctionalTest extends CoreFunctionalTestSupport {
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    @Test
    void convertsSingleItfWithAv2ch() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/av/Av2ch", projectDir);
        writeBuild(avBuild("""
                tasks.register('transform', Av2ch) {
                    inputFiles '254900.itf'
                    outputDirectory layout.buildDirectory.dir('output')
                }
                """));

        run("transform");

        Path outputFile = onlyFile(projectDir.resolve("build/output"), ".itf");
        String content = Files.readString(outputFile, ISO_8859_1);
        assertTrue(content.contains("DM01 Interlis Converter"));
        assertTrue(content.contains("MODL DM01AVCH24LV95D"));
        assertTrue(content.contains("TABL LFP3Nachfuehrung"));
    }

    @Test
    void convertsItfFileTreeWithAv2chAndZip() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/av/Av2chFileSet", projectDir);
        writeBuild(avBuild("""
                tasks.register('transform', Av2ch) {
                    inputFiles fileTree(projectDir) { include '*.itf' }
                    outputDirectory layout.buildDirectory.dir('output')
                    zip true
                }
                """));

        run("transform");

        Path outputDirectory = projectDir.resolve("build/output");
        assertTrue(Files.isRegularFile(outputDirectory.resolve("252400.itf")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("254900.itf")));
        assertZipContains(outputDirectory.resolve("252400.itf.zip"), "252400.itf");
        assertZipContains(outputDirectory.resolve("254900.itf.zip"), "254900.itf");
    }

    @Test
    void failsAv2chForInvalidInput() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/av/Av2chFail", projectDir);
        writeBuild(avBuild("""
                tasks.register('transform', Av2ch) {
                    inputFiles 'fubar.itf'
                    outputDirectory layout.buildDirectory.dir('output')
                }
                """));

        BuildResult result = runAndFail("transform");

        assertTrue(result.getOutput().contains("failed to run Av2ch"));
    }

    @Test
    void convertsSingleItfWithAv2geobau() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/av/Av2geobau", projectDir);
        writeBuild(avBuild("""
                tasks.register('av2geobau', Av2geobau) {
                    itfFiles 'empty.itf'
                    modeldir projectDir.toString()
                    dxfDirectory layout.buildDirectory.dir('dxf')
                    logFile layout.buildDirectory.file('logs/av2geobau.log')
                }
                """));

        run("av2geobau");

        Path dxf = projectDir.resolve("build/dxf/empty.dxf");
        assertTrue(Files.isRegularFile(dxf));
        String content = Files.readString(dxf, ISO_8859_1);
        assertTrue(content.contains("SECTION"));
        assertTrue(content.contains("ENTITIES"));
        assertTrue(Files.isRegularFile(projectDir.resolve("build/logs/av2geobau.log")));
    }

    @Test
    void convertsItfFileTreeWithAv2geobauAndZip() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/av/Av2geobauFileSet", projectDir);
        writeBuild(avBuild("""
                tasks.register('av2geobau', Av2geobau) {
                    itfFiles fileTree(projectDir) { include '*.itf' }
                    modeldir projectDir.toString()
                    dxfDirectory layout.buildDirectory.dir('dxf')
                    zip true
                }
                """));

        run("av2geobau");

        Path outputDirectory = projectDir.resolve("build/dxf");
        assertTrue(Files.isRegularFile(outputDirectory.resolve("empty1.dxf")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("empty2.dxf")));
        assertZipContains(outputDirectory.resolve("empty1.zip"), "empty1.dxf");
        assertZipContains(outputDirectory.resolve("empty1.zip"), "DXF_Geobau_Layerdefinition.pdf");
        assertZipContains(outputDirectory.resolve("empty1.zip"), "Hinweise.pdf");
        assertZipContains(outputDirectory.resolve("empty1.zip"), "Musterplan.pdf");
    }

    private String avBuild(String tasks) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Av2ch
                import ch.so.agi.gretl.tasks.Av2geobau

                %s
                """.formatted(tasks);
    }

    private Path onlyFile(Path directory, String extension) throws Exception {
        try (var stream = Files.list(directory)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .toList();
            assertEquals(1, files.size());
            return files.get(0);
        }
    }

    private void assertZipContains(Path zipFile, String entryName) throws Exception {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            assertNotNull(zip.getEntry(entryName), zipFile + " should contain " + entryName);
        }
    }
}
