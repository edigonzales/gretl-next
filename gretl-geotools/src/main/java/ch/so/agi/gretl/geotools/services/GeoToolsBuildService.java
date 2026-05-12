package ch.so.agi.gretl.geotools.services;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Build-wide coordination point for GeoTools tasks.
 *
 * <p>The service is intentionally small: it lets Gradle see that GeoTools tasks
 * share a constrained resource, while classpath isolation is handled by the
 * Worker API.</p>
 */
public abstract class GeoToolsBuildService implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    @Override
    public void close() {
        // Reserved for future GeoTools-specific resources.
    }
}
