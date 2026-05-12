package ch.so.agi.gretl.services;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Build-wide coordination point for the core GRETL plugin.
 *
 * <p>The service intentionally does not own task implementation dependencies.
 * It gives Gradle a visible resource for lifecycle and future throttling
 * concerns without pretending to isolate plugin classpaths.</p>
 */
public abstract class CoreGretlBuildService implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    @Override
    public void close() {
        // Reserved for future core-wide resources such as shared caches.
    }
}
