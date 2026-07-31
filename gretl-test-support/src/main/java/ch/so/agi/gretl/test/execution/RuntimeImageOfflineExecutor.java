package ch.so.agi.gretl.test.execution;

import ch.so.agi.gretl.test.docker.ContainerMount;
import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.docker.DockerContainerInspection;
import ch.so.agi.gretl.test.docker.DockerCreateRequest;
import ch.so.agi.gretl.test.process.ProcessResult;
import ch.so.agi.gretl.test.process.SecretMasker;
import ch.so.agi.gretl.test.runtime.OfflineContainerNameFactory;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes black-box Gradle builds against exactly one inspected image ID. */
public final class RuntimeImageOfflineExecutor implements GretlBuildExecutor {
    private static final String PROJECT_TARGET = "/work/project";
    private static final String GRADLE_HOME_TARGET = "/work/gradle-home";
    private final RuntimeImageDescriptor descriptor;
    private final DockerCli docker;
    private final OfflineContainerNameFactory nameFactory;
    private final ContainerUserResolver users;
    private final SecretMasker secrets;

    public RuntimeImageOfflineExecutor(RuntimeImageDescriptor descriptor) {
        this(descriptor, new DockerCli(), new OfflineContainerNameFactory(), new ContainerUserResolver(),
                new SecretMasker());
    }

    public RuntimeImageOfflineExecutor(RuntimeImageDescriptor descriptor, DockerCli docker,
            OfflineContainerNameFactory nameFactory, ContainerUserResolver users, SecretMasker secrets) {
        this.descriptor = descriptor;
        this.docker = docker;
        this.nameFactory = nameFactory;
        this.users = users;
        this.secrets = secrets;
        descriptor.verify();
    }

    public RuntimeImageOfflineExecutor(RuntimeImageDescriptor descriptor, DockerCli docker,
            OfflineContainerNameFactory nameFactory, SecretMasker secrets) {
        this(descriptor, docker, nameFactory, new ContainerUserResolver(), secrets);
    }

    @Override
    public GretlBuildResult execute(GretlBuildRequest request) {
        GretlBuildResult result = executeInternal(request);
        if (!result.successful()) {
            throw new IllegalStateException(diagnostic("Expected successful offline GRETL build", request, result));
        }
        return result;
    }

    @Override
    public GretlBuildResult executeAndExpectFailure(GretlBuildRequest request) {
        GretlBuildResult result = executeInternal(request);
        if (result.successful()) {
            throw new IllegalStateException(diagnostic("Expected offline GRETL build to fail", request, result));
        }
        return result;
    }

    public DockerCreateRequest createRequest(GretlBuildRequest request) {
        requireOfflineRequest(request);
        List<String> arguments = gradleArguments(request);
        return new DockerCreateRequest(descriptor.imageId(),
                nameFactory.create(request.projectDirectory().getFileName().toString() + "-" + request.arguments()),
                arguments, containerEnvironment(request), containerMounts(request), true, true,
                PROJECT_TARGET, users.resolveFromOverride().orElse("1001:0"));
    }

    public List<String> gradleArguments(GretlBuildRequest request) {
        List<String> arguments = new ArrayList<>(request.arguments());
        addIfAbsent(arguments, "--offline");
        addIfAbsent(arguments, "--no-daemon");
        addIfAbsent(arguments, "--console=plain");
        addIfAbsent(arguments, "--stacktrace");
        return List.copyOf(arguments);
    }

    public Map<String, String> containerEnvironment(GretlBuildRequest request) {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.putAll(request.runtimeImageOptions().containerEnvironment());
        environment.putAll(request.environment());
        environment.put("GRADLE_USER_HOME", GRADLE_HOME_TARGET);
        environment.put("GRETL_IMAGE_OFFLINE", "true");
        environment.put("DUCKDB_EXTENSION_DIRECTORY", "/opt/gretl/duckdb-extensions");
        return Map.copyOf(environment);
    }

    public List<ContainerMount> containerMounts(GretlBuildRequest request) {
        if (!request.runtimeImageOptions().readWriteMounts().isEmpty()) {
            throw new IllegalArgumentException("Strict offline execution allows only the consumer project as a read-write mount");
        }
        List<ContainerMount> mounts = new ArrayList<>();
        mounts.add(ContainerMount.readWriteBind(request.projectDirectory(), PROJECT_TARGET));
        request.runtimeImageOptions().readOnlyMounts().forEach((host, target) ->
                mounts.add(ContainerMount.readOnlyBind(host, target)));
        mounts.add(ContainerMount.tmpfs(GRADLE_HOME_TARGET));
        return List.copyOf(mounts);
    }

