package ch.so.agi.gretl.test.docker;

import ch.so.agi.gretl.test.process.ProcessExecutor;
import ch.so.agi.gretl.test.process.ProcessRequest;
import ch.so.agi.gretl.test.process.ProcessResult;

import java.nio.file.Path;
import java.time.Duration;
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

    private String inspectFormat(String imageReference, String format) {
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
