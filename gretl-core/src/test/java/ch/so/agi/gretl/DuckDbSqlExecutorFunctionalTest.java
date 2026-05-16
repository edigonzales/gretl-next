package ch.so.agi.gretl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckDbSqlExecutorFunctionalTest extends CoreFunctionalTestSupport {
    @Test
    void executesGpkgSourceAndExportsGpkg() throws Exception {
        writeSettings();
        copyResource("original-gretl/duckdb/data/ch.so.afu.abbaustellen.gpkg", "data/abbaustellen.gpkg");
        Files.writeString(projectDir.resolve("analyse.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.abbaustelle AS
                SELECT * FROM input.abbaustelle LIMIT 3;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('analyse', DuckDbSqlExecutor) {
                    database file('build/work.duckdb')
                    installExtensions true

                    sources {
                        gpkg('input') {
                            file file('data/abbaustellen.gpkg')
                            layer('abbaustelle')
                        }
                    }

                    sqlFiles 'analyse.sql'

                    exports {
                        gpkg('result') {
                            query = 'SELECT * FROM result.abbaustelle'
                            file file('build/result.gpkg')
                            layer = 'analyse'
                            overwrite = true
                        }
                    }
                }
                """));

        run("analyse");

        assertTrue(Files.exists(projectDir.resolve("build/work.duckdb")));
        assertEquals(3, sqliteInt(projectDir.resolve("build/result.gpkg"), "SELECT count(*) FROM analyse"));
    }

    @Test
    void supportsInMemoryDatabaseAndParquetExport() throws Exception {
        writeSettings();
        copyResource("original-gretl/duckdb/data/ch.so.afu.abbaustellen.gpkg", "data/abbaustellen.gpkg");
        Files.writeString(projectDir.resolve("stats.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.stats AS
                SELECT count(*)::INTEGER AS feature_count FROM input.abbaustelle;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('stats', DuckDbSqlExecutor) {
                    inMemoryDatabase()
                    installExtensions true

                    sources {
                        gpkg('input') {
                            file file('data/abbaustellen.gpkg')
                            layer('abbaustelle')
                        }
                    }

                    sqlFiles 'stats.sql'

                    exports {
                        parquet('stats') {
                            query = 'SELECT * FROM result.stats'
                            file file('build/stats.parquet')
                            overwrite = true
                        }
                    }
                }
                """));

        run("stats");

        assertFalse(Files.exists(projectDir.resolve("build/work.duckdb")));
        assertTrue(duckdbInt("SELECT feature_count FROM read_parquet('%s')"
                .formatted(projectDir.resolve("build/stats.parquet").toAbsolutePath())) > 0);
    }

    @Test
    void supportsSqlParameterSets() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("insert.sql"), """
                CREATE TABLE IF NOT EXISTS colors (id INTEGER, name VARCHAR);
                INSERT INTO colors VALUES (${id}, ${name});
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('insertRows', DuckDbSqlExecutor) {
                    database file('build/params.duckdb')
                    sqlFiles 'insert.sql'
                    sqlParameterSets([id: 1, name: "'red'"], [id: 2, name: "'blue'"])
                }
                """));

        run("insertRows");

        assertEquals("red,blue", duckdbString(projectDir.resolve("build/params.duckdb"),
                "SELECT string_agg(name, ',' ORDER BY id) FROM colors"));
    }

    @Test
    void leavesNoExportFileOrTempExportOnFailure() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("fail.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.stats AS SELECT 1 AS n;
                SELECT missing_column FROM missing_table;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('failTask', DuckDbSqlExecutor) {
                    database file('build/fail.duckdb')
                    sqlFiles 'fail.sql'
                    exports {
                        parquet('stats') {
                            query = 'SELECT * FROM result.stats'
                            file file('build/stats.parquet')
                            overwrite = true
                        }
                    }
                }
                """));

        runAndFail("failTask");

        assertFalse(Files.exists(projectDir.resolve("build/stats.parquet")));
        if (Files.isDirectory(projectDir.resolve("build"))) {
            try (Stream<Path> files = Files.list(projectDir.resolve("build"))) {
                assertEquals(0, files.filter(path -> path.getFileName().toString().contains(".tmp.parquet")).count());
            }
        }
    }

    @Test
    void runsDocumentedGpkgOnlyExample() throws Exception {
        copyTree(examplePath("gpkg-only"), projectDir);

        run("analyse");

        assertTrue(Files.exists(projectDir.resolve("build/analyse.gpkg")));
        assertTrue(Files.exists(projectDir.resolve("build/analyse.parquet")));
        assertEquals(5, duckdbInt("SELECT count(*) FROM read_parquet('%s')"
                .formatted(projectDir.resolve("build/analyse.parquet").toAbsolutePath())));
    }

    private String duckDbBuild(String taskDefinition) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.DuckDbSqlExecutor

                %s
                """.formatted(taskDefinition);
    }

    private Path examplePath(String name) {
        Path fromModule = Path.of("../docs/examples/duckdb-sql-executor", name).normalize();
        if (Files.isDirectory(fromModule)) {
            return fromModule;
        }
        return Path.of("docs/examples/duckdb-sql-executor", name).normalize();
    }

    private void copyTree(Path source, Path target) throws Exception {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private int duckdbInt(String sql) throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private String duckdbString(Path database, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:" + database.toAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    private int sqliteInt(Path database, String sql) throws Exception {
        try (Connection connection = sqlite(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
