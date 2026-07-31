package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.docker.ContainerMount;

import java.time.Duration;
import java.util.Map;

public record OfflineImageRunOptions(
        boolean networkNone,
        boolean gradleOffline,
        boolean noDaemon,
        boolean readOnlyRootFilesystem,
        boolean freshWritableGradleHome,
        boolean useBundledReadOnlyCache,
        Map<String, String> environment,
        Duration timeout) {

    public OfflineImageRunOptions {
        environment = Map.copyOf(environment == null ? Map.of() : environment);
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    public static OfflineImageRunOptions strict() {
        return new OfflineImageRunOptions(true, true, true, true, true, false, Map.of(), Duration.ofMinutes(5));
    }

    public OfflineImageRunOptions withoutBundledCache() {
        return new OfflineImageRunOptions(networkNone, gradleOffline, noDaemon, readOnlyRootFilesystem,
                freshWritableGradleHome, false, environment, timeout);
    }

    public OfflineImageRunOptions withEnvironment(String key, String value) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>(environment);
        values.put(key, value);
        return new OfflineImageRunOptions(networkNone, gradleOffline, noDaemon, readOnlyRootFilesystem,
                freshWritableGradleHome, useBundledReadOnlyCache, values, timeout);
    }

    public OfflineImageRunOptions withTimeout(Duration value) {
        return new OfflineImageRunOptions(networkNone, gradleOffline, noDaemon, readOnlyRootFilesystem,
                freshWritableGradleHome, useBundledReadOnlyCache, environment, value);
    }
}
