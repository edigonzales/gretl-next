package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ch.so.agi.gretl.testkit.GretlBuildExecutors;
import ch.so.agi.gretl.testkit.GretlTestProjectSettings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
abstract class PostgisIntegrationTestSupport {

    @Container
    static final PostgreSQLContainer<?> POSTGIS =
            ((PostgreSQLContainer<?>) new PostgisContainerProvider().newInstance())
                    .withDatabaseName("gretl")
                    .withUsername("gretl")
                    .withPassword("gretl");

    @TempDir
    Path projectDir;

    BuildResult run(String... arguments) {
        return GretlBuildExecutors.current().run(projectDir, appendPostgresArguments(arguments));
    }

    BuildResult runAndFail(String... arguments) {
        return GretlBuildExecutors.current().runAndFail(projectDir, appendPostgresArguments(arguments));
    }

    void writeSettings() throws IOException {
        GretlTestProjectSettings.write(projectDir, "postgis-test");
    }

    void writeBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

    void copyResourceTree(String resourcePath, Path target) throws IOException, URISyntaxException {
        Path source = resourcePath(resourcePath);
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    Connection pg() throws SQLException {
        return DriverManager.getConnection(POSTGIS.getJdbcUrl(), POSTGIS.getUsername(), POSTGIS.getPassword());
    }

    void createOrReplaceSchema(String schemaName) throws SQLException {
        try (Connection connection = pg(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
            statement.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
            statement.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
            statement.execute("CREATE SCHEMA " + schemaName);
        }
    }

    int count(String sql) throws SQLException {
        try (Connection connection = pg();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    String scalar(String sql) throws SQLException {
        try (Connection connection = pg();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    Set<String> stringSet(String sql) throws SQLException {
        Set<String> values = new HashSet<>();
        try (Connection connection = pg();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
        }
        return values;
    }

    void createSqlExecutorAlbumTables(String schemaName) throws SQLException {
        try (Connection connection = pg(); Statement statement = connection.createStatement()) {
            statement.execute(albumTableDdl(schemaName, "src"));
            statement.execute(albumTableDdl(schemaName, "dest"));
        }
    }

    int prepareDb2DbChainTables(String schemaName) throws SQLException {
        try (Connection connection = pg(); Statement statement = connection.createStatement()) {
            statement.execute(albumTableDdl(schemaName, "src"));
            statement.execute(albumTableDdl(schemaName, "dest"));
            statement.execute(albumTableDdl(schemaName, "intermediate"));
        }
        insertAlbumRows(schemaName, "src", 4);
        return 4;
    }

    void insertAlbumRows(String schemaName, String tableSuffix, int count) throws SQLException {
        try (Connection connection = pg();
             var statement = connection.prepareStatement(
                     "INSERT INTO " + schemaName + ".albums_" + tableSuffix + " VALUES (?,?,?,?,?)")) {
            String[] row = {"Exodus", "Andy Hunter", "7/9/2002", "Sparrow Records", "CD"};
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < row.length; j++) {
                    statement.setString(j + 1, row[j]);
                }
                statement.executeUpdate();
            }
        }
    }

    private Path resourcePath(String resourcePath) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing test resource: " + resourcePath);
        }
        return Path.of(resource.toURI());
    }

    private String albumTableDdl(String schemaName, String suffix) {
        return "CREATE TABLE " + schemaName + ".albums_" + suffix + "("
                + "title text, artist text, release_date text, publisher text, media_type text)";
    }

    private String[] appendPostgresArguments(String[] arguments) {
        String[] result = new String[arguments.length + 4];
        System.arraycopy(arguments, 0, result, 0, arguments.length);
        result[arguments.length] = "-PpgUrl=" + POSTGIS.getJdbcUrl();
        result[arguments.length + 1] = "-PpgUser=" + POSTGIS.getUsername();
        result[arguments.length + 2] = "-PpgPass=" + POSTGIS.getPassword();
        result[arguments.length + 3] = "--stacktrace";
        return result;
    }
}
