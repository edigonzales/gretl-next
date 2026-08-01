package ch.so.agi.gretl.test.execution;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.docker.DockerRunRequest;
import ch.so.agi.gretl.test.process.ProcessResult;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageBuildExecutorLifecycleTest {
    private static final String IMAGE_ID = "sha256:" + "a".repeat(64);

    @TempDir
    Path temporaryDirectory;

    @Test
    void oneShotExecutorAcceptsOneShot() throws Exception {
        CountingHomeStrategy homes = new CountingHomeStrategy(temporaryDirectory.resolve("home"));
        RecordingDocker docker = new RecordingDocker(false);
        RuntimeImageBuildExecutor executor = executor(docker, homes);

        GretlBuildResult result = executor.execute(request(RuntimeExecutionMode.ONE_SHOT));

        assertTrue(result.successful());
        assertEquals(1, homes.prepareCount);
        assertEquals(1, homes.closeCount);
        assertEquals(1, docker.runCount);
    }

    @Test
    void oneShotExecutorRejectsServiceBeforeCreatingGradleHome() throws Exception {
        CountingHomeStrategy homes = new CountingHomeStrategy(temporaryDirectory.resolve("home"));
        RecordingDocker docker = new RecordingDocker(false);
        RuntimeImageBuildExecutor executor = executor(docker, homes);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> executor.execute(request(RuntimeExecutionMode.SERVICE)));

        assertTrue(error.getMessage().contains("only ONE_SHOT"));
        assertEquals(0, homes.prepareCount);
        assertEquals(0, docker.runCount);
    }

    @Test
    void successfulExecutionClosesTemporaryGradleHome() throws Exception {
        CountingHomeStrategy homes = new CountingHomeStrategy(temporaryDirectory.resolve("success-home"));
        RuntimeImageBuildExecutor executor = executor(new RecordingDocker(false), homes);

        executor.execute(request(RuntimeExecutionMode.ONE_SHOT));

        assertEquals(1, homes.closeCount);
    }

    @Test
    void failedBuildClosesTemporaryGradleHome() throws Exception {
        CountingHomeStrategy homes = new CountingHomeStrategy(temporaryDirectory.resolve("failed-home"));
        RuntimeImageBuildExecutor executor = executor(new RecordingDocker(true), homes);

        GretlBuildResult result = executor.executeAndExpectFailure(request(RuntimeExecutionMode.ONE_SHOT));

        assertTrue(!result.successful());
        assertEquals(1, homes.closeCount);
    }

    @Test
    void dockerFailureClosesTemporaryGradleHome() throws Exception {
        CountingHomeStrategy homes = new CountingHomeStrategy(temporaryDirectory.resolve("docker-failure-home"));
        RuntimeImageBuildExecutor executor = executor(new ThrowingDocker(), homes);

        assertThrows(IllegalStateException.class,
                () -> executor.execute(request(RuntimeExecutionMode.ONE_SHOT)));

        assertEquals(1, homes.closeCount);
    }

    private RuntimeImageBuildExecutor executor(DockerCli docker, CountingHomeStrategy homes) throws Exception {
        Path idFile = Files.writeString(temporaryDirectory.resolve("image-id.txt"), IMAGE_ID + "\n");
        RuntimeImageDescriptor image = new RuntimeImageDescriptor(
                IMAGE_ID, "gretl:test", "5.0.0-SNAPSHOT", "7.6.4", "17", idFile);
        return new RuntimeImageBuildExecutor(image, docker, new ContainerUserResolver(),
                new RuntimeImageLifecycleArguments(), homes);
    }

    private GretlBuildRequest request(RuntimeExecutionMode mode) throws Exception {
        Path project = Files.createDirectories(temporaryDirectory.resolve("project-" + mode));
        return GretlBuildRequest.builder(project)
                .arguments(List.of("tasks"))
                .timeout(Duration.ofSeconds(30))
                .runtimeExecutionMode(mode)
                .build();
    }

    private static final class CountingHomeStrategy implements GradleUserHomeStrategy {
        private final Path path;
        private int prepareCount;
        private int closeCount;

        private CountingHomeStrategy(Path path) {
            this.path = path;
        }

        @Override
        public GradleUserHomeHandle prepare(Path projectDirectory, RuntimeExecutionMode executionMode) {
            prepareCount++;
            return new GradleUserHomeHandle() {
                @Override
                public Path path() {
                    return path;
                }

                @Override
                public void close() {
                    closeCount++;
                }
            };
        }
    }

    private static class RecordingDocker extends DockerCli {
        private final boolean failed;
        private int runCount;

        private RecordingDocker(boolean failed) {
            this.failed = failed;
        }

        @Override
        public ProcessResult runContainer(DockerRunRequest request) {
            runCount++;
            return new ProcessResult(failed ? 1 : 0, failed ? "build failed" : "", "",
                    Duration.ofMillis(1), List.of("docker", "run"));
        }
    }

    private static final class ThrowingDocker extends DockerCli {
        @Override
        public ProcessResult runContainer(DockerRunRequest request) {
            throw new IllegalStateException("docker unavailable");
        }

        @Override
        public ProcessResult removeContainer(String containerName, boolean force) {
            return new ProcessResult(0, "", "", Duration.ofMillis(1), List.of("docker", "rm"));
        }
    }
}
