package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageSecretTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void masksSyntheticSecretFromOutputAndCommand() {
        String secret = "RUNTIME_SECRET_42";
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("secret"));
        project.settingsGroovy("rootProject.name = 'secret'\n")
                .buildGroovy("""
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('secretCanary') {
                            doLast { println "secret=${project.findProperty('canarySecret')}" }
                        }
                        """);

        GretlBuildResult result = new RuntimeImageBuildExecutor(RuntimeImageDescriptor.fromSystemProperties(),
                new ch.so.agi.gretl.test.docker.DockerCli(),
                new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                new ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments()).execute(
                GretlBuildRequest.builder(project.directory())
                        .arguments(List.of("-PcanarySecret=" + secret, "--rerun-tasks", "secretCanary"))
                        .secret(secret)
                        .timeout(Duration.ofMinutes(2))
                        .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                        .build());

        assertTrue(result.successful(), result.output());
        assertFalse(result.output().contains(secret), "secret leaked in output");
        assertFalse(String.join(" ", result.sanitizedCommand()).contains(secret), "secret leaked in command");
    }
}
