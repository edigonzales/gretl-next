package ch.so.agi.gretl.doclet.lsp;

import ch.so.agi.gretl.doclet.GretlDoclet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LspMetadataBuilderTest {

    private final DocumentationTool documentationTool = ToolProvider.getSystemDocumentationTool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatesLspMetadataWithSqlExecutor(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        assertTrue(Files.exists(metadataFile), "Metadata file should exist");

        JsonNode root = objectMapper.readTree(metadataFile.toFile());
        assertEquals("1.0.0", root.get("schemaVersion").asText());
        assertNotNull(root.get("generatedAt"));
        assertNotNull(root.get("gretlVersion"));
        assertNotNull(root.get("source"));

        JsonNode tasks = root.get("tasks");
        assertTrue(tasks.isArray());
        assertTrue(tasks.size() > 0);

        JsonNode sqlExecutor = findTask(tasks, "SqlExecutor");
        assertNotNull(sqlExecutor, "SqlExecutor task should be present");
        assertEquals("ch.so.agi.gretl.tasks.SqlExecutor", sqlExecutor.get("qualifiedClassName").asText());
        assertEquals("SqlExecutor", sqlExecutor.get("simpleClassName").asText());
        assertEquals("database", sqlExecutor.get("category").asText());
        assertEquals("stable", sqlExecutor.get("status").asText());
        assertNotNull(sqlExecutor.get("description"));
        assertTrue(sqlExecutor.get("description").asText().length() > 0);

        JsonNode properties = sqlExecutor.get("properties");
        assertTrue(properties.isArray());
        assertTrue(properties.size() >= 4, "SqlExecutor should have at least 4 properties");

        JsonNode database = findProperty(properties, "database");
        assertNotNull(database, "database property should exist");
        assertEquals("dsl-method-and-property", database.get("kind").asText());
        assertTrue(database.get("required").asBoolean(), "database should be required");
        assertEquals("Connector", database.get("valueType").asText());

        JsonNode dbAcceptedForms = database.get("acceptedForms");
        assertTrue(dbAcceptedForms.isArray());
        assertTrue(dbAcceptedForms.size() >= 1);

        JsonNode sqlFiles = findProperty(properties, "sqlFiles");
        assertNotNull(sqlFiles, "sqlFiles property should exist");
        assertTrue(sqlFiles.get("required").asBoolean(), "sqlFiles should be required");
        assertEquals("FileCollection", sqlFiles.get("valueType").asText());

        JsonNode sqlFilesFile = sqlFiles.get("file");
        assertNotNull(sqlFilesFile, "sqlFiles should have file metadata");
        assertEquals("input", sqlFilesFile.get("role").asText());
        assertTrue(sqlFilesFile.get("extensions").isArray());
        boolean hasSql = false;
        for (JsonNode ext : sqlFilesFile.get("extensions")) {
            if (ext.asText().equals(".sql")) hasSql = true;
        }
        assertTrue(hasSql, "sqlFiles extensions should contain .sql");
        assertTrue(sqlFilesFile.get("mustExist").asBoolean());

        JsonNode sqlParams = findProperty(properties, "sqlParameters");
        assertNotNull(sqlParams, "sqlParameters property should exist");
        assertTrue(sqlParams.has("sqlParameterProvider"));
        assertTrue(sqlParams.get("sqlParameterProvider").asBoolean(),
                "sqlParameters should be marked as sqlParameterProvider");
        assertTrue(sqlParams.has("required"));
        assertEquals(false, sqlParams.get("required").asBoolean(),
                "sqlParameters should not be required by annotation");

        JsonNode sqlParamSets = findProperty(properties, "sqlParameterSets");
        assertNotNull(sqlParamSets, "sqlParameterSets property should exist");

        for (JsonNode prop : properties) {
            assertNotNull(prop.get("acceptedForms"), "Every property should have acceptedForms");
            assertTrue(prop.get("acceptedForms").isArray());
            assertTrue(prop.get("acceptedForms").size() > 0, "Every property should have at least one accepted form");
        }
    }

    @Test
    void propertiesAreSortedAlphabetically(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());
        JsonNode sqlExecutor = findTask(root.get("tasks"), "SqlExecutor");
        JsonNode properties = sqlExecutor.get("properties");

        String prevName = "";
        for (JsonNode prop : properties) {
            String name = prop.get("name").asText();
            assertTrue(name.compareTo(prevName) >= 0,
                    "Properties should be sorted: " + prevName + " before " + name);
            prevName = name;
        }
    }

    @Test
    void tasksAreSortedAlphabetically(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Gzip.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Curl.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());
        JsonNode tasks = root.get("tasks");

        String prevName = "";
        for (JsonNode task : tasks) {
            String name = task.get("name").asText();
            assertTrue(name.compareTo(prevName) >= 0,
                    "Tasks should be sorted: " + prevName + " before " + name);
            prevName = name;
        }
    }

    private void generate(Path adocOutput, Path lspOutput, List<Path> sources) {
        List<String> args = new ArrayList<>();
        args.add("-d");
        args.add(adocOutput.toString());
        args.add("-lspoutput");
        args.add(lspOutput.toString());
        args.add("-classpath");
        args.add(System.getProperty("java.class.path"));
        args.add("-quiet");
        sources.stream()
                .map(Path::toString)
                .forEach(args::add);

        DocumentationTool.DocumentationTask task = documentationTool.getTask(
                null, null, null, GretlDoclet.class, args, null);
        assertTrue(task.call(), "Doclet should complete successfully");
    }

    private static JsonNode findTask(JsonNode tasks, String name) {
        for (JsonNode task : tasks) {
            if (task.get("name").asText().equals(name)) {
                return task;
            }
        }
        return null;
    }

    private static JsonNode findProperty(JsonNode properties, String name) {
        for (JsonNode prop : properties) {
            if (prop.get("name").asText().equals(name)) {
                return prop;
            }
        }
        return null;
    }
}
