package ch.so.agi.gretl.test.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ch.so.agi.gretl.test.job.TestJobExecutionRequirement;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TaskCoverageManifestReader {
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "tasks");
    private static final Set<String> TASK_FIELDS = Set.of("className", "module", "classification", "reason", "scenarios");
    private static final Set<String> SCENARIO_FIELDS = Set.of("job", "taskPath", "targets");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public TaskCoverageManifest read(Path manifestFile) {
        Path file = manifestFile.toAbsolutePath().normalize();
        try {
            JsonNode root = YAML.readTree(Files.newBufferedReader(file));
            requireObject(root, file, "root");
            rejectUnknown(root, ROOT_FIELDS, file, "root");
            if (integer(root, "schemaVersion", file, "schemaVersion") != 4) {
                throw error(file, "schemaVersion", "must be 4");
            }
            JsonNode tasksNode = required(root, "tasks", file, "tasks");
            requireObject(tasksNode, file, "tasks");
            Map<String, TaskCoverageEntry> entries = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = tasksNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String name = field.getKey();
                JsonNode node = field.getValue();
                requireObject(node, file, "tasks." + name);
                rejectUnknown(node, TASK_FIELDS, file, "tasks." + name);
                String className = text(node, "className", file, "tasks." + name);
                String module = text(node, "module", file, "tasks." + name);
                TaskCoverageClassification classification = TaskCoverageClassification.fromYaml(
                        text(node, "classification", file, "tasks." + name));
                String reason = node.has("reason") ? text(node, "reason", file, "tasks." + name) : "";
                List<TaskCoverageScenario> scenarios = scenarios(node, file, name);
                if (entries.put(name, new TaskCoverageEntry(name, className, module, classification, reason, scenarios)) != null) {
                    throw error(file, "tasks." + name, "is duplicated");
                }
            }
            return new TaskCoverageManifest(4, entries);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read task coverage manifest '" + file + "'.", e);
        }
    }

    private List<TaskCoverageScenario> scenarios(JsonNode task, Path file, String name) {
        if (!task.has("scenarios")) return List.of();
        JsonNode value = task.get("scenarios");
        if (!value.isArray()) throw error(file, "tasks." + name + ".scenarios", "must be an array");
        List<TaskCoverageScenario> scenarios = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            JsonNode scenario = value.get(i);
            String field = "tasks." + name + ".scenarios[" + i + "]";
            requireObject(scenario, file, field);
            rejectUnknown(scenario, SCENARIO_FIELDS, file, field);
            JsonNode targetsNode = required(scenario, "targets", file, field + ".targets");
            requireObject(targetsNode, file, field + ".targets");
            Map<TestJobExecutionTarget, TestJobExecutionRequirement> targets =
                    new EnumMap<>(TestJobExecutionTarget.class);
            Iterator<Map.Entry<String, JsonNode>> targetsFields = targetsNode.fields();
            while (targetsFields.hasNext()) {
                Map.Entry<String, JsonNode> target = targetsFields.next();
                try {
                    targets.put(TestJobExecutionTarget.fromYaml(target.getKey()),
                            TestJobExecutionRequirement.fromYaml(textValue(target.getValue(), file,
                                    field + ".targets." + target.getKey())));
                } catch (IllegalArgumentException e) {
                    throw error(file, field + ".targets." + target.getKey(), e.getMessage());
                }
            }
            scenarios.add(new TaskCoverageScenario(
                    text(scenario, "job", file, field),
                    text(scenario, "taskPath", file, field), targets));
        }
        return scenarios;
    }

    private static void rejectUnknown(JsonNode node, Set<String> allowed, Path file, String field) {
        node.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) throw error(file, field + "." + name, "unknown field");
        });
    }

    private static JsonNode required(JsonNode node, String field, Path file, String path) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) throw error(file, path, "is required");
        return value;
    }

    private static String text(JsonNode node, String field, Path file, String path) {
        return textValue(required(node, field, file, path + "." + field), file, path + "." + field);
    }

    private static String textValue(JsonNode value, Path file, String path) {
        if (!value.isTextual() || value.asText().isBlank()) throw error(file, path, "must be a non-empty string");
        return value.asText();
    }

    private static int integer(JsonNode node, String field, Path file, String path) {
        JsonNode value = required(node, field, file, path);
        if (!value.canConvertToInt()) throw error(file, path, "must be an integer");
        return value.intValue();
    }

    private static void requireObject(JsonNode node, Path file, String path) {
        if (node == null || !node.isObject()) throw error(file, path, "must be an object");
    }

    private static IllegalArgumentException error(Path file, String path, String message) {
        return new IllegalArgumentException("Invalid task coverage manifest: file: " + file
                + ", field: " + path + ", " + message);
    }
}
