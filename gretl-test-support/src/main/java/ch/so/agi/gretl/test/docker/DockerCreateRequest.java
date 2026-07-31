package ch.so.agi.gretl.test.docker;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DockerCreateRequest(
        String imageId,
        String containerName,
        List<String> command,
        Map<String, String> environment,
        List<ContainerMount> mounts,
        boolean networkNone,
        boolean readOnlyRootFilesystem,
        String workingDirectory,
        String user) {
    public DockerCreateRequest {
        if (imageId == null || !imageId.matches("sha256:[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Docker image ID must be an immutable sha256 reference: " + imageId);
        }
        if (containerName == null || !containerName.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")) {
            throw new IllegalArgumentException("Invalid Docker container name: " + containerName);
        }
        command = List.copyOf(Objects.requireNonNull(command, "command must not be null"));
        environment = Map.copyOf(Objects.requireNonNull(environment, "environment must not be null"));
        mounts = List.copyOf(Objects.requireNonNull(mounts, "mounts must not be null"));
        if (!networkNone) {
            throw new IllegalArgumentException("The offline executor only accepts networkNone=true");
        }
        if (workingDirectory == null || !workingDirectory.startsWith("/")) {
            throw new IllegalArgumentException("workingDirectory must be absolute");
        }
        user = user == null ? "" : user;
    }
}
