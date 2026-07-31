package ch.so.agi.gretl.test.runtime;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record RuntimeImageRunOptions(
        Optional<String> dockerNetwork,
        boolean networkDisabled,
        Map<String, String> containerEnvironment,
        Map<Path, String> readOnlyMounts,
        Map<Path, String> readWriteMounts) {

    public RuntimeImageRunOptions {
        dockerNetwork = dockerNetwork == null ? Optional.empty() : dockerNetwork;
        if (dockerNetwork.isPresent() && networkDisabled) {
            throw new IllegalArgumentException("dockerNetwork and networkDisabled cannot both be set");
        }
        if (dockerNetwork.isEmpty() && !networkDisabled) {
            throw new IllegalArgumentException("one of dockerNetwork or networkDisabled must be set");
        }
        containerEnvironment = Map.copyOf(containerEnvironment == null ? Map.of() : containerEnvironment);
        readOnlyMounts = normalizeMounts(readOnlyMounts);
        readWriteMounts = normalizeMounts(readWriteMounts);
        if (!java.util.Collections.disjoint(readOnlyMounts.values(), readWriteMounts.values())) {
            throw new IllegalArgumentException("read-only and read-write mount targets must be distinct");
        }
    }

    public static RuntimeImageRunOptions offline() {
        return new RuntimeImageRunOptions(Optional.empty(), true, Map.of(), Map.of(), Map.of());
    }

    public static RuntimeImageRunOptions onNetwork(String network) {
        if (network == null || network.isBlank()) {
            throw new IllegalArgumentException("network must not be blank");
        }
        return new RuntimeImageRunOptions(Optional.of(network), false, Map.of(), Map.of(), Map.of());
    }

    public RuntimeImageRunOptions withEnvironment(String name, String value) {
        Map<String, String> values = new HashMap<>(containerEnvironment);
        values.put(name, value);
        return new RuntimeImageRunOptions(dockerNetwork, networkDisabled, values, readOnlyMounts, readWriteMounts);
    }

    public RuntimeImageRunOptions withReadOnlyMount(Path host, String container) {
        Map<Path, String> mounts = new HashMap<>(readOnlyMounts);
        mounts.put(host.toAbsolutePath().normalize(), container);
        return new RuntimeImageRunOptions(dockerNetwork, networkDisabled, containerEnvironment, mounts, readWriteMounts);
    }

    public RuntimeImageRunOptions withReadWriteMount(Path host, String container) {
        Map<Path, String> mounts = new HashMap<>(readWriteMounts);
        mounts.put(host.toAbsolutePath().normalize(), container);
        return new RuntimeImageRunOptions(dockerNetwork, networkDisabled, containerEnvironment, readOnlyMounts, mounts);
    }

    private static Map<Path, String> normalizeMounts(Map<Path, String> mounts) {
        Map<Path, String> normalized = new HashMap<>();
        if (mounts != null) {
            mounts.forEach((host, container) -> {
                if (host == null || container == null || !Path.of(container).isAbsolute()) {
                    throw new IllegalArgumentException("mount host and absolute container target are required");
                }
                normalized.put(host.toAbsolutePath().normalize(), container);
            });
        }
        return Map.copyOf(normalized);
    }
}
