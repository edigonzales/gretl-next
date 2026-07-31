package ch.so.agi.gretl.test.docker;

import ch.so.agi.gretl.test.process.ProcessExecutor;
import ch.so.agi.gretl.test.process.ProcessRequest;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ContainerUserResolver {
    private final ProcessExecutor processExecutor;

    public ContainerUserResolver() {
        this(new ProcessExecutor());
    }

    public ContainerUserResolver(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public Optional<String> resolve() {
        return resolveFromOverride().or(this::resolvePosixUser);
    }

    public Optional<String> resolveFromOverride() {
        String value = System.getProperty("gretl.test.runtimeImage.user");
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    public Optional<String> resolvePosixUser() {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase();
        // Docker Desktop runs the container in a VM and does not require the
        // macOS host UID. Passing that UID can prevent Gradle's native
        // services from loading; Linux bind mounts retain the host UID/GID.
        if (!operatingSystem.contains("linux")) {
            return Optional.empty();
        }
        try {
            ProcessRequest uidRequest = new ProcessRequest(
                    List.of("id", "-u"), Path.of("."), Map.of(), Duration.ofSeconds(5), Set.of());
            ProcessRequest gidRequest = new ProcessRequest(
                    List.of("id", "-g"), Path.of("."), Map.of(), Duration.ofSeconds(5), Set.of());
            var uid = processExecutor.execute(uidRequest);
            var gid = processExecutor.execute(gidRequest);
            if (uid.successful() && gid.successful()
                    && uid.standardOutput().trim().matches("[0-9]+")
                    && gid.standardOutput().trim().matches("[0-9]+")) {
                return Optional.of(uid.standardOutput().trim() + ":" + gid.standardOutput().trim());
            }
        } catch (RuntimeException ignored) {
            // Windows and restricted test runners simply use the image user.
        }
        return Optional.empty();
    }
}
