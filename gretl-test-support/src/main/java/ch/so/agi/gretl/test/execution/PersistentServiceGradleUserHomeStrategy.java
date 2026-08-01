package ch.so.agi.gretl.test.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Uses a caller-owned service home that remains available between jobs. */
public final class PersistentServiceGradleUserHomeStrategy implements GradleUserHomeStrategy {
    private final Path serviceHome;

    public PersistentServiceGradleUserHomeStrategy(Path serviceHome) {
        this.serviceHome = serviceHome.toAbsolutePath().normalize();
    }

    @Override
    public GradleUserHomeHandle prepare(Path projectDirectory, RuntimeExecutionMode executionMode) {
        if (executionMode != RuntimeExecutionMode.SERVICE) {
            throw new IllegalArgumentException("Persistent service Gradle home requires SERVICE execution mode");
        }
        try {
            Files.createDirectories(serviceHome);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create persistent service Gradle user home: " + serviceHome, e);
        }
        return new GradleUserHomeHandle() {
            @Override
            public Path path() {
                return serviceHome;
            }

            @Override
            public void close() {
                // The service lifecycle owns this home and cleans it up when the service ends.
            }
        };
    }
}
