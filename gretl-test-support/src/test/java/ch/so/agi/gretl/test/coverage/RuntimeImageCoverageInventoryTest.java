package ch.so.agi.gretl.test.coverage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageCoverageInventoryTest {
    private static final Pattern YAML_TASK = Pattern.compile("^  ([A-Za-z0-9]+):$");
    private static final Pattern YAML_NEXT_TASK = Pattern.compile("(?m)^  [A-Za-z0-9]+:");

    @Test
    void everyPublicTaskHasCoverageEntry() throws IOException {
        Path root = repositoryRoot();
        Set<String> covered = coverageTasks(root);
        Set<String> publicTasks = new HashSet<>();
        collectTaskNames(root.resolve("gretl-core/src/main/java/ch/so/agi/gretl/tasks"), publicTasks);
        collectTaskNames(root.resolve("gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks"), publicTasks);
        publicTasks.removeIf(name -> name.startsWith("Abstract") || name.endsWith("Task")
                || name.equals("GeoToolsTask") || name.equals("S3Task"));
        assertTrue(covered.containsAll(publicTasks),
                "Unclassified public tasks: " + difference(publicTasks, covered));
    }

    @Test
    void everyCoverageEntryHasValidClassificationAndScenario() throws IOException {
        Path root = repositoryRoot();
        String yaml = Files.readString(root.resolve("docs/testing/runtime-image-coverage.yaml"));
        Set<String> entries = coverageTasks(root);
        assertFalse(entries.isEmpty());
        for (String task : entries) {
            int start = yaml.indexOf("  " + task + ":");
            Matcher nextMatcher = YAML_NEXT_TASK.matcher(yaml);
            int next = nextMatcher.find(start + 3) ? nextMatcher.start() : -1;
            String block = next < 0 ? yaml.substring(start) : yaml.substring(start, next);
            assertTrue(block.contains("classification: DIRECT_E2E")
                            || block.contains("classification: COVERED_BY_CHAIN")
                            || block.contains("classification: NOT_APPLICABLE_WITH_REASON"),
                    "Invalid classification for " + task);
            assertTrue(block.contains("testClass:") && block.contains("testMethods:"),
                    "Missing scenario for " + task);
            if (block.contains("NOT_APPLICABLE_WITH_REASON")) {
                assertTrue(block.contains("reason:"), "Missing reason for " + task);
            }
        }
    }

    private Set<String> coverageTasks(Path root) throws IOException {
        Set<String> tasks = new HashSet<>();
        for (String line : Files.readAllLines(root.resolve("docs/testing/runtime-image-coverage.yaml"))) {
            Matcher matcher = YAML_TASK.matcher(line);
            if (matcher.matches()) {
                tasks.add(matcher.group(1));
            }
        }
        return tasks;
    }

    private void collectTaskNames(Path directory, Set<String> target) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.java$", ""))
                    .forEach(target::add);
        }
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private Path repositoryRoot() {
        Path current = Path.of(".").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate GRETL repository root");
    }
}
