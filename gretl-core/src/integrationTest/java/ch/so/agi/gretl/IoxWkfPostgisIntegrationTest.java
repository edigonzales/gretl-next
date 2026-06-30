package ch.so.agi.gretl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IoxWkfPostgisIntegrationTest extends PostgisIntegrationTestSupport {

    @Test
    void importsAndExportsCsv() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/iox-wkf/CsvImport", projectDir.resolve("CsvImport"));
        createOrReplaceSchema("csvimport");
        createOrReplaceSchema("csvexport");
        try (Connection connection = pg(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE csvimport.importdata("
                    + "t_id integer, \"Aint\" integer, adec decimal(7,1), atext varchar(40), "
                    + "aenum varchar(120), adate date, atimestamp timestamp, aboolean boolean, aextra varchar(40))");
            statement.execute("CREATE TABLE csvexport.exportdata("
                    + "t_id integer, \"Aint\" integer, adec decimal(7,1), atext varchar(40), "
                    + "aenum varchar(120), adate date, atimestamp timestamp, aboolean boolean, aextra varchar(40))");
            statement.execute("INSERT INTO csvexport.exportdata(t_id, \"Aint\", adec, atext, adate, atimestamp, aboolean) "
                    + "VALUES (1,2,3.4,'abc','2013-10-21','2015-02-16T08:35:45.000','true')");
            statement.execute("INSERT INTO csvexport.exportdata(t_id) VALUES (2)");
        }
        writeBuild(ioxWkfBuild("""
                tasks.register('csvImport', CsvImport) {
                    database(project.property('pgUrl'), project.property('pgUser'), project.property('pgPass'))
                    schemaName 'csvimport'
                    tableName 'importdata'
                    firstLineIsHeader true
                    dataFile 'CsvImport/data1.csv'
                }

                tasks.register('csvExport', CsvExport) {
                    database(project.property('pgUrl'), project.property('pgUser'), project.property('pgPass'))
                    schemaName 'csvexport'
                    tableName 'exportdata'
                    firstLineIsHeader true
                    attributes 't_id', 'Aint', 'adec', 'atext', 'aenum', 'adate', 'atimestamp', 'aboolean'
                    dataFile layout.buildDirectory.file('data.csv')
                }
                """));

        run("csvImport", "csvExport");

        assertEquals(2, count("SELECT count(*) FROM csvimport.importdata"));
        assertEquals("abc", scalar("SELECT atext FROM csvimport.importdata WHERE t_id = 1"));
        List<String> lines = Files.readAllLines(projectDir.resolve("build/data.csv"), StandardCharsets.UTF_8);
        assertEquals("\"t_id\",\"Aint\",\"adec\",\"atext\",\"aenum\",\"adate\",\"atimestamp\",\"aboolean\"", lines.get(0));
        assertEquals("\"1\",\"2\",\"3.4\",\"abc\",\"\",\"2013-10-21\",\"2015-02-16T08:35:45.000\",\"true\"", lines.get(1));
    }

    @Test
    void importsJsonArrayAndObject() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/iox-wkf/JsonImportArray", projectDir.resolve("JsonImportArray"));
        copyResourceTree("original-gretl/iox-wkf/JsonImportObject", projectDir.resolve("JsonImportObject"));
        createOrReplaceSchema("jsonimport");
        try (Connection connection = pg(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE jsonimport.jsonarray(json_text_col text)");
            statement.execute("CREATE TABLE jsonimport.jsonobject(json_text_col text)");
        }
        writeBuild(ioxWkfBuild("""
                tasks.register('jsonArray', JsonImport) {
                    database(project.property('pgUrl'), project.property('pgUser'), project.property('pgPass'))
                    jsonFile 'JsonImportArray/data.json'
                    qualifiedTableName 'jsonimport.jsonarray'
                    columnName 'json_text_col'
                }

                tasks.register('jsonObject', JsonImport) {
                    database(project.property('pgUrl'), project.property('pgUser'), project.property('pgPass'))
                    jsonFile 'JsonImportObject/data.json'
                    qualifiedTableName 'jsonimport.jsonobject'
                    columnName 'json_text_col'
                }
                """));

        run("jsonArray", "jsonObject");

        assertEquals(2, count("SELECT count(*) FROM jsonimport.jsonarray"));
        assertEquals(1, count("SELECT count(*) FROM jsonimport.jsonobject"));
        assertTrue(scalar("SELECT json_text_col FROM jsonimport.jsonarray ORDER BY json_text_col LIMIT 1").contains("Doe"));
    }

    @Test
    void importsAndExportsGeoPackageTables() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/iox-wkf/GpkgImport", projectDir.resolve("GpkgImport"));
        createOrReplaceSchema("gpkgimport");
        createOrReplaceSchema("gpkgexport");
        try (Connection connection = pg(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE gpkgimport.importdata(fid integer, idname varchar, geom geometry(POINT,2056))");
            statement.execute("CREATE TABLE gpkgexport.exportdata(attr varchar, the_geom geometry(POINT,2056))");
            statement.execute("INSERT INTO gpkgexport.exportdata(attr, the_geom) "
                    + "VALUES ('coord2d', '0101000020080800001CD4411DD441CDBF0E69626CDD33E23F')");
            statement.execute("CREATE TABLE gpkgexport.exportdata1(attr varchar, the_geom geometry(POINT,2056))");
            statement.execute("CREATE TABLE gpkgexport.exportdata2(attr varchar, the_geom geometry(POINT,2056))");
            statement.execute("INSERT INTO gpkgexport.exportdata1(attr, the_geom) SELECT attr, the_geom FROM gpkgexport.exportdata");
            statement.execute("INSERT INTO gpkgexport.exportdata2(attr, the_geom) SELECT attr, the_geom FROM gpkgexport.exportdata");
        }
        writeBuild(ioxWkfBuild("""
                tasks.register('gpkgImport', GpkgImport) {
                    database(project.property('pgUrl'), project.property('pgUser'), project.property('pgPass'))
                    schemaName 'gpkgimport'
                    srcTableName 'Point'
                    dstTableName 'importdata'
                    dataFile 'GpkgImport/point.gpkg'
                    batchSize 200
                    fetchSize 200
                }

                tasks.register('gpkgExportOne', GpkgExport) {
                    database(project.property('pgUrl'), project.property('pgUser'), project.property('pgPass'))
                    schemaName 'gpkgexport'
                    srcTableName 'exportdata'
                    dstTableName 'exportdata'
                    dataFile layout.buildDirectory.file('one.gpkg')
                    batchSize 200
                    fetchSize 200
                }

                tasks.register('gpkgExportTwo', GpkgExport) {
                    database(project.property('pgUrl'), project.property('pgUser'), project.property('pgPass'))
                    schemaName 'gpkgexport'
                    srcTableName 'exportdata1', 'exportdata2'
                    dstTableName 'exportdata1', 'exportdata2'
                    dataFile layout.buildDirectory.file('two.gpkg')
                }
                """));

        run("gpkgImport", "gpkgExportOne", "gpkgExportTwo");

        assertEquals(1, count("SELECT count(*) FROM gpkgimport.importdata"));
        assertEquals("12", scalar("SELECT idname FROM gpkgimport.importdata"));
        assertEquals(1, sqliteInt(projectDir.resolve("build/one.gpkg"), "SELECT count(*) FROM exportdata"));
        assertEquals(1, sqliteInt(projectDir.resolve("build/two.gpkg"), "SELECT count(*) FROM exportdata1"));
        assertEquals(1, sqliteInt(projectDir.resolve("build/two.gpkg"), "SELECT count(*) FROM exportdata2"));
    }

    private String ioxWkfBuild(String tasks) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.CsvImport
                import ch.so.agi.gretl.tasks.CsvExport
                import ch.so.agi.gretl.tasks.JsonImport
                import ch.so.agi.gretl.tasks.GpkgImport
                import ch.so.agi.gretl.tasks.GpkgExport

                %s
                """.formatted(tasks);
    }

    private int sqliteInt(Path database, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
