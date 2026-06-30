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

    @Test
    void exportsToExistingPostgisTableWithJdbcPath() throws Exception {
        writeSettings();
        createPointTargetTable("duckdbexecutor_pgexport");
        Files.writeString(projectDir.resolve("export.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.points AS
                SELECT 1::INTEGER AS id, 'first'::VARCHAR AS name, ST_GeomFromText('POINT(1 2)') AS geom
                UNION ALL
                SELECT 2::INTEGER AS id, 'second'::VARCHAR AS name, ST_GeomFromText('POINT(3 4)') AS geom;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('exportPostgis', DuckDbSqlExecutor) {
                    inMemoryDatabase()
                    installExtensions true

                    targets {
                        postgres('out') {
                            database pgUrl, pgUser, pgPass
                        }
                    }

                    sqlFiles 'export.sql'

                    exports {
                        postgres('points') {
                            target = 'out'
                            query = 'SELECT * FROM result.points ORDER BY id'
                            table = 'duckdbexecutor_pgexport.points'
                            mode = 'truncate'
                            geometry('geom') {
                                srid = 2056
                                type = 'POINT'
                            }
                        }
                    }
                }
                """));

        BuildResult result = run("exportPostgis");

        assertEquals(2, count("SELECT count(*) FROM duckdbexecutor_pgexport.points"));
        assertEquals("2056", scalar("SELECT ST_SRID(geom)::text FROM duckdbexecutor_pgexport.points WHERE id = 1"));
        assertEquals("POINT(1 2)", scalar("SELECT ST_AsText(geom) FROM duckdbexecutor_pgexport.points WHERE id = 1"));
        assertFalse(result.getOutput().contains(POSTGIS.getPassword()));
        assertFalse(result.getOutput().contains("CREATE SECRET"));
    }

    @Test
    void createsPostgisTableWithJdbcPath() throws Exception {
        writeSettings();
        createOrReplaceSchema("duckdbexecutor_pgcreate");
        Files.writeString(projectDir.resolve("create_export.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.points AS
                SELECT 5::INTEGER AS id, ST_GeomFromText('POINT(5 6)') AS geom;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('createPostgis', DuckDbSqlExecutor) {
                    inMemoryDatabase()
                    installExtensions true

                    targets {
                        postgres('out') {
                            database pgUrl, pgUser, pgPass
                        }
                    }

                    sqlFiles 'create_export.sql'

                    exports {
                        postgres('createdPoints') {
                            target = 'out'
                            query = 'SELECT * FROM result.points'
                            table = 'duckdbexecutor_pgcreate.points'
                            mode = 'replace'
                            create = true
                            geometry('geom') {
                                srid = 2056
                                type = 'POINT'
                            }
                        }
                    }
                }
                """));

        run("createPostgis");

        assertEquals(1, count("SELECT count(*) FROM duckdbexecutor_pgcreate.points"));
        assertEquals("POINT(5 6)", scalar("SELECT ST_AsText(geom) FROM duckdbexecutor_pgcreate.points"));
        assertEquals("POINT", scalar("""
                SELECT type
                FROM geometry_columns
                WHERE f_table_schema = 'duckdbexecutor_pgcreate'
                  AND f_table_name = 'points'
                  AND f_geometry_column = 'geom'
                """));
    }

    @Test
    void supportsNativeDuckDbScalarExportToPostgres() throws Exception {
        writeSettings();
        createScalarTargetTable("duckdbexecutor_pgnative");
        Files.writeString(projectDir.resolve("native.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.records AS
                SELECT 1::INTEGER AS id, 'native'::VARCHAR AS name;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('nativePostgis', DuckDbSqlExecutor) {
                    inMemoryDatabase()
                    installExtensions true

                    targets {
                        postgres('out') {
                            database pgUrl, pgUser, pgPass
                        }
                    }

                    sqlFiles 'native.sql'

                    exports {
                        postgres('values') {
                            target = 'out'
                            query = 'SELECT * FROM result.records'
                            table = 'duckdbexecutor_pgnative.records'
                            mode = 'append'
                            writePath = 'duckdb'
                        }
                    }
                }
                """));

        run("nativePostgis");

        assertEquals("native", scalar("SELECT name FROM duckdbexecutor_pgnative.records WHERE id = 1"));
    }

    @Test
    void exposesWritablePostgresTargetToUserSql() throws Exception {
        writeSettings();
        createOrReplaceSchema("duckdbexecutor_direct");
        Files.writeString(projectDir.resolve("direct.sql"), """
                CREATE TABLE out.duckdbexecutor_direct.records AS
                SELECT 7::INTEGER AS id, 'direct'::VARCHAR AS name;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('directTarget', DuckDbSqlExecutor) {
                    inMemoryDatabase()
                    installExtensions true

                    targets {
                        postgres('out') {
                            database pgUrl, pgUser, pgPass
                        }
                    }

                    sqlFiles 'direct.sql'
                }
                """));

        run("directTarget");

        assertEquals("direct", scalar("SELECT name FROM duckdbexecutor_direct.records WHERE id = 7"));
    }

    @Test
    void rollsBackJdbcPostgresExportWhenLaterExportFails() throws Exception {
        writeSettings();
        createScalarTargetTable("duckdbexecutor_pgrollback");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO duckdbexecutor_pgrollback.records VALUES (99, 'existing')");
        }
        Files.writeString(projectDir.resolve("rollback.sql"), """
                CREATE SCHEMA IF NOT EXISTS result;
                CREATE TABLE result.records AS
                SELECT 1::INTEGER AS id, 'new'::VARCHAR AS name;
                """, StandardCharsets.UTF_8);
        writeBuild(duckDbBuild("""
                tasks.register('rollbackPostgis', DuckDbSqlExecutor) {
                    inMemoryDatabase()
                    installExtensions true

                    targets {
                        postgres('out') {
                            database pgUrl, pgUser, pgPass
                        }
                    }

                    sqlFiles 'rollback.sql'

                    exports {
                        postgres('values') {
                            target = 'out'
                            query = 'SELECT * FROM result.records'
                            table = 'duckdbexecutor_pgrollback.records'
                            mode = 'append'
                        }
                        parquet('broken') {
                            query = 'SELECT * FROM missing_table'
                            file file('build/broken.parquet')
                            overwrite = true
                        }
                    }
                }
                """));

        runAndFail("rollbackPostgis");

        assertEquals(1, count("SELECT count(*) FROM duckdbexecutor_pgrollback.records"));
        assertEquals("existing", scalar("SELECT name FROM duckdbexecutor_pgrollback.records WHERE id = 99"));
        assertFalse(Files.exists(projectDir.resolve("build/broken.parquet")));
    }

    @Test
    void runsDocumentedPostgisTargetExample() throws Exception {
        createOrReplaceSchema("duckdbexecutor_example_target");
        copyTree(examplePath("postgis-target"), projectDir);

        run("exportToPostgis", "-PpgSchema=duckdbexecutor_example_target");

        assertEquals(2, count("SELECT count(*) FROM duckdbexecutor_example_target.duckdb_executor_points"));
        assertEquals("POINT", scalar("""
                SELECT type
                FROM geometry_columns
                WHERE f_table_schema = 'duckdbexecutor_example_target'
                  AND f_table_name = 'duckdb_executor_points'
                  AND f_geometry_column = 'geom'
                """));
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

    private void createPointTargetTable(String schema) throws Exception {
        createOrReplaceSchema(schema);
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + schema + ".points("
                    + "id integer primary key, name text, geom geometry(Point, 2056))");
        }
    }

    private void createScalarTargetTable(String schema) throws Exception {
        createOrReplaceSchema(schema);
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + schema + ".records("
                    + "id integer primary key, name text)");
        }
    }

    private void copyTestDuckdbGpkg(String target) throws Exception {
        Path source = Path.of("src/test/resources/fixtures/duckdb/data/ch.so.afu.abbaustellen.gpkg");
        Path destination = projectDir.resolve(target);
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path examplePath(String name) {
        Path fromModule = Path.of("../docs/examples/duckdb-sql-executor", name).normalize();
        if (Files.isDirectory(fromModule)) {
            return fromModule;
        }
        return Path.of("docs/examples/duckdb-sql-executor", name).normalize();
    }

    private void copyTree(Path source, Path target) throws Exception {
        try (var stream = Files.walk(source)) {
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
