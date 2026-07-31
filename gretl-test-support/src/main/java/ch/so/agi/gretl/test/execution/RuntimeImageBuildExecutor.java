package ch.so.agi.gretl.test.execution;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.docker.DockerRunRequest;
import ch.so.agi.gretl.test.process.ProcessResult;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RuntimeImageBuildExecutor implements GretlBuildExecutor {
    private final RuntimeImageDescriptor image;
    private final DockerCli docker;
    private final ContainerUserResolver userResolver;
    private final RuntimeImageGradleArguments gradleArguments;

    public RuntimeImageBuildExecutor(
            RuntimeImageDescriptor image,
            DockerCli docker,
            ContainerUserResolver userResolver,
            RuntimeImageGradleArguments gradleArguments) {
        this.image = image;
        this.docker = docker;
        this.userResolver = userResolver;
        this.gradleArguments = gradleArguments;
        image.verify();
    }

    @Override
    public GretlBuildResult execute(GretlBuildRequest request) {
        GretlBuildResult result = executeInternal(request);
        if (!result.successful()) {
            throw new IllegalStateException(diagnostic("Expected successful GRETL build", request, result));
        }
        return result;
    }

    @Override
    public GretlBuildResult executeAndExpectFailure(GretlBuildRequest request) {
        GretlBuildResult result = executeInternal(request);
        if (result.successful()) {
            throw new IllegalStateException(diagnostic("Expected GRETL build to fail", request, result));
        }
        return result;
    }

    public DockerRunRequest toDockerRunRequest(GretlBuildRequest request) {
        RuntimeImageRunOptions options = request.runtimeImageOptions();
        Path gradleUserHome = createIsolatedGradleUserHome(request.projectDirectory());
        return new DockerRunRequest(
                image.imageId(),
                createContainerName(request),
                request.projectDirectory(),
                gradleUserHome,
                gradleArguments.arguments(inferProfile(request.arguments()), request.arguments()),
                options.containerEnvironment(),
                options.dockerNetwork(),
                options.networkDisabled(),
                userResolver.resolve(),
                request.timeout(),
                request.secretValues(),
                options.readOnlyMounts(),
                options.readWriteMounts());
    }

    public GretlBuildResult toBuildResult(ProcessResult result) {
        String output = result.output();
        return new GretlBuildResult(
                result.exitCode(),
                result.standardOutput(),
                result.standardError(),
                result.duration(),
                result.sanitizedCommand(),
                new GradleTaskOutputParser().parse(output));
    }

    public Path createIsolatedGradleUserHome(Path projectDirectory) {
        try {
            Path parent = projectDirectory.toAbsolutePath().normalize().getParent();
            return Files.createTempDirectory(parent, "gretl-runtime-gradle-home-");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create isolated Gradle user home for " + projectDirectory, e);
        }
    }

    public String createContainerName(GretlBuildRequest request) {
        String readable = request.projectDirectory().getFileName().toString()
                .replaceAll("[^A-Za-z0-9_.-]", "-");
        String hash = UUID.nameUUIDFromBytes(request.projectDirectory().toString().getBytes())
                .toString().replace("-", "").substring(0, 12);
        return ("gretl-e2e-" + readable + "-" + hash).substring(0,
                Math.min(127, ("gretl-e2e-" + readable + "-" + hash).length()));
    }

    private GretlBuildResult executeInternal(GretlBuildRequest request) {
        DockerRunRequest dockerRequest = toDockerRunRequest(request);
        try {
            ProcessResult result = docker.runContainer(dockerRequest);
            GretlBuildResult buildResult = toBuildResult(result);
            if (result.standardError().contains("timed out")) {
                docker.removeContainer(dockerRequest.containerName(), true);
            }
            return buildResult;
        } catch (RuntimeException e) {
            try {
                docker.removeContainer(dockerRequest.containerName(), true);
            } catch (RuntimeException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        } finally {
            deleteTree(dockerRequest.gradleUserHome());
        }
    }

    private RuntimeInvocationProfile inferProfile(List<String> arguments) {
        if (arguments.contains("--no-daemon")) {
            return arguments.contains("--offline")
                    ? RuntimeInvocationProfile.ONE_SHOT_OFFLINE : RuntimeInvocationProfile.ONE_SHOT_ONLINE;
        }
        return RuntimeInvocationProfile.LONG_LIVED_DAEMON;
    }

    private String diagnostic(String prefix, GretlBuildRequest request, GretlBuildResult result) {
        return prefix + ": imageId=" + image.imageId() + ", imageTag=" + image.imageTag()
                + ", project=" + request.projectDirectory() + ", exitCode=" + result.exitCode()
                + ", command=" + result.sanitizedCommand() + System.lineSeparator()
                + "stdout:\n" + result.standardOutput() + System.lineSeparator()
                + "stderr:\n" + result.standardError();
    }

    private void deleteTree(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Temporary directories are best-effort cleanup; the test result remains authoritative.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
