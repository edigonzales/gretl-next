package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.fixture.FixturePluginRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageDaemonReuseTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void longLivedContainerReusesDaemonAcrossProjectsAndStopsCleanly() throws Exception {
        RuntimeImageDescriptor image = RuntimeImageDescriptor.fromSystemProperties();
        Path jobs = Files.createDirectories(temporaryDirectory.resolve("jobs"));
        Path jobA = createJob(jobs, "job-a", "a");
        Path jobB = createJob(jobs, "job-b", "b");
        Path gradleHome = Files.createDirectories(temporaryDirectory.resolve("gradle-home"));

        try (RuntimeImageServiceContainer service = RuntimeImageServiceContainer.start(
                image, jobs, gradleHome, Optional.empty(), new ContainerUserResolver().resolve())) {
            assertTrue(service.isRunning());
            var first = service.execGretl(Path.of("job-a"), List.of("--console=plain", "--rerun-tasks", "writeMarker"));
            assertEquals(0, first.exitCode(), first.output());
            Set<Long> firstPids = service.daemonPids();
            assertTrue(Files.exists(jobA.resolve("build/marker.txt")));

            var second = service.execGretl(Path.of("job-b"), List.of("--console=plain", "--rerun-tasks", "writeMarker"));
            assertEquals(0, second.exitCode(), second.output());
            Set<Long> secondPids = service.daemonPids();
            assertTrue(!firstPids.isEmpty(), "No Gradle daemon was reported: " + first);
            assertTrue(!secondPids.isEmpty(), "No Gradle daemon was reported: " + second);
            assertTrue(!java.util.Collections.disjoint(firstPids, secondPids),
                    "Daemon was not reused: first=" + firstPids + ", second=" + secondPids);
            assertEquals("a", Files.readString(jobA.resolve("build/marker.txt"), StandardCharsets.UTF_8));
            assertEquals("b", Files.readString(jobB.resolve("build/marker.txt"), StandardCharsets.UTF_8));

            Files.writeString(jobA.resolve("build.gradle"), """
                    plugins { id 'ch.so.agi.gretl' }
                    tasks.register('writeMarker') { doLast { file('build/marker.txt').text = 'changed' } }
                    """, StandardCharsets.UTF_8);
            var changed = service.execGretl(Path.of("job-a"), List.of("--console=plain", "--rerun-tasks", "writeMarker"));
            assertEquals(0, changed.exitCode(), changed.output());
            assertEquals("changed", Files.readString(jobA.resolve("build/marker.txt"), StandardCharsets.UTF_8));

            var stop = service.stopGradleDaemons();
            assertEquals(0, stop.exitCode(), stop.output());
            assertTrue(service.isRunning(), "Stopping Gradle must not stop the service container");
            var afterStop = service.execGretl(Path.of("job-b"), List.of("--console=plain", "--rerun-tasks", "writeMarker"));
            assertEquals(0, afterStop.exitCode(), afterStop.output());
            Set<Long> afterStopPids = service.daemonPids();
            assertTrue(!afterStopPids.isEmpty());
            assertTrue(java.util.Collections.disjoint(firstPids, afterStopPids),
                    "A new daemon should be started after --stop");

            var secondJob = service.execGretl(Path.of("job-a"), List.of("--rerun-tasks", "writeMarker"));
            assertEquals(0, secondJob.exitCode(), secondJob.output());
            assertTrue(service.isRunning());
        }
    }

    @Test
    void serviceContainerCanComposeAnAdditionalReadOnlyPluginRepository() throws Exception {
        RuntimeImageDescriptor image = RuntimeImageDescriptor.fromSystemProperties();
        Path jobs = Files.createDirectories(temporaryDirectory.resolve("plugin-jobs"));
        Path repository = FixturePluginRepository.create(temporaryDirectory.resolve("fixture-plugin-repo"));
        Path project = Files.createDirectories(jobs.resolve("consumer"));
        Files.writeString(project.resolve("settings.gradle"), """
                pluginManagement {
                    repositories { maven { url = uri('/fixture/plugin-repo') } }
                }
                rootProject.name = 'service-additional-plugin'
                """, StandardCharsets.UTF_8);
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'fixture.additional' version '1.0'
                }
                tasks.register('combined') {
                    dependsOn 'fixturePluginTask'
                    doLast { file('build/service-plugin.txt').text = 'service' }
                }
                """, StandardCharsets.UTF_8);
        Path gradleHome = Files.createDirectories(temporaryDirectory.resolve("plugin-gradle-home"));

        try (RuntimeImageServiceContainer service = RuntimeImageServiceContainer.start(
                image, jobs, gradleHome, Optional.empty(), new ContainerUserResolver().resolve(),
                Map.of(repository, "/fixture/plugin-repo"))) {
            var result = service.execGretl(Path.of("consumer"),
                    List.of("--rerun-tasks", "combined", "--console=plain"));
            assertEquals(0, result.exitCode(), result.output());
            assertTrue(Files.exists(project.resolve("build/service-plugin.txt")));
            assertEquals("service", Files.readString(project.resolve("build/service-plugin.txt"), StandardCharsets.UTF_8));
        }
    }

    private Path createJob(Path jobs, String name, String marker) throws Exception {
        Path job = Files.createDirectories(jobs.resolve(name));
        Files.writeString(job.resolve("settings.gradle"), "rootProject.name = '" + name + "'\n");
        Files.writeString(job.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                tasks.register('writeMarker') {
                    doLast {
                        def output = file('build/marker.txt')
                        output.parentFile.mkdirs()
                        output.text = '%s'
                    }
                }
                """.formatted(marker), StandardCharsets.UTF_8);
        return job;
    }
}
