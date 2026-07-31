package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageOfflineExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageOfflineDuckDbTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsBundledSpatialExtensionWithoutInstallOrNetwork() throws Exception {
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("duckdb"));
        project.settingsGroovy("rootProject.name = 'duckdb'\n")
                .textFile("spatial.sql", "LOAD spatial; LOAD postgres; LOAD excel; "
                        + "SELECT ST_AsText(ST_Point(2600000, 1200000));\n")
                .buildGroovy("""
                        import ch.so.agi.gretl.tasks.SqlExecutor
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('spatial', SqlExecutor) {
                            database "jdbc:duckdb:${projectDir}/spatial.duckdb"
                            sqlFiles 'spatial.sql'
                        }
                        """);

        GretlBuildResult result = GretlBuildRequestRunner.run(project, "spatial", Duration.ofMinutes(4));
        assertTrue(result.successful(), result.output());
        assertTrue(Files.exists(project.path("spatial.duckdb")), result.output());
        assertTrue(result.output().contains("POINT"), result.output());
    }

    private static final class GretlBuildRequestRunner {
        private static GretlBuildResult run(GradleTestProject project, String task, Duration timeout) {
            return new RuntimeImageOfflineExecutor(RuntimeImageDescriptor.fromSystemProperties()).execute(
                    GretlBuildRequest.builder(project.directory())
                            .arguments(List.of("--rerun-tasks", task))
                            .timeout(timeout)
                            .runtimeImageOptions(RuntimeImageRunOptions.offline())
                            .build());
        }
    }
}
