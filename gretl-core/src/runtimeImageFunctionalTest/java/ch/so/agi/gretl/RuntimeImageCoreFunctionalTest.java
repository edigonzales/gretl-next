package ch.so.agi.gretl;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.execution.RuntimeImageGradleArguments;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("runtime-image")
@Tag("runtime-image-smoke")
class RuntimeImageCoreFunctionalTest {
    @TempDir
    Path project;

    @Test
    void runsGzipAndSqliteCanariesFromMountedProject() throws Exception {
        writeSettings();
        Files.createDirectories(project.resolve("input"));
        Files.writeString(project.resolve("input/data.txt"), "GRETL runtime image", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("schema.sql"), """
                CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);
                INSERT INTO colors VALUES (1, 'red');
                """, StandardCharsets.UTF_8);
        Files.createDirectories(project.resolve("build"));
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.tasks.SqlExecutor
                tasks.register('compressFile', Gzip) {
                    dataFile 'input/data.txt'
                    gzipFile layout.buildDirectory.file('data.txt.gz')
                }
                tasks.register('initDb', SqlExecutor) {
                    database "jdbc:sqlite:${file('build/runtime.db').absolutePath}"
                    sqlFiles 'schema.sql'
                }
                tasks.register('coreCanary') { dependsOn 'compressFile', 'initDb' }
                """, StandardCharsets.UTF_8);

        GretlBuildResult result = executor().execute(request("coreCanary"));
        assertTrue(result.successful(), result.output());
        assertTrue(Files.exists(project.resolve("build/data.txt.gz")));
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + project.resolve("build/runtime.db"));
             var statement = connection.createStatement();
             var rows = statement.executeQuery("select name from colors")) {
            assertTrue(rows.next());
            assertEquals("red", rows.getString(1));
        }
    }

    @Test
    void loadsDuckDbSpatialExtension() throws Exception {
        writeSettings();
        Files.writeString(project.resolve("spatial.sql"), """
                LOAD spatial;
                CREATE TABLE points AS SELECT ST_Point(2600000, 1200000) AS geom;
                """, StandardCharsets.UTF_8);
        Files.createDirectories(project.resolve("build"));
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                import ch.so.agi.gretl.tasks.SqlExecutor
                tasks.register('spatialCanary', SqlExecutor) {
                    database "jdbc:duckdb:${file('build/spatial.duckdb').absolutePath}"
                    sqlFiles 'spatial.sql'
                }
                """, StandardCharsets.UTF_8);

        GretlBuildResult result = executor().execute(request("spatialCanary"));
        assertTrue(result.successful(), result.output());
        assertTrue(Files.exists(project.resolve("build/spatial.duckdb")));
    }

    private RuntimeImageBuildExecutor executor() {
        return new RuntimeImageBuildExecutor(RuntimeImageDescriptor.fromSystemProperties(), new DockerCli(),
                new ContainerUserResolver(), new RuntimeImageGradleArguments());
    }

    private GretlBuildRequest request(String task) {
        return GretlBuildRequest.builder(project)
                .arguments("--rerun-tasks", task)
                .timeout(Duration.ofMinutes(5))
                .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                .build();
    }

    private void writeSettings() throws Exception {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-core'\n");
    }
}
