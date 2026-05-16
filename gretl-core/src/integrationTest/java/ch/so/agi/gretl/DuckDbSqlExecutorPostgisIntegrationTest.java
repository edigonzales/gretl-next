package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckDbSqlExecutorPostgisIntegrationTest extends PostgisIntegrationTestSupport {
    @Test
    void readsPostgisGeometryWithAutoDiscovery() throws Exception {
        writeSettings();
        createGemeindenTable("duckdbexecutor_geom");
        Files.writeString(projectDir.resolve("analyse.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.areas AS
                SELECT id, round(ST_Area(geom))::INTEGER AS area_m2
                FROM pub.gemeinden
                ORDER BY id;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('analyse', DuckDbSqlExecutor) {
                    database file('build/work.duckdb')
                    installExtensions true

                    sources {
                        postgres('pub') {
                            database pgUrl, pgUser, pgPass
                            table('duckdbexecutor_geom.gemeinden')
                        }
                    }

                    sqlFiles 'analyse.sql'

                    exports {
                        parquet('areas') {
                            query = 'SELECT * FROM result.areas'
                            file file('build/areas.parquet')
                            overwrite = true
                        }
                    }
                }
                """));

        run("analyse");

        assertEquals("100,400", duckdbString("SELECT string_agg(area_m2::VARCHAR, ',' ORDER BY id) "
                + "FROM read_parquet('%s')".formatted(projectDir.resolve("build/areas.parquet").toAbsolutePath())));
    }

    @Test
    void federatesPostgisAndGeoPackageSourcesWithoutLoggingPassword() throws Exception {
        writeSettings();
        createGemeindenTable("duckdbexecutor_federated");
        copyTestDuckdbGpkg("data/ch.so.afu.abbaustellen.gpkg");
        Files.writeString(projectDir.resolve("federated.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.summary AS
                SELECT p.id, count(*)::INTEGER AS gpkg_rows
                FROM pub.gemeinden p
                CROSS JOIN input.abbaustelle g
                GROUP BY p.id
                ORDER BY p.id;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('federated', DuckDbSqlExecutor) {
                    inMemoryDatabase()
                    installExtensions true

                    sources {
                        postgres('pub') {
                            database pgUrl, pgUser, pgPass
                            table('duckdbexecutor_federated.gemeinden')
                        }
                        gpkg('input') {
                            file file('data/ch.so.afu.abbaustellen.gpkg')
                            layer('abbaustelle')
                        }
                    }

                    sqlFiles 'federated.sql'

                    exports {
                        parquet('summary') {
                            query = 'SELECT * FROM result.summary'
                            file file('build/summary.parquet')
                            overwrite = true
                        }
                    }
                }
                """));

        BuildResult result = run("federated");

        assertTrue(duckdbInt("SELECT count(*) FROM read_parquet('%s')"
                .formatted(projectDir.resolve("build/summary.parquet").toAbsolutePath())) > 0);
        assertFalse(result.getOutput().contains(POSTGIS.getPassword()));
        assertFalse(result.getOutput().contains("CREATE SECRET"));
    }

    @Test
    void supportsGeometryOverrideExcludeColumn() throws Exception {
        writeSettings();
        createObjektTable("duckdbexecutor_override");
        Files.writeString(projectDir.resolve("override.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.objekt AS
                SELECT id, ST_GeometryType(flaeche)::VARCHAR AS geometry_type
                FROM pub.objekt;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('overrideGeom', DuckDbSqlExecutor) {
                    database file('build/override.duckdb')
                    installExtensions true

                    sources {
                        postgres('pub') {
                            database pgUrl, pgUser, pgPass
                            table('duckdbexecutor_override.objekt') {
                                geometry('labelpunkt') {
                                    include = false
                                }
                            }
                        }
                    }

                    sqlFiles 'override.sql'

                    exports {
                        parquet('objekt') {
                            query = 'SELECT * FROM result.objekt'
                            file file('build/objekt.parquet')
                            overwrite = true
                        }
                    }
                }
                """));

        run("overrideGeom");

        assertEquals("POLYGON", duckdbString("SELECT geometry_type FROM read_parquet('%s')"
                .formatted(projectDir.resolve("build/objekt.parquet").toAbsolutePath())));
    }

    private void createGemeindenTable(String schema) throws Exception {
        createOrReplaceSchema(schema);
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + schema + ".gemeinden("
                    + "id integer primary key, name text, geom geometry(MultiPolygon, 2056))");
            statement.execute("INSERT INTO " + schema + ".gemeinden VALUES "
                    + "(1, 'small', ST_Multi(ST_GeomFromText('POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))', 2056))),"
                    + "(2, 'large', ST_Multi(ST_GeomFromText('POLYGON((0 0, 20 0, 20 20, 0 20, 0 0))', 2056)))");
        }
    }

    private void createObjektTable(String schema) throws Exception {
        createOrReplaceSchema(schema);
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + schema + ".objekt("
                    + "id integer primary key, flaeche geometry(Polygon, 2056), labelpunkt geometry(Point, 2056))");
            statement.execute("INSERT INTO " + schema + ".objekt VALUES "
                    + "(1, ST_GeomFromText('POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))', 2056),"
                    + " ST_GeomFromText('POINT(5 5)', 2056))");
        }
    }

    private void copyTestDuckdbGpkg(String target) throws Exception {
        Path source = Path.of("src/test/resources/original-gretl/duckdb/data/ch.so.afu.abbaustellen.gpkg");
        Path destination = projectDir.resolve(target);
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private String duckDbBuild(String taskDefinition) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.DuckDbSqlExecutor

                def pgUrl = providers.gradleProperty('pgUrl').get()
                def pgUser = providers.gradleProperty('pgUser').get()
                def pgPass = providers.gradleProperty('pgPass').get()

                %s
                """.formatted(taskDefinition);
    }

    private int duckdbInt(String sql) throws Exception {
        try (Connection connection = duckdbMemory();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private String duckdbString(String sql) throws Exception {
        try (Connection connection = duckdbMemory();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    private Connection duckdbMemory() throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        return DriverManager.getConnection("jdbc:duckdb:");
    }
}
