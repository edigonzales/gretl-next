package ch.so.agi.gretl.test.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ch.so.agi.gretl.test.fixture.TestFixtureType;
import ch.so.agi.gretl.test.fixture.TestJobBindingTarget;
import ch.so.agi.gretl.test.fixture.TestJobFixtureBinding;
import ch.so.agi.gretl.test.fixture.TestJobFixtureRequirement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads the small, data-only job descriptor using Jackson YAML. */
public final class TestJobYamlReader {
    private static final Set<String> TOP_LEVEL_FIELDS = Set.of(
            "schemaVersion", "id", "description", "category", "builds", "entryTasks",
            "expectedTasks", "executionTargets", "capabilities", "fixtures", "assertions", "timeoutSeconds");
    private static final Set<String> BUILD_FIELDS = Set.of("id", "file", "language", "executionTargets");
    private static final Set<String> DECLARATION_FIELDS = Set.of("requirement", "reason");
    private static final Set<String> FIXTURE_FIELDS = Set.of("id", "type", "bindings");
    private static final Set<String> BINDING_FIELDS = Set.of("source", "target", "name");
    private static final Set<String> EXPECTED_TASK_FIELDS = Set.of("path", "className");
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public TestJobDescriptor read(Path yamlFile) {
        Path file = yamlFile.toAbsolutePath().normalize();
        try {
            JsonNode root = MAPPER.readTree(Files.newBufferedReader(file));
            if (root == null || !root.isObject()) {
                throw error(file, "descriptor", "must be a YAML object");
            }
            rejectUnknown(root, TOP_LEVEL_FIELDS, file, "top-level");
            int schemaVersion = integer(root, "schemaVersion", file);
            if (schemaVersion == 1) {
                throw error(file, "schemaVersion", "Schema version 1 is no longer supported.");
            }
            if (schemaVersion != 2) throw error(file, "schemaVersion", "must be 2");

            List<TestJobBuildVariant> builds = new ArrayList<>();
            JsonNode buildsNode = required(root, "builds", file);
            if (!buildsNode.isArray()) throw error(file, "builds", "must be an array");
            for (int i = 0; i < buildsNode.size(); i++) {
                JsonNode node = buildsNode.get(i);
                rejectUnknown(node, BUILD_FIELDS, file, "builds[" + i + "]");
                String languageValue = text(node, "language", file, "builds[" + i + "]");
                TestJobBuildLanguage language;
                try {
                    language = TestJobBuildLanguage.valueOf(languageValue.toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw error(file, "builds[" + i + "].language", "must be GROOVY or KOTLIN");
                }
                builds.add(new TestJobBuildVariant(
                        text(node, "id", file, "builds[" + i + "]"),
                        text(node, "file", file, "builds[" + i + "]"), language,
                        declarations(node, "executionTargets", file, "builds[" + i + "]")));
            }

            List<String> entryTasks = strings(root, "entryTasks", file);
            List<ExpectedTaskExecution> expectedTasks = new ArrayList<>();
            JsonNode expectedNode = required(root, "expectedTasks", file);
            if (!expectedNode.isArray()) throw error(file, "expectedTasks", "must be an array");
            for (int i = 0; i < expectedNode.size(); i++) {
                JsonNode node = expectedNode.get(i);
                rejectUnknown(node, EXPECTED_TASK_FIELDS, file, "expectedTasks[" + i + "]");
                expectedTasks.add(new ExpectedTaskExecution(
                        text(node, "path", file, "expectedTasks[" + i + "]"),
                        text(node, "className", file, "expectedTasks[" + i + "]")));
            }

            Map<TestJobExecutionTarget, TestJobExecutionRequirement> targets =
                    new EnumMap<>(TestJobExecutionTarget.class);
            JsonNode targetNode = required(root, "executionTargets", file);
            if (!targetNode.isObject()) throw error(file, "executionTargets", "must be an object");
            Iterator<Map.Entry<String, JsonNode>> targetFields = targetNode.fields();
            while (targetFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = targetFields.next();
                try {
                    targets.put(TestJobExecutionTarget.fromYaml(entry.getKey()),
                            TestJobExecutionRequirement.fromYaml(textValue(entry.getValue(), file,
                                    "executionTargets." + entry.getKey())));
                } catch (IllegalArgumentException e) {
                    throw error(file, "executionTargets." + entry.getKey(), e.getMessage());
                }
            }

            List<TestJobFixtureRequirement> fixtures = fixtures(root, file);

            int timeoutSeconds = root.has("timeoutSeconds") ? integer(root, "timeoutSeconds", file) : 300;
            return new TestJobDescriptor(
                    schemaVersion,
                    text(root, "id", file, "id"),
                    text(root, "description", file, "description"),
                    text(root, "category", file, "category"),
                    builds,
                    entryTasks,
                    expectedTasks,
                    targets,
                    new LinkedHashSet<>(strings(root, "capabilities", file)),
                    fixtures,
                    text(root, "assertions", file, "assertions"),
                    Duration.ofSeconds(timeoutSeconds),
                    file.getParent());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read test job descriptor '" + file + "'.", e);
        }
    }

    private List<TestJobFixtureRequirement> fixtures(JsonNode root, Path file) {
        JsonNode node = required(root, "fixtures", file);
        if (!node.isArray()) throw error(file, "fixtures", "must be an array");
        List<TestJobFixtureRequirement> values = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode fixture = node.get(i);
            String prefix = "fixtures[" + i + "]";
            rejectUnknown(fixture, FIXTURE_FIELDS, file, prefix);
            JsonNode bindings = required(fixture, "bindings", file);
            if (!bindings.isArray()) throw error(file, prefix + ".bindings", "must be an array");
            List<TestJobFixtureBinding> bindingValues = new ArrayList<>();
            Set<String> names = new HashSet<>();
            for (int j = 0; j < bindings.size(); j++) {
                JsonNode binding = bindings.get(j);
                String bindingPrefix = prefix + ".bindings[" + j + "]";
                rejectUnknown(binding, BINDING_FIELDS, file, bindingPrefix);
                String name = text(binding, "name", file, bindingPrefix);
                if (!names.add(name)) throw error(file, bindingPrefix + ".name", "must be unique within fixture");
                TestJobBindingTarget target;
                try { target = TestJobBindingTarget.fromYaml(text(binding, "target", file, bindingPrefix)); }
                catch (IllegalArgumentException e) { throw error(file, bindingPrefix + ".target", e.getMessage()); }
                bindingValues.add(new TestJobFixtureBinding(text(binding, "source", file, bindingPrefix), target, name));
            }
            TestFixtureType type;
            try { type = TestFixtureType.fromYaml(text(fixture, "type", file, prefix)); }
            catch (IllegalArgumentException e) { throw error(file, prefix + ".type", e.getMessage()); }
            values.add(new TestJobFixtureRequirement(text(fixture, "id", file, prefix), type, bindingValues));
        }
        return List.copyOf(values);
    }

