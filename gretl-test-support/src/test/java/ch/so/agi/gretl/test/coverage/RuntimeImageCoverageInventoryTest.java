package ch.so.agi.gretl.test.coverage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageCoverageInventoryTest {
    @Test
    void everyPublicTaskHasCoverageEntry() throws IOException {
        Path root = repositoryRoot();
        TaskCoverageManifest manifest = new TaskCoverageManifestReader()
                .read(root.resolve("docs/testing/task-coverage.yaml"));
        Set<String> publicTasks = new HashSet<>();
        collectTaskNames(root.resolve("gretl-core/src/main/java/ch/so/agi/gretl/tasks"), publicTasks);
        collectTaskNames(root.resolve("gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks"), publicTasks);
        publicTasks.removeIf(name -> name.startsWith("Abstract") || name.endsWith("Task")
                || name.equals("GeoToolsTask") || name.equals("S3Task"));
        Set<String> covered = new HashSet<>(manifest.tasks().keySet());
        assertTrue(covered.containsAll(publicTasks),
                "Unclassified public tasks: " + difference(publicTasks, covered));
    }

    @Test
    void everyCoverageEntryUsesAnHonestClassification() throws IOException {
        Path root = repositoryRoot();
        TaskCoverageManifest manifest = new TaskCoverageManifestReader()
                .read(root.resolve("docs/testing/task-coverage.yaml"));
        assertFalse(manifest.tasks().isEmpty());
        Set<String> publicClasses = new HashSet<>();
        collectTaskNames(root.resolve("gretl-core/src/main/java/ch/so/agi/gretl/tasks"), publicClasses);
        collectTaskNames(root.resolve("gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks"), publicClasses);
        publicClasses.removeIf(name -> name.startsWith("Abstract") || name.endsWith("Task")
                || name.equals("GeoToolsTask") || name.equals("S3Task"));
        for (TaskCoverageEntry entry : manifest.entries()) {
            assertTrue(publicClasses.contains(entry.name()),
                    "Unknown task entry: " + entry.name());
            if (entry.classification() == TaskCoverageClassification.DIRECT_JOB_EXECUTION) {
                assertFalse(entry.scenarios().isEmpty(), "Direct entry has no scenario: " + entry.name());
            } else {
                assertTrue(entry.scenarios().isEmpty(), "Non-direct entry has a scenario: " + entry.name());
            }
            if (entry.classification() == TaskCoverageClassification.NO_CANONICAL_JOB_TRACE) {
                assertFalse(entry.reason().isBlank(), "Missing gap reason: " + entry.name());
            }
        }
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
