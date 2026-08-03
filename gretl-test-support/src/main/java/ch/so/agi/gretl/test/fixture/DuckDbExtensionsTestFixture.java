package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/** Exposes the already-installed GRETL DuckDB extension directory. */
public final class DuckDbExtensionsTestFixture implements TestFixture {
    private boolean started;
    private boolean closed;
    private Path extensionDirectory;

    @Override public TestFixtureType type() { return TestFixtureType.DUCKDB_EXTENSIONS; }
    @Override public synchronized boolean requiresDockerNetwork() { return false; }

    @Override public synchronized void start(TestFixtureStartContext context) {
        if (closed) throw new IllegalStateException("DuckDB extensions fixture is closed");
        if (started) return;
        String configured = System.getProperty("gretl.test.duckdbExtensionDirectory", "");
        if (configured.isBlank()) {
            extensionDirectory = Path.of("/opt/gretl/duckdb-extensions");
        } else {
            extensionDirectory = Path.of(configured).toAbsolutePath().normalize();
            if (!Files.isDirectory(extensionDirectory)) {
                throw new IllegalArgumentException("Configured DuckDB extension directory does not exist: " + extensionDirectory);
            }
            boolean spatial = false;
            try (var paths = Files.walk(extensionDirectory)) {
                for (Path path : paths.toList()) {
                    if (path.getFileName().toString().equals("spatial.duckdb_extension")
                            && Files.isRegularFile(path) && Files.size(path) > 0) {
                        spatial = true;
                        break;
                    }
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Cannot inspect DuckDB extension directory", e);
            }
            if (!spatial) throw new IllegalStateException("DuckDB extension directory has no spatial extension: " + extensionDirectory);
        }
        started = true;
    }
    @Override public synchronized boolean isRunning() { return started && !closed; }
    @Override public synchronized TestFixtureLease acquire(TestJobExecutionIdentity identity) {
        if (!isRunning()) throw new IllegalStateException("DuckDB extensions fixture is not running");
        return new DuckDbExtensionsFixtureLease(identity.namespace(), extensionDirectory);
    }
    @Override public synchronized void close() { closed = true; }

    static final class DuckDbExtensionsFixtureLease implements TestFixtureLease {
        private final String id;
        private final Path directory;
        private boolean closed;
        DuckDbExtensionsFixtureLease(String id, Path directory) { this.id = id; this.directory = directory; }
        @Override public String id() { return id; }
        @Override public TestFixtureType type() { return TestFixtureType.DUCKDB_EXTENSIONS; }
        @Override public synchronized TestFixtureEndpointView endpointView(TestJobExecutionTarget target) {
            if (closed) throw new IllegalStateException("DuckDB extensions lease is closed");
            String value = target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                    || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE
                    ? "/opt/gretl/duckdb-extensions" : directory.toString();
            return new TestFixtureEndpointView(Map.of("extensionDirectory", TestFixtureValue.publicValue(value)), Optional.empty());
        }
        @Override public synchronized boolean isHealthy() { return !closed; }
        @Override public synchronized void close() { closed = true; }
    }
}
