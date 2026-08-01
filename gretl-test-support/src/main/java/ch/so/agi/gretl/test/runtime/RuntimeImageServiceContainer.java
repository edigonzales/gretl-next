package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.process.ProcessExecutor;
import ch.so.agi.gretl.test.process.ProcessRequest;
import ch.so.agi.gretl.test.process.ProcessResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RuntimeImageServiceContainer implements AutoCloseable {
    private final DockerCli docker;
    private final ProcessExecutor processes;
    private final String containerId;
    private final String containerName;
    private final Path gradleUserHome;

    private RuntimeImageServiceContainer(DockerCli docker, ProcessExecutor processes, String containerId,
                                         String containerName, Path gradleUserHome) {
        this.docker = docker;
        this.processes = processes;
        this.containerId = containerId;
        this.containerName = containerName;
        this.gradleUserHome = gradleUserHome;
    }

    public static RuntimeImageServiceContainer start(
            RuntimeImageDescriptor image,
            Path jobsRoot,
            Path gradleUserHome,
            Optional<String> network,
            Optional<String> user) {
        return start(image, jobsRoot, gradleUserHome, network, user, Map.of());
    }

    public static RuntimeImageServiceContainer start(
            RuntimeImageDescriptor image,
            Path jobsRoot,
            Path gradleUserHome,
            Optional<String> network,
            Optional<String> user,
            Map<Path, String> additionalReadOnlyMounts) {
        image.verify();
        try {
            Files.createDirectories(jobsRoot);
            Files.createDirectories(gradleUserHome);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create service container mounts", e);
        }
        ProcessExecutor processes = new ProcessExecutor();
        DockerCli docker = new DockerCli(processes);
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String name = "gretl-service-" + runId;
        List<String> command = new ArrayList<>(List.of(
                "docker", "run", "-d", "--pull=never", "--name", name,
                "--label", "ch.so.agi.gretl.test=true",
                "--label", "ch.so.agi.gretl.test.run=" + runId,
                "--mount", "type=bind,src=" + jobsRoot.toAbsolutePath().normalize() + ",dst=/home/gradle/project",
                "--mount", "type=bind,src=" + gradleUserHome.toAbsolutePath().normalize() + ",dst=/home/gradle/.gradle",
                "--workdir", "/home/gradle/project",
                "--env", "GRADLE_USER_HOME=/home/gradle/.gradle"));
        additionalReadOnlyMounts.forEach((host, target) -> command.addAll(List.of(
                "--mount", "type=bind,src=" + host.toAbsolutePath().normalize() + ",dst=" + target + ",readonly")));
        user.ifPresent(value -> command.addAll(List.of("--user", value)));
        command.addAll(List.of("--entrypoint", "sleep"));
        network.ifPresent(value -> command.addAll(List.of("--network", value)));
        command.add(image.imageId());
        command.add("infinity");
        ProcessResult started = docker.execute(command, Duration.ofMinutes(1), Set.of());
        if (!started.successful()) {
            throw new IllegalStateException("Cannot start GRETL service container: " + started.output());
        }
        return new RuntimeImageServiceContainer(docker, processes, started.standardOutput().trim(), name, gradleUserHome);
    }

    public ProcessResult execGretl(Path relativeProjectDir, List<String> arguments) {
        return execGretl(relativeProjectDir, arguments, Set.of(), Duration.ofMinutes(15));
    }

    public ProcessResult execGretl(Path relativeProjectDir, List<String> arguments,
                                   Set<String> secrets, Duration timeout) {
        Path relative = relativeProjectDir.normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException("relativeProjectDir must stay below the service jobs root");
        }
        List<String> command = new ArrayList<>(List.of("docker", "exec", containerName, "gretl",
                "--project-dir=/home/gradle/project/" + relative));
        command.addAll(arguments);
        return docker.execute(command, timeout, secrets);
    }

    /** Returns the daemon status for this service container. */
    public ProcessResult gradleStatus() {
        return executeAdministrativeGradle(List.of("--status", "--console=plain"), Duration.ofMinutes(5));
    }

    /** Stops daemons in the service container without stopping the container. */
    public ProcessResult stopGradleDaemons() {
        return executeAdministrativeGradle(List.of("--stop"), Duration.ofMinutes(5));
    }

    public Set<Long> daemonPids() {
        ProcessResult result = gradleStatus();
        Set<Long> pids = new HashSet<>();
        for (String line : result.output().split("\\R")) {
            if (!line.contains("IDLE") && !line.contains("BUSY")) {
                continue;
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^\\s*([0-9]{2,})\\s+")
                    .matcher(line);
            if (matcher.find()) {
                pids.add(Long.parseLong(matcher.group(1)));
            }
        }
        return Set.copyOf(pids);
    }

    public boolean isRunning() {
        return docker.execute(List.of("docker", "inspect", "-f", "{{.State.Running}}", containerName),
                Duration.ofSeconds(20), Set.of()).successful();
    }

    public String containerId() {
        return containerId;
    }

    private ProcessResult executeAdministrativeGradle(List<String> arguments, Duration timeout) {
        List<String> command = new ArrayList<>(List.of("docker", "exec", containerName, "gradle"));
        command.addAll(arguments);
        return docker.execute(command, timeout, Set.of());
    }

    @Override
    public void close() {
        try {
            docker.removeContainer(containerName, true);
        } finally {
            try (var paths = Files.walk(gradleUserHome)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best effort cleanup.
                    }
                });
            } catch (IOException ignored) {
                // Best effort cleanup.
            }
        }
    }
}
