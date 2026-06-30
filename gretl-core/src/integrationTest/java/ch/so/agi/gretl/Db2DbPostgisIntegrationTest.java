package ch.so.agi.gretl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Db2DbPostgisIntegrationTest extends PostgisIntegrationTestSupport {

    @Test
    void executesOriginalFetchSizeFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/db2db", projectDir.resolve("sql"));
        createOrReplaceSchema("db2dbtaskfetchsize");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE db2dbtaskfetchsize.source_data(
                        t_id integer, aint integer, adec decimal(7,1), atext varchar(40),
                        adate date, atimestamp timestamp, aboolean boolean,
                        geom_so geometry(POINT,2056))
                    """);
            statement.execute("""
                    CREATE TABLE db2dbtaskfetchsize.target_data(
                        t_id integer, aint integer, adec decimal(7,1), atext varchar(40),
                        adate date, atimestamp timestamp, aboolean boolean,
                        geom_so geometry(POINT,2056))
                    """);
            statement.execute("""
                    INSERT INTO db2dbtaskfetchsize.source_data
                    VALUES (1, 2, 3.4, 'abc', '2013-10-21', '2015-02-16T08:35:45', true,
                            ST_GeomFromText('POINT(2638000 1175250)', 2056))
                    """);
            statement.execute("""
                    INSERT INTO db2dbtaskfetchsize.source_data
                    VALUES (2, 33, 44.4, 'asdf', '2017-12-21', '2015-03-16T11:35:45', true,
                            ST_GeomFromText('POINT(2648000 1185250)', 2056))
                    """);
        }
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'sql/fetchSize.sql', 'db2dbtaskfetchsize.target_data', false
                    fetchSize 1
                }
                """));

        run("copyRows");

        assertEquals(2, count("select count(*) from db2dbtaskfetchsize.target_data"));
        assertEquals("2056", scalar("select distinct st_srid(geom_so)::text from db2dbtaskfetchsize.target_data"));
    }

    @Test
    void executesOriginalTaskChainFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/db2db", projectDir.resolve("sql"));
        createOrReplaceSchema("db2dbtaskchain");
        int expectedRows = prepareDb2DbChainTables("db2dbtaskchain");
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'sql/statementChain_AToB.sql', 'db2dbtaskchain.albums_intermediate', false
                    transfer 'sql/statementChain_BToA.sql', 'db2dbtaskchain.albums_dest', false
                    sqlParameters([srcTable: 'albums_src'])
                }
                """));

        run("copyRows");

        assertEquals(expectedRows, count("select count(*) from db2dbtaskchain.albums_dest"));
    }

    @Test
    void executesOriginalRelativePathFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/db2db", projectDir.resolve("sql"));
        createOrReplaceSchema("relativepath");
        int expectedRows = prepareDb2DbChainTables("relativepath");
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'sql/relativePath.sql', 'relativepath.albums_dest', false
                }
                """));

        run("copyRows");

        assertEquals(expectedRows, count("select count(*) from relativepath.albums_dest"));
    }

    @Test
    void executesOriginalDeleteAllRowsFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/db2db", projectDir.resolve("sql"));
        createOrReplaceSchema("deletedesttablecontent");
        int expectedRows = prepareDb2DbChainTables("deletedesttablecontent");
        insertAlbumRows("deletedesttablecontent", "dest", 3);
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'sql/deleteTableSuccesful.sql', 'deletedesttablecontent.albums_dest', true
                }
                """));

        run("copyRows");

        assertEquals(expectedRows, count("select count(*) from deletedesttablecontent.albums_dest"));
    }

    @Test
    void executesOriginalParameterFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/db2db", projectDir.resolve("sql"));
        createParameterListTables();
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'sql/parameter.sql', 'parameterlist.dest', false
                    sqlParameters([srcTable: 'src1'])
                }
                """));

        run("copyRows");

        assertEquals(Set.of("1a", "1b"), stringSet("select title from parameterlist.dest"));
    }

    @Test
    void executesOriginalParameterListFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/db2db", projectDir.resolve("sql"));
        createParameterListTables();
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'sql/parameter-list.sql', 'parameterlist.dest', false
                    sqlParameterSets([srcTable: 'src1'], [srcTable: 'src2'])
                }
                """));

        run("copyRows");

        assertEquals(5, count("select count(*) from parameterlist.dest"));
    }

    @Test
    void rollsBackAllTransfersOnFailure() throws Exception {
        writeSettings();
        createOrReplaceSchema("db2dbrollback");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("create table db2dbrollback.source_colors (id integer primary key, name text)");
            statement.execute("create table db2dbrollback.target_colors (id integer primary key, name text)");
            statement.execute("insert into db2dbrollback.source_colors values (1, 'red')");
        }
        Files.writeString(projectDir.resolve("copy.sql"),
                "select id, name from db2dbrollback.source_colors;", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("fail.sql"),
                "select id, name from db2dbrollback.missing;", StandardCharsets.UTF_8);
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'copy.sql', 'db2dbrollback.target_colors', false
                    transfer 'fail.sql', 'db2dbrollback.target_colors', false
                }
                """));

        runAndFail("copyRows");

        assertEquals(0, count("select count(*) from db2dbrollback.target_colors"));
    }

    @Test
    void rollsBackAllParameterSetsOnFailure() throws Exception {
        writeSettings();
        createOrReplaceSchema("db2dbparamrollback");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("create table db2dbparamrollback.target_colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("copy.sql"),
                "select ${id}::integer as id, '${name}'::text as name;", StandardCharsets.UTF_8);
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'copy.sql', 'db2dbparamrollback.target_colors', false
                    sqlParameterSets([id: 1, name: 'red'], [id: 1, name: 'duplicate'])
                }
                """));

        runAndFail("copyRows");

        assertEquals(0, count("select count(*) from db2dbparamrollback.target_colors"));
    }

    @Test
    void copiesGeometryTransforms() throws Exception {
        writeSettings();
        createOrReplaceSchema("db2dbgeometry");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("""
                    create table db2dbgeometry.target_geoms (
                        geom_wkt geometry(Point,2056),
                        geom_wkb geometry(Point,2056),
                        geom_geojson geometry(Point,2056))
                    """);
        }
        Files.writeString(projectDir.resolve("geoms.sql"), """
                select
                    ST_AsText(ST_GeomFromText('POINT(2600000 1200000)', 2056)) as geom_wkt,
                    ST_AsBinary(ST_GeomFromText('POINT(2600001 1200001)', 2056)) as geom_wkb,
                    ST_AsGeoJSON(ST_GeomFromText('POINT(2600002 1200002)', 2056)) as geom_geojson;
                """, StandardCharsets.UTF_8);
        writeBuild(db2DbBuild("""
                tasks.register('copyRows', Db2Db) {
                    sourceDatabase pgUrl, pgUser, pgPass
                    targetDatabase pgUrl, pgUser, pgPass
                    transfer 'geoms.sql', 'db2dbgeometry.target_geoms', false,
                            'geom_wkt:WKT:2056', 'geom_wkb:WKB:2056', 'geom_geojson:GEOJSON:2056'
                }
                """));

        run("copyRows");

        assertEquals(1, count("select count(*) from db2dbgeometry.target_geoms"));
        assertEquals("2056", scalar("""
                select distinct st_srid(geom_wkt)::text
                from db2dbgeometry.target_geoms
                where st_srid(geom_wkb) = 2056 and st_srid(geom_geojson) = 2056
                """));
    }

    private void createParameterListTables() throws Exception {
        createOrReplaceSchema("parameterlist");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("create table parameterlist.src1(title text)");
            statement.execute("create table parameterlist.src2(title text)");
            statement.execute("create table parameterlist.dest(title text)");
            statement.execute("insert into parameterlist.src1(title) values('1a'), ('1b')");
            statement.execute("insert into parameterlist.src2(title) values('2a'), ('2b'), ('2c')");
        }
    }

    private String db2DbBuild(String taskDefinition) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                def pgUrl = providers.gradleProperty('pgUrl').get()
                def pgUser = providers.gradleProperty('pgUser').get()
                def pgPass = providers.gradleProperty('pgPass').get()

                %s
                """.formatted(taskDefinition);
    }
}
