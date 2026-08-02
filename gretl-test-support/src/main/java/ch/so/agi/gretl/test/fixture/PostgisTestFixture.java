package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.util.Map;
import java.util.Optional;

public final class PostgisTestFixture implements TestFixture {
    public static final String IMAGE = "postgis/postgis:16-3.4@sha256:44126d872ac91993766c341e369c539e8196614321765d36a6f1bab0419a5fa5";
    private final PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
            DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("gretl")
            .withUsername("gretl_user")
            .withPassword("gretl_password")
            .withNetworkAliases("postgis");
    private TestFixtureNetwork network;
    private boolean closed;

    @Override public TestFixtureType type() { return TestFixtureType.POSTGIS; }
    @Override public synchronized void start(TestFixtureNetwork network) {
        if (closed) throw new IllegalStateException("PostGIS fixture is closed");
        if (this.network != null) return;
        this.network = network;
        container.withNetwork(network.testcontainersNetwork()).start();
        try (var connection = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("create extension if not exists postgis");
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Could not initialize PostGIS extension", e);
        }
    }
    @Override public boolean isRunning() { return container.isRunning(); }
    @Override public synchronized TestFixtureLease acquire(TestJobExecutionIdentity identity) {
        if (!isRunning()) throw new IllegalStateException("PostGIS fixture is not running");
        String schema = "gretl_" + identity.shortToken().replace('-', '_');
        createSchema(schema);
        return new PostgisTestFixtureLease(this, identity.namespace(), schema,
                container.getUsername(), container.getPassword(), container.getDatabaseName());
    }

    TestFixtureEndpointView endpoint(String schema, String username, String password, String database,
                                     TestJobExecutionTarget target) {
        boolean runtime = target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE;
        String url = runtime ? "jdbc:postgresql://postgis:5432/" + database + "?currentSchema=" + schema + ",public"
                : container.getJdbcUrl() + (container.getJdbcUrl().contains("?") ? "&" : "?")
                + "currentSchema=" + schema + ",public";
        return new TestFixtureEndpointView(Map.of(
                "jdbcUrl", TestFixtureValue.publicValue(url),
                "username", TestFixtureValue.publicValue(username),
                "password", TestFixtureValue.secretValue(password),
                "database", TestFixtureValue.publicValue(database),
                "schema", TestFixtureValue.publicValue(schema)),
                runtime ? Optional.of(network.dockerNetworkId()) : Optional.empty());
    }

    void dropSchema(String schema) {
        try (var connection = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("drop schema if exists \"" + quoteIdentifier(schema) + "\" cascade");
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Could not clean PostGIS fixture lease", e);
        }
    }

    private void createSchema(String schema) {
        try (var connection = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("create schema \"" + quoteIdentifier(schema) + "\"");
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Could not create PostGIS lease schema", e);
        }
    }

    private static String quoteIdentifier(String value) {
        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("Invalid SQL identifier");
        return value.replace("\"", "\"\"");
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (container.isRunning()) container.stop();
    }
}
