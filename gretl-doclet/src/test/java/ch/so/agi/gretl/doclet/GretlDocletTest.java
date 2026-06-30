package ch.so.agi.gretl.doclet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GretlDocletTest {
    private final DocumentationTool documentationTool = ToolProvider.getSystemDocumentationTool();

    @Test
    void rendersFixtureTaskAsAsciiDocGoldenFile(@TempDir Path tempDir) throws IOException {
        generate(tempDir, List.of(Path.of("src/test/java/ch/so/agi/gretl/doclet/fixtures/FixtureTask.java")));

        String actual = Files.readString(tempDir.resolve("task-fixturetask.adoc"));
        String expected = Files.readString(Path.of("src/test/resources/ch/so/agi/gretl/doclet/expected-fixturetask.adoc"));
        assertEquals(normalize(expected), normalize(actual));
    }

    @Test
    void rendersFixtureTaskWithGermanLocale(@TempDir Path tempDir) throws IOException {
        generate(tempDir, List.of(Path.of("src/test/java/ch/so/agi/gretl/doclet/fixtures/FixtureTask.java")), "de_CH");

        String actual = Files.readString(tempDir.resolve("task-fixturetask.adoc"));
        String expected = Files.readString(Path.of("src/test/resources/ch/so/agi/gretl/doclet/expected-fixturetask_de.adoc"));
        assertEquals(normalize(expected), normalize(actual));
    }

    @Test
    void documentsRealGretlTasksWithoutInternalGradleProperties(@TempDir Path tempDir) throws IOException {
        generate(tempDir, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Db2Db.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Gzip.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/XslTransformer.java"),
                Path.of("../gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks/ReadShapefile.java"),
                Path.of("../gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks/Vectorize.java"),
                Path.of("../gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks/RasterReclassify.java")));

        String index = Files.readString(tempDir.resolve("task-reference.adoc"));
        assertTrue(index.contains("include::task-sqlexecutor.adoc[]"));
        assertTrue(index.contains("include::task-db2db.adoc[]"));
        assertTrue(index.contains("include::task-rasterreclassify.adoc[]"));

        String sqlExecutor = Files.readString(tempDir.resolve("task-sqlexecutor.adoc"));
        assertTrue(sqlExecutor.contains("`database(String jdbcUrl)`"));
        assertTrue(sqlExecutor.contains("`sqlFiles(Object... paths)`"));
        assertFalse(sqlExecutor.contains("getJdbcUrl"));
        assertFalse(sqlExecutor.contains("getPassword"));
        assertFalse(sqlExecutor.contains("coreService"));
        assertFalse(sqlExecutor.contains("executeSQLExecutor"));

        String vectorize = Files.readString(tempDir.resolve("task-vectorize.adoc"));
        assertTrue(vectorize.contains("`cellValues(Number... values)`"));
        assertFalse(vectorize.contains("workerClasspath"));
        assertFalse(vectorize.contains("geoToolsService"));
        assertFalse(vectorize.contains("execute()"));
    }

    private void generate(Path outputDirectory, List<Path> sources) {
        generate(outputDirectory, sources, null);
    }

    private void generate(Path outputDirectory, List<Path> sources, String locale) {
        List<String> args = new ArrayList<>();
        args.add("-d");
        args.add(outputDirectory.toString());
        if (locale != null) {
            args.add("-docletlocale");
            args.add(locale);
        }
        args.add("-classpath");
        args.add(System.getProperty("java.class.path"));
        args.add("-quiet");
        sources.stream()
                .map(Path::toString)
                .forEach(args::add);

        DocumentationTool.DocumentationTask task = documentationTool.getTask(
                null,
                null,
                null,
                GretlDoclet.class,
                args,
                null);
        assertTrue(task.call());
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }
}