    public void verifyInspection(DockerContainerInspection inspection) {
        inspection.assertStrictOffline(descriptor);
        String tagId = docker.inspectImage(descriptor.imageTag()).imageId();
        if (!descriptor.imageId().equals(tagId)) {
            throw new IllegalStateException("Runtime image tag " + descriptor.imageTag() + " resolves to " + tagId
                    + ", expected immutable image ID " + descriptor.imageId());
        }
    }

    public GretlBuildResult result(GretlBuildRequest request, String containerId, ProcessResult process) {
        return new GretlBuildResult(process.exitCode(), process.standardOutput(), process.standardError(),
                process.duration(), process.sanitizedCommand(), new GradleTaskOutputParser().parse(process.output()));
    }

    private GretlBuildResult executeInternal(GretlBuildRequest request) {
        DockerCreateRequest create = createRequest(request);
        String containerId = null;
        ProcessResult process = null;
        RuntimeException primaryFailure = null;
        try {
            containerId = docker.createContainer(create, request.secretValues());
            DockerContainerInspection inspection = docker.inspectContainer(containerId);
            verifyInspection(inspection);
            process = docker.startAndAttach(containerId, request.timeout(), request.secretValues());
            GretlBuildResult result = result(request, containerId, process);
            writeDiagnosticsOnFailure(request, create, inspection, result);
            return result;
        } catch (RuntimeException e) {
            primaryFailure = e;
            throw e;
        } finally {
            if (containerId != null) {
                try {
                    docker.removeForce(containerId);
                } catch (RuntimeException cleanupFailure) {
                    if (primaryFailure != null) {
                        primaryFailure.addSuppressed(cleanupFailure);
                    } else {
                        throw cleanupFailure;
                    }
                }
            }
        }
    }

    private void writeDiagnosticsOnFailure(GretlBuildRequest request, DockerCreateRequest create,
            DockerContainerInspection inspection, GretlBuildResult result) {
        if (result.successful() || !Boolean.getBoolean("gretlOfflineTestKeepDiagnostics")) {
            return;
        }
        Path directory = descriptor.imageIdFile().toAbsolutePath().normalize().getParent().resolve("diagnostics");
        try {
            Files.createDirectories(directory);
            Path context = directory.getParent().getParent().resolve("docker");
            String base = nameFactory.create(request.projectDirectory().getFileName().toString());
            Path descriptorFile = directory.getParent().resolve("image-descriptor.json");
            if (Files.isRegularFile(descriptorFile)) {
                Files.copy(descriptorFile, directory.resolve(base + "-image-descriptor.json"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            for (String file : List.of("build.info", "manifests/offline-artifacts.json",
                    "manifests/dependency-closure.json", "manifests/checksums.sha256")) {
                Path source = context.resolve(file);
                if (Files.isRegularFile(source)) {
                    Files.copy(source, directory.resolve(base + "-" + file.replace('/', '-')),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.writeString(directory.resolve(base + "-command.txt"),
                    String.join(" ", create.command()), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve(base + "-inspection.txt"), inspection.toString(), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve(base + "-stdout.txt"), secrets.mask(result.standardOutput(), request.secretValues()),
                    StandardCharsets.UTF_8);
            Files.writeString(directory.resolve(base + "-stderr.txt"), secrets.mask(result.standardError(), request.secretValues()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Diagnostics are best-effort and must not hide the test failure.
        }
    }

    private void requireOfflineRequest(GretlBuildRequest request) {
        if (!request.runtimeImageOptions().networkDisabled()) {
            throw new IllegalArgumentException("RuntimeImageOfflineExecutor requires networkDisabled=true");
        }
    }

    private void addIfAbsent(List<String> arguments, String argument) {
        if (!arguments.contains(argument)) {
            arguments.add(argument);
        }
    }

    private String diagnostic(String prefix, GretlBuildRequest request, GretlBuildResult result) {
        return prefix + ": imageId=" + descriptor.imageId() + ", project=" + request.projectDirectory()
                + ", exitCode=" + result.exitCode() + ", command=" + result.sanitizedCommand()
                + System.lineSeparator() + result.output();
    }
}
