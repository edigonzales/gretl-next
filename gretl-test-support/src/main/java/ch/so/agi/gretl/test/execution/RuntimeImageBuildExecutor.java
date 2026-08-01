package ch.so.agi.gretl.test.execution;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.docker.DockerRunRequest;
import ch.so.agi.gretl.test.process.ProcessResult;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RuntimeImageBuildExecutor implements GretlBuildExecutor {
    private final RuntimeImageDescriptor image;
    private final DockerCli docker;
    private final ContainerUserResolver userResolver;
    private final RuntimeImageLifecycleArguments lifecycleArguments;
    private final GradleUserHomeStrategy gradleUserHomeStrategy;

    public RuntimeImageBuildExecutor(
                RuntimeImageDescriptor image,
                DockerCli docker,
                ContainerUserResolver userResolver,
                RuntimeImageLifecycleArguments lifecycleArguments) {
        this(image, docker, userResolver, lifecycleArguments, new FreshGradleUserHomeStrategy());
    }

    public RuntimeImageBuildExecutor(
            RuntimeImageDescriptor image,
            DockerCli docker,
            ContainerUserResolver userResolver,
            RuntimeImageLifecycleArguments lifecycleArguments,
            GradleUserHomeStrategy gradleUserHomeStrategy) {
        this.image = image;
        this.docker = docker;
        this.userResolver = userResolver;
        this.lifecycleArguments = lifecycleArguments;
        this.gradleUserHomeStrategy = gradleUserHomeStrategy;
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

    DockerRunRequest toDockerRunRequest(GretlBuildRequest request, Path gradleUserHome) {
        RuntimeImageRunOptions options = request.runtimeImageOptions();
        Map<String, String> environment = new HashMap<>(options.containerEnvironment());
        environment.putAll(request.environment());
        return new DockerRunRequest(
                image.imageId(),
                createContainerName(request),
                request.projectDirectory(),
                gradleUserHome,
                lifecycleArguments.arguments(RuntimeExecutionMode.ONE_SHOT, request.arguments()),
                environment,
                options.dockerNetwork(),
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

    public String createContainerName(GretlBuildRequest request) {
        String readable = request.projectDirectory().getFileName().toString()
                .replaceAll("[^A-Za-z0-9_.-]", "-");
        String hash = UUID.nameUUIDFromBytes(request.projectDirectory().toString().getBytes())
                .toString().replace("-", "").substring(0, 12);
        return ("gretl-e2e-" + readable + "-" + hash).substring(0,
                Math.min(127, ("gretl-e2e-" + readable + "-" + hash).length()));
    }

    private GretlBuildResult executeInternal(GretlBuildRequest request) {
        requireOneShot(request);
        try (GradleUserHomeHandle home = gradleUserHomeStrategy.prepare(
                request.projectDirectory(), RuntimeExecutionMode.ONE_SHOT)) {
            DockerRunRequest dockerRequest = toDockerRunRequest(request, home.path());
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
            }
        }
    }

    private void requireOneShot(GretlBuildRequest request) {
        if (request.runtimeExecutionMode() != RuntimeExecutionMode.ONE_SHOT) {
            throw new IllegalArgumentException(
                    "RuntimeImageBuildExecutor supports only ONE_SHOT execution. "
                            + "Use RuntimeImageServiceContainer for SERVICE execution.");
        }
    }

    private String diagnostic(String prefix, GretlBuildRequest request, GretlBuildResult result) {
        return prefix + ": imageId=" + image.imageId() + ", imageTag=" + image.imageTag()
                + ", project=" + request.projectDirectory() + ", exitCode=" + result.exitCode()
                + ", command=" + result.sanitizedCommand() + System.lineSeparator()
                + "stdout:\n" + result.standardOutput() + System.lineSeparator()
                + "stderr:\n" + result.standardError();
    }

}
