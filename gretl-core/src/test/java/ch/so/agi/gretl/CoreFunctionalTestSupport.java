package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.io.TempDir;
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
import java.sql.Statement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class CoreFunctionalTestSupport {

    @TempDir
    Path projectDir;

    BuildResult run(String... arguments) {
        return GretlBuildExecutors.current().run(projectDir, arguments);
    }

    BuildResult runAndFail(String... arguments) {
        return GretlBuildExecutors.current().runAndFail(projectDir, arguments);
    }

    void writeSettings() throws IOException {
        GretlTestProjectSettings.write(projectDir, "core-test");
    }

    void writeBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

    void writeKotlinBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), content, StandardCharsets.UTF_8);
    }

    Path copyResource(String resourcePath, String targetPath) throws IOException, URISyntaxException {
        Path target = projectDir.resolve(targetPath);
        Files.createDirectories(target.getParent());
        Files.copy(resourcePath(resourcePath), target, StandardCopyOption.REPLACE_EXISTING);
        return target;
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

    Path resourcePath(String resourcePath) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing test resource: " + resourcePath);
        }
        return Path.of(resource.toURI());
    }

    void createColorTable(Path database) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("create table if not exists colors (id integer primary key, name text)");
        }
    }

    void createFruitTable(Path database) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("create table if not exists fruits (id integer primary key, name text)");
        }
    }

    void insertColor(Path database, int id, String name) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("insert into colors (id, name) values (" + id + ", '" + name + "')");
        }
    }

    void insertFruit(Path database, int id, String name) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("insert into fruits (id, name) values (" + id + ", '" + name + "')");
        }
    }

    String scalar(Path database, String sql) throws Exception {
        try (Connection connection = sqlite(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    int scalarInt(Path database, String sql) throws Exception {
        try (Connection connection = sqlite(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    Connection sqlite(Path path) throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
    }

}
