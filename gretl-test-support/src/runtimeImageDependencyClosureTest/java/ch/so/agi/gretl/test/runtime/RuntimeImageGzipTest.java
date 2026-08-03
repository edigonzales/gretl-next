package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageGzipTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void compressesFileWithGroovyDsl() throws Exception {
        GradleTestProject project = project("gzip-groovy");
        byte[] input = "bundled gzip payload\n".getBytes(StandardCharsets.UTF_8);
        project.settingsGroovy("rootProject.name = 'gzip-groovy'\n")
                .binaryFile("data/input.txt", input)
                .buildGroovy("""
                        import ch.so.agi.gretl.tasks.Gzip
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('compress', Gzip) {
                            dataFile 'data/input.txt'
                            gzipFile 'build/nested/output/payload.txt.gz'
                        }
                        """);

        GretlBuildResult result = run(project, "compress");
        assertTrue(result.successful(), result.output());
        assertArrayEquals(input, gunzip(project.path("build/nested/output/payload.txt.gz")));
        assertFalse(result.output().matches("(?is).*download(ing|ed).*"), result.output());
    }

    @Test
    void missingInputFailsClearly() {
        GradleTestProject project = project("gzip-missing");
        project.settingsGroovy("rootProject.name = 'gzip-missing'\n")
                .buildGroovy("""
                        import ch.so.agi.gretl.tasks.Gzip
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('compress', Gzip) {
                            dataFile 'data/missing.txt'
                            gzipFile 'build/out/payload.gz'
                        }
                        """);

        GretlBuildResult result = runExpectFailure(project, "compress");
        assertTrue(result.output().contains("data/missing.txt"), result.output());
    }

    private GradleTestProject project(String name) {
        return GradleTestProject.create(temporaryDirectory.resolve(name));
    }

    private GretlBuildResult run(GradleTestProject project, String task) {
        return executor().execute(request(project, task));
    }

    private GretlBuildResult runExpectFailure(GradleTestProject project, String task) {
        return executor().executeAndExpectFailure(request(project, task));
    }

    private GretlBuildRequest request(GradleTestProject project, String task) {
        return GretlBuildRequest.builder(project.directory())
                .arguments(List.of("--rerun-tasks", task))
                .timeout(Duration.ofMinutes(2))
                .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                .build();
    }

    private RuntimeImageBuildExecutor executor() {
        return new RuntimeImageBuildExecutor(RuntimeImageDescriptor.fromSystemProperties(),
                new ch.so.agi.gretl.test.docker.DockerCli(),
                new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                new ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments());
    }

    private static byte[] gunzip(Path path) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPInputStream input = new GZIPInputStream(Files.newInputStream(path))) {
            input.transferTo(output);
        }
        return output.toByteArray();
    }
}
