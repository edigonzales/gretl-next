package ch.so.agi.gretl.control.worker.execution;

import ch.so.agi.gretl.control.api.ClaimedRun;
import ch.so.agi.gretl.control.worker.WorkerProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GretlCommandFactoryTest {
    @Test
    void buildsGradleCommandWithParametersSecretsAndJvmOptions() {
        WorkerProperties properties = new WorkerProperties();
        properties.setWorkspaceRoot(Path.of("/workspace"));
        properties.setGretlExecutable("/usr/local/bin/gretl");
        GretlCommandFactory factory = new GretlCommandFactory(properties);

        GretlCommand command = factory.create(new ClaimedRun(
                "run-1",
                "job-1",
                "jobs/colors",
                List.of("importData"),
                Map.of("limit", 50),
                Map.of("db-password", "secret"),
                "1g",
                List.of("-Dfeature=true"),
                60));

        assertEquals(List.of("/usr/local/bin/gretl", "importData", "-Plimit=50"), command.command());
        assertEquals(Path.of("/workspace/jobs/colors").toAbsolutePath().normalize(), command.workingDirectory());
        assertEquals("-Xmx1g -Dfeature=true", command.environment().get("GRADLE_OPTS"));
        assertEquals("50", command.environment().get("GRETL_PARAM_LIMIT"));
        assertEquals("secret", command.environment().get("GRETL_SECRET_DB_PASSWORD"));
    }
}
