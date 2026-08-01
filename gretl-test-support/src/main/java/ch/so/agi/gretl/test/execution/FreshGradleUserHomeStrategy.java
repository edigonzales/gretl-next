package ch.so.agi.gretl.test.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Provides a clean, project-local temporary home for one-shot image tests. */
public final class FreshGradleUserHomeStrategy implements GradleUserHomeStrategy {
    @Override
    public GradleUserHomeHandle prepare(Path projectDirectory, RuntimeExecutionMode executionMode) {
        if (executionMode == null) {
            throw new IllegalArgumentException("executionMode must not be null");
        }
        try {
            Path parent = projectDirectory.toAbsolutePath().normalize().getParent();
            Path home = Files.createTempDirectory(parent, "gretl-runtime-gradle-home-");
            return new TemporaryGradleUserHomeHandle(home);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create a fresh Gradle user home for " + projectDirectory, e);
        }
    }

    private static final class TemporaryGradleUserHomeHandle implements GradleUserHomeHandle {
        private final Path path;

        private TemporaryGradleUserHomeHandle(Path path) {
            this.path = path;
        }

        @Override
        public Path path() {
            return path;
        }

        @Override
        public void close() {
            if (!Files.exists(path)) {
                return;
            }
            try (var paths = Files.walk(path)) {
                paths.sorted(Comparator.reverseOrder()).forEach(value -> {
                    try {
                        Files.deleteIfExists(value);
                    } catch (IOException e) {
                        // Cleanup must not hide the build result.
                    }
                });
            } catch (IOException e) {
                // Cleanup is best effort; the temporary path is unique to this run.
            }
        }
    }
}
