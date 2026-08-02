package ch.so.agi.gretl.test.job;

import java.nio.file.Path;
import java.util.Objects;

public record MaterializedTestJob(
        TestJobDescriptor descriptor,
        TestJobBuildVariant buildVariant,
        TestJobExecutionTarget target,
        TestJobExecutionId executionId,
        Path projectDirectory,
        Path buildFile,
        Path settingsFile,
        Path traceFile,
        Path expectedDirectory) {
    public MaterializedTestJob {
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(buildVariant);
        Objects.requireNonNull(target);
        Objects.requireNonNull(executionId);
        projectDirectory = projectDirectory.toAbsolutePath().normalize();
        buildFile = buildFile.toAbsolutePath().normalize();
        settingsFile = settingsFile.toAbsolutePath().normalize();
        traceFile = traceFile.toAbsolutePath().normalize();
        expectedDirectory = expectedDirectory.toAbsolutePath().normalize();
        if (expectedDirectory.startsWith(projectDirectory)) {
            throw new IllegalArgumentException("Expected directory must stay outside the consumer project");
        }
    }

    public MaterializedTestJob(TestJobDescriptor descriptor,
                               TestJobBuildVariant buildVariant,
                               TestJobExecutionTarget target,
                               Path projectDirectory,
                               Path buildFile,
                               Path settingsFile,
                               Path traceFile) {
        this(descriptor, buildVariant, target,
                TestJobExecutionId.create(descriptor, buildVariant, target),
                projectDirectory, buildFile, settingsFile, traceFile,
                descriptor.sourceDirectory().resolve("expected"));
    }

    public Path resolve(String relativePath) {
        Path result = projectDirectory.resolve(relativePath).normalize();
        if (!result.startsWith(projectDirectory)) throw new IllegalArgumentException("Job path escapes materialized project: " + relativePath);
        return result;
    }

    public Path traceBootstrapFile() {
        return projectDirectory.resolve(".gretl-test/task-trace.init.gradle");
    }

    public Path expectedDirectory() {
        return expectedDirectory;
    }

    public Path resolveExpected(String relativePath) {
        Path result = expectedDirectory.resolve(relativePath).normalize();
        if (!result.startsWith(expectedDirectory)) {
            throw new IllegalArgumentException("Expected path escapes expected directory: " + relativePath);
        }
        return result;
    }
}
