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
        assertFalse(actual.contains("== FixtureTask"));
    }

    @Test
    void rendersFixtureTaskWithGermanLocale(@TempDir Path tempDir) throws IOException {
        generate(tempDir, List.of(Path.of("src/test/java/ch/so/agi/gretl/doclet/fixtures/FixtureTask.java")), "de_CH");

        String actual = Files.readString(tempDir.resolve("task-fixturetask.adoc"));
        String expected = Files.readString(Path.of("src/test/resources/ch/so/agi/gretl/doclet/expected-fixturetask_de.adoc"));
        assertEquals(normalize(expected), normalize(actual));
        assertTrue(actual.contains("| DSL-Methode | Beschreibung | Erforderlich"));
        assertFalse(actual.contains("| Standard"));
        assertFalse(actual.contains("\n| 7\n"));
    }

    @Test
    void rendersInheritedDslMethodsWithJavaOverrideSemantics(@TempDir Path tempDir) throws IOException {
        generate(tempDir, List.of(
                Path.of("src/test/java/ch/so/agi/gretl/doclet/fixtures/InheritedFixtureTask.java")));

        String actual = Files.readString(tempDir.resolve("task-inheritedfixturetask.adoc"));
        assertTrue(actual.contains("`inheritedInput(String value)`"));
        assertTrue(actual.contains("`inheritedOption(String value)`"));
        assertTrue(actual.contains("`inheritedOption(String value, int count)`"));
        assertTrue(actual.contains("`localMethod(String value)`"));
        assertTrue(actual.contains("Configures the derived override."));
        assertEquals(1, actual.lines()
                .filter(line -> line.startsWith("| `overridden("))
                .count());
        assertFalse(actual.contains("`hidden("));
        assertFalse(actual.contains("`internalHelper("));
    }

    @Test
    void rendersLocalizedDescriptionOfInheritedDslMethod(@TempDir Path tempDir) throws IOException {
        generate(tempDir, List.of(
                Path.of("src/test/java/ch/so/agi/gretl/doclet/fixtures/InheritedFixtureTask.java")), "de_CH");

        String actual = Files.readString(tempDir.resolve("task-inheritedfixturetask.adoc"));
        assertTrue(actual.contains("Konfiguriert eine geerbte Eingabe."));
    }

    @Test
    void removesStaleGeneratedAsciiDocFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("task-stale.adoc"), "stale");

        generate(tempDir, List.of(Path.of("src/test/java/ch/so/agi/gretl/doclet/fixtures/FixtureTask.java")));

        assertFalse(Files.exists(tempDir.resolve("task-stale.adoc")));
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

    @Test
    void documentsInheritedDslMethodsOfAllRealTasks(@TempDir Path tempDir) throws IOException {
        generate(tempDir, realTaskSources());

        try (var generatedFiles = Files.list(tempDir)) {
            long taskFiles = generatedFiles
                    .filter(path -> path.getFileName().toString().startsWith("task-"))
                    .filter(path -> !path.getFileName().toString().equals("task-reference.adoc"))
                    .count();
            assertEquals(45, taskFiles);
        }

        try (var generatedFiles = Files.list(tempDir)) {
            for (Path taskFile : generatedFiles
                    .filter(path -> path.getFileName().toString().startsWith("task-"))
                    .filter(path -> !path.getFileName().toString().equals("task-reference.adoc"))
                    .toList()) {
                String taskDoc = Files.readString(taskFile);
                assertTrue(taskDoc.contains("| `"),
                        taskFile.getFileName() + " should contain at least one DSL method");
            }
        }

        String csvValidator = Files.readString(tempDir.resolve("task-csvvalidator.adoc"));
        assertTrue(csvValidator.contains("`dataFiles(Object... paths)`"));
        assertTrue(csvValidator.contains("`models(String value)`"));

        String ili2pgImport = Files.readString(tempDir.resolve("task-ili2pgimport.adoc"));
        assertTrue(ili2pgImport.contains("`database(String jdbcUrl)`"));
        assertTrue(ili2pgImport.contains("`schema(String name)`"));
        assertTrue(ili2pgImport.contains("`transferFiles(Object... paths)`"));

        String gpkgValidator = Files.readString(tempDir.resolve("task-gpkgvalidator.adoc"));
        assertTrue(gpkgValidator.contains("`dataFiles(Object... paths)`"));
        assertTrue(gpkgValidator.contains("`tableName(String value)`"));
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

    private static List<Path> realTaskSources() throws IOException {
        List<Path> sources = new ArrayList<>();
        addJavaSources(sources, Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks"));
        addJavaSources(sources, Path.of("../gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks"));
        return sources;
    }

    private static void addJavaSources(List<Path> sources, Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .forEach(sources::add);
        }
    }
}
