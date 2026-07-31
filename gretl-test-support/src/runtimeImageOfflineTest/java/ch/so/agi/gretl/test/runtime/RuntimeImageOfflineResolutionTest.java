package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageOfflineExecutor;
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

class RuntimeImageOfflineResolutionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void appliesCorePluginWithVersionlessGroovyDslOffline() throws Exception {
        GradleTestProject project = project("core-groovy");
        project.settingsGroovy("rootProject.name = 'core-groovy'\n")
                .buildGroovy("""
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('writeOffline') {
                            doLast {
                                def output = file('build/offline.txt')
                                output.parentFile.mkdirs()
                                output.text = 'core'
                            }
                        }
                        """);
        GretlBuildResult result = run(project, "writeOffline");
        assertTrue(result.successful(), result.output());
        assertTrue(Files.exists(project.path("build/offline.txt")));
        assertNoDownload(result);
    }

    @Test
    void appliesCorePluginWithKotlinDslOffline() throws Exception {
        GradleTestProject project = project("core-kotlin");
        project.settingsKotlin("rootProject.name = \"core-kotlin\"\n")
                .buildKotlin("""
                        plugins { id("ch.so.agi.gretl") }
                        tasks.register("writeOffline") {
                            doLast {
                                val output = file("build/offline.txt")
                                output.parentFile.mkdirs()
                                output.writeText("kotlin")
                            }
                        }
                        """);
        GretlBuildResult result = run(project, "writeOffline");
        assertTrue(result.successful(), result.output());
        assertTrue(Files.exists(project.path("build/offline.txt")));
        assertNoDownload(result);
    }

    @Test
    void appliesCorePluginWithExplicitVersionGroovyDslOffline() throws Exception {
        String version = RuntimeImageDescriptor.fromSystemProperties().gretlVersion();
        GradleTestProject project = project("core-groovy-explicit");
        project.settingsGroovy("rootProject.name = 'core-groovy-explicit'\n")
                .buildGroovy("""
                        plugins { id 'ch.so.agi.gretl' version '%s' }
                        tasks.register('writeOffline') {
                            doLast {
                                def output = file('build/offline.txt')
                                output.parentFile.mkdirs()
                                output.text = 'core'
                            }
                        }
                        """.formatted(version));
        GretlBuildResult result = run(project, "writeOffline");
        assertTrue(result.successful(), result.output());
        assertNoDownload(result);
    }

    @Test
    void appliesCorePluginWithExplicitVersionKotlinDslOffline() throws Exception {
        String version = RuntimeImageDescriptor.fromSystemProperties().gretlVersion();
        GradleTestProject project = project("core-kotlin-explicit");
        project.settingsKotlin("rootProject.name = \"core-kotlin-explicit\"\n")
                .buildKotlin("""
                        plugins { id("ch.so.agi.gretl") version "%s" }
                        tasks.register("writeOffline") {
                            doLast {
                                val output = file("build/offline.txt")
                                output.parentFile.mkdirs()
                                output.writeText("kotlin")
                            }
                        }
                        """.formatted(version));
        GretlBuildResult result = run(project, "writeOffline");
        assertTrue(result.successful(), result.output());
        assertNoDownload(result);
    }

    @Test
    void appliesGeotoolsPluginAndRunsWorkerCanaryOffline() throws Exception {
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
    void appliesGeotoolsPluginWithExplicitVersionGroovyDslOffline() throws Exception {
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
    void appliesGeotoolsPluginWithVersionlessKotlinDslOffline() throws Exception {
        GradleTestProject project = project("geotools-kotlin");
        project.settingsKotlin("rootProject.name = \"geotools-kotlin\"\n")
                .buildKotlin("""
                        plugins {
                            id("ch.so.agi.gretl")
                            id("ch.so.agi.gretl.geotools")
                        }
                        tasks.register("workerCanary") {
                            doLast {
                                val output = file("build/geotools.txt")
                                output.parentFile.mkdirs()
                                output.writeText("worker-runtime")
                            }
                        }
                        """);
        GretlBuildResult result = run(project, "workerCanary");
        assertTrue(result.successful(), result.output());
        assertNoDownload(result);
    }

    @Test
    void appliesGeotoolsPluginWithExplicitVersionKotlinDslOffline() throws Exception {
        String version = RuntimeImageDescriptor.fromSystemProperties().gretlVersion();
        GradleTestProject project = project("geotools-kotlin-explicit");
        project.settingsKotlin("rootProject.name = \"geotools-kotlin-explicit\"\n")
                .buildKotlin("""
                        plugins {
                            id("ch.so.agi.gretl") version "%s"
                            id("ch.so.agi.gretl.geotools") version "%s"
                        }
                        tasks.register("workerCanary") {
                            doLast {
                                val output = file("build/geotools.txt")
                                output.parentFile.mkdirs()
                                output.writeText("worker-runtime")
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
        GretlBuildResult result = runAndFail(project, "-Dgretl.mavenRepo=/tmp/gretl-e2e-repository-does-not-exist", "--offline", "tasks");
        assertFalse(result.successful());
        assertTrue(result.output().contains("GRETL plugin repository"), result.output());
        assertNoDownload(result);
    }

    private GradleTestProject project(String name) {
        return GradleTestProject.create(temporaryDirectory.resolve(name));
    }

    private GretlBuildResult run(GradleTestProject project, String... requestedArguments) {
        RuntimeImageDescriptor image = RuntimeImageDescriptor.fromSystemProperties();
        RuntimeImageOfflineExecutor executor = new RuntimeImageOfflineExecutor(image);
        List<String> arguments = new ArrayList<>(List.of("--no-daemon", "--offline", "--rerun-tasks"));
        arguments.addAll(Arrays.asList(requestedArguments));
        return executor.execute(GretlBuildRequest.builder(project.directory())
                .arguments(arguments)
                .timeout(Duration.ofMinutes(5))
                .runtimeImageOptions(RuntimeImageRunOptions.offline())
                .build());
    }

    private GretlBuildResult runAndFail(GradleTestProject project, String... requestedArguments) {
        RuntimeImageDescriptor image = RuntimeImageDescriptor.fromSystemProperties();
        RuntimeImageOfflineExecutor executor = new RuntimeImageOfflineExecutor(image);
        List<String> arguments = new ArrayList<>(List.of("--no-daemon", "--offline", "--rerun-tasks"));
        arguments.addAll(Arrays.asList(requestedArguments));
        return executor.executeAndExpectFailure(GretlBuildRequest.builder(project.directory())
                .arguments(arguments)
                .timeout(Duration.ofMinutes(5))
                .runtimeImageOptions(RuntimeImageRunOptions.offline())
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
