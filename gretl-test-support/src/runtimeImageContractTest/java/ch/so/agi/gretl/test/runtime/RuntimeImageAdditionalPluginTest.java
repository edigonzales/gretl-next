package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments;
import ch.so.agi.gretl.test.fixture.FixturePluginRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageAdditionalPluginTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void combinesBundledGretlWithConsumerPluginRepository() throws Exception {
        Path repository = FixturePluginRepository.create(temporaryDirectory.resolve("fixture-plugin-repo"));
        Path project = Files.createDirectories(temporaryDirectory.resolve("consumer"));
        Files.writeString(project.resolve("settings.gradle"), """
                pluginManagement {
                    repositories { maven { url = uri('/fixture/plugin-repo') } }
                }
                rootProject.name = 'additional-plugin'
                """, StandardCharsets.UTF_8);
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'fixture.additional' version '1.0'
                }
                tasks.register('combined') { dependsOn 'fixturePluginTask'; doLast { file('build/gretl.txt').text = 'core' } }
                """, StandardCharsets.UTF_8);

        RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(
                RuntimeImageDescriptor.fromSystemProperties(), new DockerCli(), new ContainerUserResolver(),
                new RuntimeImageLifecycleArguments());
        RuntimeImageRunOptions options = RuntimeImageRunOptions.defaults()
                .withReadOnlyMount(repository, "/fixture/plugin-repo");
        GretlBuildResult result = executor.execute(GretlBuildRequest.builder(project)
                .arguments("--rerun-tasks", "combined")
                .timeout(Duration.ofMinutes(3))
                .runtimeImageOptions(options)
                .build());

        assertTrue(result.successful(), result.output());
        assertEquals("core", Files.readString(project.resolve("build/gretl.txt"), StandardCharsets.UTF_8));
        assertTrue(Files.exists(project.resolve("build/fixture-plugin.txt")));
    }
}
