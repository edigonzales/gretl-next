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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LspMetadataSchemaTest {

    private final DocumentationTool documentationTool = ToolProvider.getSystemDocumentationTool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> VALID_STATUSES = Set.of("stable", "incubating", "deprecated", "internal", "experimental");
    private static final Set<String> VALID_KINDS = Set.of("property", "dsl-method", "dsl-method-and-property", "gradle-inherited", "internal");
    private static final Set<String> VALID_FILE_ROLES = Set.of("input", "output", "input-output", "directory-input", "directory-output", "unknown");
    private static final Set<String> VALID_FORM_STYLES = Set.of("method-call", "assignment", "set-method", "unknown");

    @Test
    void generatedDocumentHasRequiredTopLevelFields(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());

        assertTrue(root.has("schemaVersion"), "Missing schemaVersion");
        assertTrue(root.has("generatedAt"), "Missing generatedAt");
        assertTrue(root.has("gretlVersion"), "Missing gretlVersion");
        assertTrue(root.has("source"), "Missing source");
        assertTrue(root.has("tasks"), "Missing tasks");

        JsonNode source = root.get("source");
        assertTrue(source.has("repository"), "Missing source.repository");
        assertTrue(source.has("doclet"), "Missing source.doclet");
    }

    @Test
    void everyTaskHasRequiredFields(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Gzip.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Db2Db.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());

        for (JsonNode task : root.get("tasks")) {
            String taskName = task.get("name").asText();
            assertTrue(task.has("name"), taskName + ": missing name");
            assertTrue(task.has("qualifiedClassName"), taskName + ": missing qualifiedClassName");
            assertTrue(task.has("simpleClassName"), taskName + ": missing simpleClassName");
            assertTrue(task.has("status"), taskName + ": missing status");
            assertTrue(task.has("description"), taskName + ": missing description");
            assertTrue(task.has("properties"), taskName + ": missing properties");

            String status = task.get("status").asText();
            assertTrue(VALID_STATUSES.contains(status),
                    taskName + ": invalid status '" + status + "'");
        }
    }

    @Test
    void everyPropertyHasRequiredFields(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Gzip.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Db2Db.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());

        for (JsonNode task : root.get("tasks")) {
            String taskName = task.get("name").asText();
            assertTrue(task.get("properties").isArray(), taskName + ": properties must be an array");

            for (JsonNode prop : task.get("properties")) {
                String propName = prop.get("name").asText();
                assertTrue(prop.has("name"), taskName + "." + propName + ": missing name");
                assertTrue(prop.has("kind"), taskName + "." + propName + ": missing kind");
                assertTrue(prop.has("valueType"), taskName + "." + propName + ": missing valueType");
                assertTrue(prop.has("required"), taskName + "." + propName + ": missing required");
                assertTrue(prop.has("description"), taskName + "." + propName + ": missing description");
                assertTrue(prop.has("acceptedForms"), taskName + "." + propName + ": missing acceptedForms");

                String kind = prop.get("kind").asText();
                assertTrue(VALID_KINDS.contains(kind),
                        taskName + "." + propName + ": invalid kind '" + kind + "'");

                assertTrue(prop.get("acceptedForms").isArray(),
                        taskName + "." + propName + ": acceptedForms must be an array");

                for (JsonNode form : prop.get("acceptedForms")) {
                    assertTrue(form.has("style"), taskName + "." + propName + ": acceptedForm missing style");
                    String style = form.get("style").asText();
                    assertTrue(VALID_FORM_STYLES.contains(style),
                            taskName + "." + propName + ": invalid style '" + style + "'");
                    assertTrue(form.has("signature"), taskName + "." + propName + ": acceptedForm missing signature");
                    assertTrue(form.has("insertText"), taskName + "." + propName + ": acceptedForm missing insertText");
                    assertTrue(form.has("legacy"), taskName + "." + propName + ": acceptedForm missing legacy");
                }

                if (prop.has("file")) {
                    JsonNode file = prop.get("file");
                    assertTrue(file.has("role"), taskName + "." + propName + ".file: missing role");
                    String role = file.get("role").asText();
                    assertTrue(VALID_FILE_ROLES.contains(role),
                            taskName + "." + propName + ".file: invalid role '" + role + "'");
                    assertTrue(file.has("extensions"), taskName + "." + propName + ".file: missing extensions");
                    assertTrue(file.get("extensions").isArray(),
                            taskName + "." + propName + ".file: extensions must be an array");
                }
            }
        }
    }

    @Test
    void taskNamesAreUnique(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Gzip.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Db2Db.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/CsvExport.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/CsvImport.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Curl.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());

        Set<String> names = new java.util.HashSet<>();
        for (JsonNode task : root.get("tasks")) {
            String name = task.get("name").asText();
            assertTrue(names.add(name), "Duplicate task name: " + name);
        }
    }

    @Test
    void propertyNamesAreUniquePerTask(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Curl.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());

        for (JsonNode task : root.get("tasks")) {
            Set<String> propNames = new java.util.HashSet<>();
            for (JsonNode prop : task.get("properties")) {
                String name = prop.get("name").asText();
                assertTrue(propNames.add(name),
                        task.get("name").asText() + ": duplicate property " + name);
            }
        }
    }

    @Test
    void fileRoleExtensionsAreValid(@TempDir Path tempDir) throws Exception {
        Path adocOutput = tempDir.resolve("adoc");
        Path lspOutput = tempDir.resolve("lsp");
        Files.createDirectories(lspOutput);

        generate(adocOutput, lspOutput, List.of(
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/SqlExecutor.java"),
                Path.of("../gretl-core/src/main/java/ch/so/agi/gretl/tasks/Gzip.java")));

        Path metadataFile = lspOutput.resolve("gretl-lsp-metadata.json");
        JsonNode root = objectMapper.readTree(metadataFile.toFile());

        for (JsonNode task : root.get("tasks")) {
            for (JsonNode prop : task.get("properties")) {
                if (prop.has("file")) {
                    JsonNode file = prop.get("file");
                    String role = file.get("role").asText();
                    assertTrue(VALID_FILE_ROLES.contains(role),
                            "Invalid file role: " + role);

                    for (JsonNode ext : file.get("extensions")) {
                        assertTrue(ext.asText().startsWith("."),
                                "File extension should start with '.': " + ext.asText());
                    }
                }
            }
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
}
