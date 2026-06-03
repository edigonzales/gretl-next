package ch.so.agi.gretl.services;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Serializes INTERLIS tool executions that rely on JVM-global logging and file
 * listener state.
 */
public abstract class InterlisBuildService implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    @Override
    public void close() {
        // Reserved for future INTERLIS-wide shared resources.
    }
}
