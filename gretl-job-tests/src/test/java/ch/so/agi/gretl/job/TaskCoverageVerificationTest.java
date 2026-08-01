package ch.so.agi.gretl.job;

import ch.so.agi.gretl.test.coverage.CoverageVerificationReport;
import ch.so.agi.gretl.test.coverage.TaskCoverageManifest;
import ch.so.agi.gretl.test.coverage.TaskCoverageManifestReader;
import ch.so.agi.gretl.test.coverage.TaskCoverageReportWriter;
import ch.so.agi.gretl.test.coverage.TaskCoverageVerifier;
import ch.so.agi.gretl.test.job.FileSystemTestJobCatalog;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;
import ch.so.agi.gretl.test.trace.TaskExecutionTraceReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCoverageVerificationTest {
    @Test
    void verifiesPublicTasksAgainstCanonicalJobTraces() throws IOException {
        Path repositoryRoot = requiredPath("gretl.test.repositoryRoot");
        TaskCoverageManifest manifest = new TaskCoverageManifestReader()
                .read(repositoryRoot.resolve("docs/testing/task-coverage.yaml"));
        FileSystemTestJobCatalog catalog = FileSystemTestJobCatalog.load(requiredPath("gretl.test.jobsRoot"));
        List<TaskExecutionTrace> traces = readTraces(requiredPath("gretl.test.coverageTraceRoot"));
        Set<String> publicTaskClasses = publicTaskClasses(repositoryRoot);

        CoverageVerificationReport report = new TaskCoverageVerifier().verify(
                manifest, catalog, traces, publicTaskClasses);
        Path reportDirectory = requiredPath("gretl.test.coverageReportDir");
        new TaskCoverageReportWriter().write(report,
                reportDirectory.resolve("coverage.json"), reportDirectory.resolve("coverage.adoc"));
        assertTrue(report.successful(), () -> "Coverage verification failed: " + report.errors());
    }

    private List<TaskExecutionTrace> readTraces(Path root) throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        TaskExecutionTraceReader reader = new TaskExecutionTraceReader();
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .sorted()
                    .map(reader::read)
                    .toList();
        }
    }

    private Set<String> publicTaskClasses(Path root) throws IOException {
        Set<String> classes = new HashSet<>();
        collect(root.resolve("gretl-core/src/main/java/ch/so/agi/gretl/tasks"),
                "ch.so.agi.gretl.tasks", classes);
        collect(root.resolve("gretl-geotools/src/main/java/ch/so/agi/gretl/geotools/tasks"),
                "ch.so.agi.gretl.geotools.tasks", classes);
        classes.removeIf(name -> name.endsWith(".GeoToolsTask") || name.endsWith(".S3Task")
                || name.substring(name.lastIndexOf('.') + 1).startsWith("Abstract")
                || name.endsWith("Task"));
        return Set.copyOf(classes);
    }

    private void collect(Path directory, String packageName, Set<String> target) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (var files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> packageName + "." + path.getFileName().toString().replaceFirst("\\.java$", ""))
                    .forEach(target::add);
        }
    }

    private Path requiredPath(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + property);
        return Path.of(value).toAbsolutePath().normalize();
    }
}
