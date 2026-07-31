package ch.so.agi.gretl.test.docker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** A deliberately small, typed description of a Docker mount. */
public record ContainerMount(Path hostPath, String containerPath, MountAccess access, MountType type) {
    public ContainerMount {
        hostPath = Objects.requireNonNull(hostPath, "hostPath must not be null")
                .toAbsolutePath().normalize();
        if (!Files.exists(hostPath)) {
            throw new IllegalArgumentException("Mount source does not exist: " + hostPath);
        }
        containerPath = Objects.requireNonNull(containerPath, "containerPath must not be null");
        if (!Path.of(containerPath).isAbsolute()) {
            throw new IllegalArgumentException("Mount target must be absolute: " + containerPath);
        }
        access = Objects.requireNonNull(access, "access must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
    }

    public static ContainerMount readOnlyBind(Path host, String target) {
        return new ContainerMount(host, target, MountAccess.READ_ONLY, MountType.BIND);
    }

    public static ContainerMount readWriteBind(Path host, String target) {
        return new ContainerMount(host, target, MountAccess.READ_WRITE, MountType.BIND);
    }

    /** Host path is ignored by Docker for a tmpfs mount, but retained for a stable value object. */
    public static ContainerMount tmpfs(String target) {
        return new ContainerMount(Path.of("/"), target, MountAccess.READ_WRITE, MountType.TMPFS);
    }

    public enum MountAccess { READ_ONLY, READ_WRITE }

    public enum MountType { BIND, TMPFS }
}
