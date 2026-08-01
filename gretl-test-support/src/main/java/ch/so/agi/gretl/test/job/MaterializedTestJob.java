package ch.so.agi.gretl.test.job;

import java.nio.file.Path;
import java.util.Objects;

public record MaterializedTestJob(
        TestJobDescriptor descriptor,
        TestJobBuildVariant buildVariant,
        TestJobExecutionTarget target,
        Path projectDirectory,
        Path buildFile,
        Path settingsFile,
        Path traceFile) {
    public MaterializedTestJob {
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(buildVariant);
        Objects.requireNonNull(target);
        projectDirectory = projectDirectory.toAbsolutePath().normalize();
        buildFile = buildFile.toAbsolutePath().normalize();
        settingsFile = settingsFile.toAbsolutePath().normalize();
        traceFile = traceFile.toAbsolutePath().normalize();
    }

    public Path resolve(String relativePath) {
        Path result = projectDirectory.resolve(relativePath).normalize();
        if (!result.startsWith(projectDirectory)) throw new IllegalArgumentException("Job path escapes materialized project: " + relativePath);
        return result;
    }

    public Path traceBootstrapFile() {
        return projectDirectory.resolve(".gretl-test/task-trace.init.gradle");
    }
}
