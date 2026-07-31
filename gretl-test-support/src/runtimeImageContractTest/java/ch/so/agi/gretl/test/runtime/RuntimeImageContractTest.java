package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.docker.DockerImageInspection;
import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.execution.RuntimeImageGradleArguments;
import ch.so.agi.gretl.test.process.ProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageContractTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void imageIdFileReferencesTheExactLocalImage() {
        RuntimeImageDescriptor image = image();
        DockerCli docker = new DockerCli();
        DockerImageInspection inspection = docker.inspectImage(image.imageId());

        assertEquals(image.imageId(), inspection.imageId());
        assertTrue(docker.imageExists(image.imageId()));
        assertEquals(image.imageId(), docker.inspectImage(image.imageTag()).imageId());
    }

    @Test
    void imageUsesExpectedRuntimeIdentityAndMetadata() {
        RuntimeImageDescriptor image = image();
        DockerImageInspection inspection = new DockerCli().inspectImage(image.imageId());

        assertNotEquals("0", inspection.user());
        assertNotEquals("root", inspection.user());
        assertTrue(inspection.entrypoint().contains("gretl"), inspection.raw());
        assertEquals("/home/gradle/project", inspection.workingDirectory());
        assertTrue(inspection.labels().contains(image.gretlVersion()), inspection.labels());
    }

    @Test
    void buildInfoMatchesProjectVersionGradleAndJava() {
        RuntimeImageDescriptor image = image();
        ProcessResult result = new DockerCli().execute(List.of(
                "docker", "run", "--rm", "--pull=never", "--entrypoint", "/bin/sh", image.imageId(),
                "-c", "cat /opt/gretl/build.info"), Duration.ofSeconds(30), Set.of());

        assertTrue(result.successful(), result.output());
        assertTrue(result.standardOutput().contains("GRETL Modular " + image.gretlVersion()), result.output());
        assertTrue(result.standardOutput().contains("Gradle " + image.expectedGradleVersion()), result.output());
        assertTrue(result.standardOutput().contains("Java " + image.expectedJavaMajorVersion()), result.output());
    }

    @Test
    void imageContainsStructuredPluginRepositoryAndWorkerRuntime() {
        RuntimeImageDescriptor image = image();
        DockerCli docker = new DockerCli();
        assertFile(docker, image, "/opt/gretl/init/gretl.init.gradle");
        assertFile(docker, image, "/opt/gretl/build.info");
        assertFile(docker, image, "/opt/gretl/manifests/duckdb-extensions.json");
        assertFile(docker, image, "/usr/local/bin/gretl");
        assertPomUnder(docker, image, "/opt/gretl/maven-repository/ch/so/agi/gretl/ch.so.agi.gretl.gradle.plugin/"
                + image.gretlVersion());
        assertPomUnder(docker, image, "/opt/gretl/maven-repository/ch/so/agi/gretl/geotools/ch.so.agi.gretl.geotools.gradle.plugin/"
                + image.gretlVersion());
        ProcessResult files = docker.execute(List.of("docker", "run", "--rm", "--pull=never", "--entrypoint", "/usr/bin/find", image.imageId(),
                "/opt/gretl/maven-repository", "-type", "f"), Duration.ofSeconds(30), Set.of());
        assertTrue(files.successful(), files.output());
        assertTrue(files.standardOutput().contains("gretl-geotools"), files.standardOutput());
        assertFalse(files.standardOutput().contains("-sources.jar"), files.standardOutput());
        assertFalse(files.standardOutput().contains("-javadoc.jar"), files.standardOutput());

        ProcessResult worker = docker.execute(List.of("docker", "run", "--rm", "--pull=never",
                "--entrypoint", "/bin/sh", image.imageId(), "-c",
                "jar=$(find /opt/gretl/maven-repository/ch/so/agi/gretl-geotools -name 'gretl-geotools-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1); unzip -l \"$jar\""),
                Duration.ofSeconds(30), Set.of());
        assertTrue(worker.successful(), worker.output());
        assertTrue(worker.standardOutput().contains("gretl-geotools-worker-classpath/"), worker.output());
    }

    @Test
    void mountedConsumerCanWriteHostReadableOutput() throws Exception {
        RuntimeImageDescriptor image = image();
        Path project = Files.createDirectories(temporaryDirectory.resolve("GRETL E2E äöü"));
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'contract'\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                tasks.register('writeMarker') {
                    doLast {
                        def output = file('build/marker.txt')
                        output.parentFile.mkdirs()
                        output.text = 'runtime-image'
                    }
                }
                """, StandardCharsets.UTF_8);

        RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(
                image, new DockerCli(), new ContainerUserResolver(), new RuntimeImageGradleArguments());
        GretlBuildResult result = executor.execute(GretlBuildRequest.builder(project)
                .arguments("--no-daemon", "--offline", "--rerun-tasks", "writeMarker")
                .timeout(Duration.ofMinutes(2))
                .runtimeImageOptions(RuntimeImageRunOptions.offline())
                .build());

        assertTrue(result.successful(), result.output());
        Path marker = project.resolve("build/marker.txt");
        assertEquals("runtime-image", Files.readString(marker, StandardCharsets.UTF_8));
        Files.delete(marker);
        assertFalse(Files.exists(marker));
    }

    @Test
    void launcherPropagatesFailureExitCode() throws Exception {
        RuntimeImageDescriptor image = image();
        Path project = Files.createDirectories(temporaryDirectory.resolve("failure"));
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'failure'\n");
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                tasks.register('failTask') { doLast { throw new GradleException('expected failure') } }
                """);
        RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(
                image, new DockerCli(), new ContainerUserResolver(), new RuntimeImageGradleArguments());
        GretlBuildResult result = executor.executeAndExpectFailure(GretlBuildRequest.builder(project)
                .arguments("--no-daemon", "--offline", "failTask")
                .timeout(Duration.ofMinutes(2))
                .runtimeImageOptions(RuntimeImageRunOptions.offline())
                .build());
        assertNotEquals(0, result.exitCode());
        assertTrue(result.output().contains("expected failure"), result.output());
    }

    private RuntimeImageDescriptor image() {
        return RuntimeImageDescriptor.fromSystemProperties();
    }

    private void assertFile(DockerCli docker, RuntimeImageDescriptor image, String path) {
        var result = docker.execute(List.of("docker", "run", "--rm", "--pull=never", "--entrypoint", "/usr/bin/test", image.imageId(),
                "-s", path), Duration.ofSeconds(30), Set.of());
        assertTrue(result.successful(), "Missing image file " + path + ": " + result.output());
    }

    private void assertPomUnder(DockerCli docker, RuntimeImageDescriptor image, String directory) {
        ProcessResult result = docker.execute(List.of("docker", "run", "--rm", "--pull=never", "--entrypoint",
                "/usr/bin/find", image.imageId(), directory, "-type", "f", "-name", "*.pom"),
                Duration.ofSeconds(30), Set.of());
        assertTrue(result.successful() && !result.standardOutput().isBlank(),
                "Missing plugin marker POM under " + directory + ": " + result.output());
    }
}
