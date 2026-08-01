package ch.so.agi.gretl.test.docker;

import ch.so.agi.gretl.test.process.ProcessExecutor;
import ch.so.agi.gretl.test.process.ProcessRequest;
import ch.so.agi.gretl.test.process.ProcessResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DockerCli {
    private final ProcessExecutor processExecutor;

    public DockerCli() {
        this(new ProcessExecutor());
    }

    public DockerCli(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public DockerInfo verifyAvailable() {
        try {
            ProcessResult version = execute(List.of("docker", "version", "--format", "{{.Server.Version}}"),
                    Duration.ofSeconds(20), Set.of());
            ProcessResult info = execute(List.of("docker", "info", "--format", "{{.OSType}}/{{.Architecture}}"),
                    Duration.ofSeconds(20), Set.of());
            if (!version.successful() || !info.successful()) {
                throw new IllegalStateException("Docker daemon is not reachable.\n" + version.output() + info.output());
            }
            return new DockerInfo(version.standardOutput().trim(), info.standardOutput().trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Docker is required to execute runtimeImageE2eTest; Docker CLI/daemon verification failed.", e);
        }
    }

    public DockerImageInspection inspectImage(String imageReference) {
        String id = inspectFormat(imageReference, "{{.Id}}");
        String entrypoint = inspectFormat(imageReference, "{{json .Config.Entrypoint}}");
        String workingDirectory = inspectFormat(imageReference, "{{.Config.WorkingDir}}");
        String user = inspectFormat(imageReference, "{{.Config.User}}");
        String labels = inspectFormat(imageReference, "{{json .Config.Labels}}");
        String raw = String.join(System.lineSeparator(), id, entrypoint, workingDirectory, user, labels);
        return new DockerImageInspection(id, entrypoint, workingDirectory, user, labels, raw);
    }

    public String inspectFormat(String imageReference, String format) {
        ProcessResult result = execute(List.of("docker", "image", "inspect", "--format", format, imageReference),
                Duration.ofSeconds(30), Set.of());
        if (!result.successful()) {
            throw new IllegalStateException("Docker image is not available locally: " + imageReference + "\n"
                    + result.output());
        }
        return result.standardOutput().trim();
    }

    public ProcessResult runContainer(DockerRunRequest request) {
        List<String> command = new DockerRunCommandBuilder().build(request);
        return execute(command, request.timeout(), request.secretValues());
    }

    public String createContainer(DockerCreateRequest request) {
        return createContainer(request, Set.of());
    }

    public String createContainer(DockerCreateRequest request, Set<String> secrets) {
        List<String> command = new ArrayList<>(List.of("docker", "create", "--pull=never",
                "--name", request.containerName()));
        request.network().ifPresent(network -> command.addAll(List.of("--network", network)));
        if (request.readOnlyRootFilesystem()) {
            command.add("--read-only");
        }
        if (request.user() != null && !request.user().isBlank()) {
            command.addAll(List.of("--user", request.user()));
        }
        command.addAll(List.of("--workdir", request.workingDirectory(), "--tmpfs", "/tmp:rw,exec,nosuid,nodev,mode=1777"));
        for (ContainerMount mount : request.mounts()) {
            if (mount.type() == ContainerMount.MountType.TMPFS) {
                command.addAll(List.of("--tmpfs", mount.containerPath() + ":rw,exec,nosuid,nodev,mode=1777"));
            } else {
                String value = "type=bind,src=" + mount.hostPath() + ",dst=" + mount.containerPath();
                if (mount.access() == ContainerMount.MountAccess.READ_ONLY) {
                    value += ",readonly";
                }
                command.addAll(List.of("--mount", value));
            }
        }
        request.environment().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                command.addAll(List.of("--env", entry.getKey() + "=" + entry.getValue())));
        command.add(request.imageId());
        command.addAll(request.command());
        ProcessResult result = execute(command, Duration.ofSeconds(30), secrets);
        if (!result.successful() || result.standardOutput().isBlank()) {
            throw new IllegalStateException("docker create failed: " + result.output());
        }
        return result.standardOutput().trim().split("\\R", 2)[0];
    }

    public DockerContainerInspection inspectContainer(String containerId) {
        String id = inspectContainerFormat(containerId, "{{.Id}}");
        String imageId = inspectContainerFormat(containerId, "{{.Image}}");
        String network = inspectContainerFormat(containerId, "{{.HostConfig.NetworkMode}}");
        String user = inspectContainerFormat(containerId, "{{.Config.User}}");
        boolean readOnly = Boolean.parseBoolean(inspectContainerFormat(containerId, "{{.HostConfig.ReadonlyRootfs}}"));
        Map<String, DockerMountInspection> mounts = new LinkedHashMap<>();
        String mountOutput = inspectContainerFormat(containerId,
                "{{range .Mounts}}{{.Source}}|{{.Destination}}|{{.Type}}|{{.RW}}{{\"\\n\"}}{{end}}");
        for (String line : mountOutput.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length == 4) {
                mounts.put(parts[1], new DockerMountInspection(parts[0], parts[1], parts[2], Boolean.parseBoolean(parts[3])));
            }
        }
        String tmpfsOutput = inspectContainerFormat(containerId,
                "{{range $target, $options := .HostConfig.Tmpfs}}tmpfs|{{$target}}|tmpfs|true{{\"\\n\"}}{{end}}");
        for (String line : tmpfsOutput.split("\\R")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", -1);
            if (parts.length == 4) {
                mounts.put(parts[1], new DockerMountInspection(parts[0], parts[1], parts[2], Boolean.parseBoolean(parts[3])));
            }
        }
        Map<String, String> environment = new LinkedHashMap<>();
        String envOutput = inspectContainerFormat(containerId, "{{range .Config.Env}}{{.}}{{\"\\n\"}}{{end}}");
        for (String line : envOutput.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                environment.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return new DockerContainerInspection(id, imageId, network, user, readOnly, mounts, environment);
    }

    private String inspectContainerFormat(String containerId, String format) {
        ProcessResult result = execute(List.of("docker", "inspect", "--format", format, containerId),
                Duration.ofSeconds(30), Set.of());
        if (!result.successful()) {
            throw new IllegalStateException("Docker container is not available: " + containerId + "\n"
                    + result.output());
        }
        return result.standardOutput().trim();
    }

    public ProcessResult startAndAttach(String containerId, Duration timeout, Set<String> secrets) {
        return execute(List.of("docker", "start", "--attach", containerId), timeout, secrets);
    }

    public int waitForContainer(String containerId, Duration timeout) {
        ProcessResult result = execute(List.of("docker", "wait", containerId), timeout, Set.of());
        if (!result.successful()) {
            throw new IllegalStateException("docker wait failed for " + containerId + ": " + result.output());
        }
        try {
            return Integer.parseInt(result.standardOutput().trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("docker wait returned an invalid exit code: " + result.output(), e);
        }
    }

    public String logs(String containerId) {
        ProcessResult result = execute(List.of("docker", "logs", containerId), Duration.ofSeconds(30), Set.of());
        return result.output();
    }

    public void removeForce(String containerId) {
        ProcessResult result = execute(List.of("docker", "rm", "--force", containerId), Duration.ofSeconds(30), Set.of());
        if (!result.successful() && !result.output().toLowerCase().contains("no such container")) {
            throw new IllegalStateException("docker rm failed for " + containerId + ": " + result.output());
        }
    }

    public ProcessResult removeContainer(String containerName, boolean force) {
        List<String> command = force
                ? List.of("docker", "rm", "-f", containerName)
                : List.of("docker", "rm", containerName);
        return execute(command, Duration.ofSeconds(30), Set.of());
    }

    public boolean imageExists(String imageReference) {
        return execute(List.of("docker", "image", "inspect", imageReference), Duration.ofSeconds(20), Set.of())
                .successful();
    }

    public ProcessResult execute(List<String> command, Duration timeout, Set<String> secrets) {
        return processExecutor.execute(new ProcessRequest(command, Path.of("."), Map.of(), timeout, secrets));
    }

    public record DockerInfo(String serverVersion, String platform) {
    }
}