    private Map<TestJobExecutionTarget, TestJobExecutionDeclaration> declarations(
            JsonNode node, String field, Path file, String prefix) {
        Map<TestJobExecutionTarget, TestJobExecutionDeclaration> result = new EnumMap<>(TestJobExecutionTarget.class);
        if (!node.has(field)) return Map.of();
        JsonNode targets = node.get(field);
        if (!targets.isObject()) throw error(file, prefix + "." + field, "must be an object");
        Iterator<Map.Entry<String, JsonNode>> fields = targets.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            TestJobExecutionTarget target;
            try { target = TestJobExecutionTarget.fromYaml(entry.getKey()); }
            catch (IllegalArgumentException e) { throw error(file, prefix + "." + field + "." + entry.getKey(), e.getMessage()); }
            JsonNode value = entry.getValue();
            TestJobExecutionRequirement requirement;
            String reason = null;
            if (value.isTextual()) {
                requirement = TestJobExecutionRequirement.fromYaml(value.asText());
            } else {
                rejectUnknown(value, DECLARATION_FIELDS, file, prefix + "." + field + "." + entry.getKey());
                requirement = TestJobExecutionRequirement.fromYaml(text(value, "requirement", file,
                        prefix + "." + field + "." + entry.getKey()));
                if (value.has("reason")) reason = text(value, "reason", file,
                        prefix + "." + field + "." + entry.getKey());
            }
            try {
                result.put(target, new TestJobExecutionDeclaration(requirement,
                        java.util.Optional.ofNullable(reason)));
            } catch (IllegalArgumentException e) {
                throw error(file, prefix + "." + field + "." + entry.getKey(), e.getMessage());
            }
        }
        return Map.copyOf(result);
    }

    private static void rejectUnknown(JsonNode node, Set<String> allowed, Path file, String field) {
        if (node == null || !node.isObject()) throw error(file, field, "must be an object");
        node.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) throw error(file, field + "." + name, "unknown field");
        });
    }

    private static JsonNode required(JsonNode node, String field, Path file) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) throw error(file, field, "is required");
        return value;
    }

    private static String text(JsonNode node, String field, Path file, String prefix) {
        return textValue(required(node, field, file), file, prefix + "." + field);
    }

    private static String textValue(JsonNode value, Path file, String field) {
        if (!value.isTextual() || value.asText().isBlank()) throw error(file, field, "must be a non-empty string");
        return value.asText();
    }

    private static int integer(JsonNode node, String field, Path file) {
        JsonNode value = required(node, field, file);
        if (!value.canConvertToInt()) throw error(file, field, "must be an integer");
        return value.intValue();
    }

    private static List<String> strings(JsonNode root, String field, Path file) {
        JsonNode node = required(root, field, file);
        if (!node.isArray()) throw error(file, field, "must be an array");
        List<String> values = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            values.add(textValue(node.get(i), file, field + "[" + i + "]"));
        }
        return values;
    }

    private static IllegalArgumentException error(Path file, String field, String message) {
        return new IllegalArgumentException("Invalid test job descriptor: file: " + file
                + ", field: " + field + ", " + message);
    }
}
