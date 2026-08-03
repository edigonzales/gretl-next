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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageBundledPluginResolutionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void appliesCorePluginWithVersionlessGroovyDsl() throws Exception {
        GradleTestProject project = project("core-groovy");
        project.settingsGroovy("rootProject.name = 'core-groovy'\n")
                .buildGroovy("""
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('writeMarker') {
                            doLast {
                                def output = file('build/marker.txt')
                                output.parentFile.mkdirs()
                                output.text = 'core'
                            }
                        }
                        """);
        GretlBuildResult result = run(project, "writeMarker");
        assertTrue(result.successful(), result.output());
        assertTrue(Files.exists(project.path("build/marker.txt")));
        assertNoDownload(result);
    }

    @Test
    void appliesCorePluginWithExplicitVersionGroovyDsl() throws Exception {
        String version = RuntimeImageDescriptor.fromSystemProperties().gretlVersion();
        GradleTestProject project = project("core-groovy-explicit");
        project.settingsGroovy("rootProject.name = 'core-groovy-explicit'\n")
                .buildGroovy("""
                        plugins { id 'ch.so.agi.gretl' version '%s' }
                        tasks.register('writeMarker') {
                            doLast {
                                def output = file('build/marker.txt')
                                output.parentFile.mkdirs()
                                output.text = 'core'
                            }
                        }
                        """.formatted(version));
        GretlBuildResult result = run(project, "writeMarker");
        assertTrue(result.successful(), result.output());
        assertNoDownload(result);
    }

    @Test
    void appliesGeotoolsPluginAndRunsWorkerCanary() throws Exception {
        GradleTestProject project = project("geotools-groovy");
        project.settingsGroovy("rootProject.name = 'geotools-groovy'\n")
                .buildGroovy("""
                        plugins {
                            id 'ch.so.agi.gretl'
                            id 'ch.so.agi.gretl.geotools'
                        }
                        tasks.register('workerCanary') {
                            doLast {
                                assert tasks.named('readShapefile') != null
                                def output = file('build/geotools.txt')
                                output.parentFile.mkdirs()
                                output.text = 'worker-runtime'
                            }
                        }
                        """);
        GretlBuildResult result = run(project, "workerCanary");
        assertTrue(result.successful(), result.output());
        assertTrue(Files.exists(project.path("build/geotools.txt")));
        assertNoDownload(result);
    }

    @Test
    void appliesGeotoolsPluginWithExplicitVersionGroovyDsl() throws Exception {
        String version = RuntimeImageDescriptor.fromSystemProperties().gretlVersion();
        GradleTestProject project = project("geotools-groovy-explicit");
        project.settingsGroovy("rootProject.name = 'geotools-groovy-explicit'\n")
                .buildGroovy("""
                        plugins {
                            id 'ch.so.agi.gretl' version '%s'
                            id 'ch.so.agi.gretl.geotools' version '%s'
                        }
                        tasks.register('workerCanary') {
                            doLast {
                                def output = file('build/geotools.txt')
                                output.parentFile.mkdirs()
                                output.text = 'worker-runtime'
                            }
                        }
                        """.formatted(version, version));
        GretlBuildResult result = run(project, "workerCanary");
        assertTrue(result.successful(), result.output());
        assertNoDownload(result);
    }

    @Test
    void failsWhenEmbeddedRepositoryIsExplicitlyRemoved() throws Exception {
        GradleTestProject project = project("missing-repository");
        project.settingsGroovy("rootProject.name = 'missing-repository'\n")
                .buildGroovy("plugins { id 'ch.so.agi.gretl' }\n");
        GretlBuildResult result = runAndFail(project, "-Dgretl.mavenRepo=/tmp/gretl-e2e-repository-does-not-exist", "tasks");
        assertFalse(result.successful());
        assertTrue(result.output().contains("GRETL plugin repository"), result.output());
        assertNoDownload(result);
    }

    @Test
    void rejectsDifferentBundledCoreVersion() throws Exception {
        GradleTestProject project = project("not-bundled-version");
        project.settingsGroovy("rootProject.name = 'not-bundled-version'\n")
                .buildGroovy("plugins { id 'ch.so.agi.gretl' version '999.0.0' }\n");

        GretlBuildResult result = runAndFail(project, "tasks");

        assertTrue(result.output().contains("Requested GRETL plugin version 999.0.0 is not bundled."), result.output());
        assertTrue(result.output().contains("Gradle dependency downloads are disabled."), result.output());
    }

    @Test
    void rejectsNonBundledGretlPlugin() throws Exception {
        GradleTestProject project = project("not-bundled-gretl-plugin");
        project.settingsGroovy("rootProject.name = 'not-bundled-gretl-plugin'\n")
                .buildGroovy("plugins { id 'ch.so.agi.gretl.notbundled' version '1.0.0' }\n");

        GretlBuildResult result = runAndFail(project, "tasks");

        assertTrue(result.output().contains("ch.so.agi.gretl.notbundled"), result.output());
        assertNoDownload(result);
    }

    @Test
    void rejectsNonBundledThirdPartyPlugin() throws Exception {
        GradleTestProject project = project("not-bundled-third-party");
        project.settingsGroovy("rootProject.name = 'not-bundled-third-party'\n")
                .buildGroovy("plugins { id 'com.example.not-bundled' version '1.0.0' }\n");

        GretlBuildResult result = runAndFail(project, "tasks");

        assertTrue(result.output().contains("com.example.not-bundled"), result.output());
        assertNoDownload(result);
    }

    private GradleTestProject project(String name) {
        return GradleTestProject.create(temporaryDirectory.resolve(name));
    }

    private GretlBuildResult run(GradleTestProject project, String... requestedArguments) {
        RuntimeImageDescriptor image = RuntimeImageDescriptor.fromSystemProperties();
        RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(image,
                new ch.so.agi.gretl.test.docker.DockerCli(),
                new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                new ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments());
        List<String> arguments = new ArrayList<>(List.of("--rerun-tasks"));
        arguments.addAll(Arrays.asList(requestedArguments));
        return executor.execute(GretlBuildRequest.builder(project.directory())
                .arguments(arguments)
                .timeout(Duration.ofMinutes(5))
                .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                .build());
    }

    private GretlBuildResult runAndFail(GradleTestProject project, String... requestedArguments) {
        RuntimeImageDescriptor image = RuntimeImageDescriptor.fromSystemProperties();
        RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(image,
                new ch.so.agi.gretl.test.docker.DockerCli(),
                new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                new ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments());
        List<String> arguments = new ArrayList<>(List.of("--rerun-tasks"));
        arguments.addAll(Arrays.asList(requestedArguments));
        return executor.executeAndExpectFailure(GretlBuildRequest.builder(project.directory())
                .arguments(arguments)
                .timeout(Duration.ofMinutes(5))
                .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                .build());
    }

    private void assertNoDownload(GretlBuildResult result) {
        String output = result.output();
        assertFalse(output.contains("Downloading"), output);
        assertFalse(output.contains("repo.maven.apache.org"), output);
        assertFalse(output.contains("plugins.gradle.org"), output);
        assertFalse(output.contains("services.gradle.org"), output);
    }
}
