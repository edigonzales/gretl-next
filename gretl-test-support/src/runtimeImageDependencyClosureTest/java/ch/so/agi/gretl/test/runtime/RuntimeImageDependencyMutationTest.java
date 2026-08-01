package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageDependencyMutationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void wrongBundledVersionFailsClearlyWithoutFallback() {
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("wrong-version"));
        project.settingsGroovy("rootProject.name = 'wrong-version'\n")
                .buildGroovy("plugins { id 'ch.so.agi.gretl' }\n");

        GretlBuildResult result = new RuntimeImageBuildExecutor(RuntimeImageDescriptor.fromSystemProperties(),
                new ch.so.agi.gretl.test.docker.DockerCli(),
                new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                new ch.so.agi.gretl.test.execution.RuntimeImageGradleArguments())
                .executeAndExpectFailure(GretlBuildRequest.builder(project.directory())
                        .arguments(List.of("-Dgretl.version=0.0.0-mutated", "--rerun-tasks", "tasks"))
                        .timeout(Duration.ofMinutes(2))
                        .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                        .build());

        assertFalse(result.successful());
        assertTrue(result.output().contains("Requested GRETL plugin version 0.0.0-mutated is not bundled."), result.output());
        assertTrue(result.output().contains("0.0.0-mutated"), result.output());
    }

    @Test
    void removingCoreMarkerMakesCorePluginResolutionFail() throws Exception {
        RuntimeImageDescriptor mutated = buildMutationImage("remove-core-marker", """
                RUN rm -rf /opt/gretl/maven-repository/ch/so/agi/gretl/ch.so.agi.gretl.gradle.plugin
                """);
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("missing-marker"));
        project.settingsGroovy("rootProject.name = 'missing-marker'\n")
                .buildGroovy("plugins { id 'ch.so.agi.gretl' }\n");

        GretlBuildResult result = runAgainst(mutated, project);
        assertFalse(result.successful());
        assertTrue(result.output().contains("ch.so.agi.gretl")
                || result.output().contains("GRETL plugin repository"), result.output());
    }

    @Test
    void removingCoreImplementationJarMakesCorePluginResolutionFail() throws Exception {
        RuntimeImageDescriptor mutated = buildMutationImage("remove-core-jar", """
                RUN find /opt/gretl/maven-repository/ch/so/agi/gretl-core -name 'gretl-core-*.jar' -delete
                """);
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("missing-core-jar"));
        project.settingsGroovy("rootProject.name = 'missing-core-jar'\n")
                .buildGroovy("plugins { id 'ch.so.agi.gretl' }\n");

        GretlBuildResult result = runAgainst(mutated, project);
        assertFalse(result.successful());
        assertTrue(result.output().contains("gretl-core") || result.output().contains("plugin"), result.output());
    }

    private GretlBuildResult runAgainst(RuntimeImageDescriptor image, GradleTestProject project) {
        try {
            return new RuntimeImageBuildExecutor(image,
                    new ch.so.agi.gretl.test.docker.DockerCli(),
                    new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                    new ch.so.agi.gretl.test.execution.RuntimeImageGradleArguments()).executeAndExpectFailure(
                    GretlBuildRequest.builder(project.directory())
                            .arguments(List.of("--rerun-tasks", "tasks"))
                            .timeout(Duration.ofMinutes(2))
                            .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                            .build());
        } finally {
            try {
                Process cleanup = new ProcessBuilder("docker", "image", "rm", "--force", image.imageTag())
                        .redirectErrorStream(true).start();
                cleanup.waitFor();
            } catch (Exception ignored) {
                // Preserve the mutation assertion if cleanup itself fails.
            }
        }
    }

    private RuntimeImageDescriptor buildMutationImage(String name, String mutation) throws Exception {
        RuntimeImageDescriptor base = RuntimeImageDescriptor.fromSystemProperties();
        Path context = Files.createDirectories(temporaryDirectory.resolve("image-" + name));
        String tag = "gretl-dependency-closure-mutation:" + name;
        Files.writeString(context.resolve("Dockerfile"), "FROM " + base.imageTag() + "\n"
                + "USER root\n" + mutation + "USER gradle\n", StandardCharsets.UTF_8);
        Path idFile = context.resolve("image-id.txt");
        List<String> command = List.of("docker", "build", "--pull=false", "--iidfile", idFile.toString(),
                "--tag", tag, ".");
        Process process = new ProcessBuilder(command).directory(context.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Mutation image build failed: " + output);
        }
        String imageId = Files.readString(idFile).trim();
        return new RuntimeImageDescriptor(imageId, tag, base.gretlVersion(), base.expectedGradleVersion(),
                base.expectedJavaMajorVersion(), idFile);
    }
}
