package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageSqliteTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void executesMultipleSqlFilesInOrderOffline() throws Exception {
        GradleTestProject project = project("sqlite");
        project.settingsGroovy("rootProject.name = 'sqlite'\n")
                .textFile("schema.sql", "CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);\n")
                .textFile("data.sql", "INSERT INTO colors VALUES (1, 'red');\nINSERT INTO colors VALUES (2, 'blue');\n")
                .buildGroovy("""
                        import ch.so.agi.gretl.tasks.SqlExecutor
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('initDb', SqlExecutor) {
                            database "jdbc:sqlite:${projectDir}/colors.db"
                            sqlFiles 'schema.sql', 'data.sql'
                        }
                        """);

        GretlBuildResult result = executor().execute(request(project, "initDb"));
        assertTrue(result.successful(), result.output());
        assertTrue(Files.size(project.path("colors.db")) > 0);
        assertTrue(result.output().contains("SqlExecutor"), result.output());
    }

    @Test
    void transactionRollbackFailsWithoutPartialResultOffline() throws Exception {
        GradleTestProject project = project("sqlite-rollback");
        project.settingsGroovy("rootProject.name = 'sqlite-rollback'\n")
                .textFile("schema.sql", "CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);\n")
                .textFile("bad.sql", "INSERT INTO colors VALUES (1, 'red');\nINSERT INTO missing VALUES (1);\n")
                .buildGroovy("""
                        import ch.so.agi.gretl.tasks.SqlExecutor
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('initDb', SqlExecutor) {
                            database "jdbc:sqlite:${projectDir}/colors.db"
                            sqlFiles 'schema.sql'
                        }
                        tasks.register('fail', SqlExecutor) {
                            dependsOn 'initDb'
                            database "jdbc:sqlite:${projectDir}/colors.db"
                            sqlFiles 'bad.sql'
                        }
                        """);

        GretlBuildResult result = executor().executeAndExpectFailure(request(project, "fail"));
        assertTrue(result.output().contains("missing"), result.output());
    }

    private GradleTestProject project(String name) {
        return GradleTestProject.create(temporaryDirectory.resolve(name));
    }

    private GretlBuildRequest request(GradleTestProject project, String task) {
        return GretlBuildRequest.builder(project.directory())
                .arguments(List.of("--rerun-tasks", task))
                .timeout(Duration.ofMinutes(3))
                .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                .build();
    }

    private RuntimeImageBuildExecutor executor() {
        return new RuntimeImageBuildExecutor(RuntimeImageDescriptor.fromSystemProperties(),
                new ch.so.agi.gretl.test.docker.DockerCli(),
                new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                new ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments());
    }
}
