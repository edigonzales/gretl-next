package ch.so.agi.gretl;

import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.EndBasketEvent;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox.StartBasketEvent;
import ch.interlis.iox.StartTransferEvent;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ili2duckdbFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void importsSchemaIntoDuckDb() throws Exception {
        writeSettings();
        copyResource("original-gretl/interlis/ili2duckdb/importSchema/KS3-20060703.ili", "KS3-20060703.ili");
        writeBuild(duckDbBuild("""
                tasks.register('schemaImport', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    coalesceJson.set(true)
                    createBasketCol.set(true)
                }
                """));

        run("schemaImport");

        Path database = projectDir.resolve("build/db/my_gb2av.duckdb");
        try (Connection connection = duckdb(database); Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("select content from gb2av.t_ili2db_model")) {
                assertTrue(rs.next());
                assertTrue(rs.getString(1).contains("INTERLIS 2.2;"));
                assertFalse(rs.next());
            }
            try (ResultSet rs = statement.executeQuery("""
                    select column_name
                    from information_schema.columns
                    where table_schema = 'gb2av'
                      and table_name = 'vollzugsgegenstand'
                      and column_name = 'mutationsnummer'
                    """)) {
                assertTrue(rs.next());
                assertEquals("mutationsnummer", rs.getString(1));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    void importsTransferFileAfterSchemaImport() throws Exception {
        writeSettings();
        copyResource("original-gretl/interlis/ili2duckdb/import/KS3-20060703.ili", "KS3-20060703.ili");
        copyResource("original-gretl/interlis/ili2duckdb/import/VOLLZUG_SO0200002401_1531_20180105113131.xml",
                "VOLLZUG_SO0200002401_1531_20180105113131.xml");
        writeBuild(duckDbBuild("""
                tasks.register('schemaImport', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    coalesceJson.set(true)
                    createBasketCol.set(true)
                }

                tasks.register('importData', Ili2duckdbImport) {
                    dependsOn 'schemaImport'
                    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    transferFiles 'VOLLZUG_SO0200002401_1531_20180105113131.xml'
                }
                """));

        run("importData");

        Path database = projectDir.resolve("build/db/my_gb2av.duckdb");
        try (Connection connection = duckdb(database); Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("select astatus from gb2av.vollzugsgegenstand")) {
                assertTrue(rs.next());
                assertEquals("Eintrag", rs.getString(1));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    void exportsTransferFileAfterImport() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ili2duckdb/import", projectDir);
        writeBuild(duckDbBuild("""
                tasks.register('schemaImport', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    coalesceJson.set(true)
                    createBasketCol.set(true)
                }

                tasks.register('importData', Ili2duckdbImport) {
                    dependsOn 'schemaImport'
                    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    transferFiles 'VOLLZUG_SO0200002401_1531_20180105113131.xml'
                }

                tasks.register('exportData', Ili2duckdbExport) {
                    dependsOn 'importData'
                    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    dataFiles layout.buildDirectory.file('out/export.xml')
                }
                """));

        run("exportData");

        assertXtfFile(projectDir.resolve("build/out/export.xml"));
    }

    @Test
    void derivesDatasetNameFromTransferFileAndSlice() throws Exception {
        writeSettings();
        copyResource("original-gretl/interlis/ili2duckdb/import/KS3-20060703.ili", "KS3-20060703.ili");
        copyResource("original-gretl/interlis/ili2duckdb/import/VOLLZUG_SO0200002401_1531_20180105113131.xml",
                "src/prefix_alpha.xml");
        writeBuild(duckDbBuild("""
                tasks.register('schemaImport', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/datasets.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    coalesceJson.set(true)
                    createBasketCol.set(true)
                    createDatasetCol.set(true)
                }

                tasks.register('importData', Ili2duckdbImport) {
                    dependsOn 'schemaImport'
                    databaseFile layout.buildDirectory.file('db/datasets.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    transferFiles 'src/prefix_alpha.xml'
                    datasetNamesFromTransferFiles()
                    datasetNameSlice 7
                }
                """));

        run("importData");

        Path database = projectDir.resolve("build/db/datasets.duckdb");
        assertEquals("alpha", duckdbScalar(database, "select datasetname from gb2av.t_ili2db_dataset"));
    }

    @Test
    void derivesDatasetNameFromSeparateFilesAndSlice() throws Exception {
        writeSettings();
        copyResource("original-gretl/interlis/ili2duckdb/import/KS3-20060703.ili", "KS3-20060703.ili");
        copyResource("original-gretl/interlis/ili2duckdb/import/VOLLZUG_SO0200002401_1531_20180105113131.xml",
                "transfer/original.xml");
        Files.createDirectories(projectDir.resolve("names"));
        Files.writeString(projectDir.resolve("names/id_beta.ref"), "beta");
        writeBuild(duckDbBuild("""
                tasks.register('schemaImport', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/datasets.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    coalesceJson.set(true)
                    createBasketCol.set(true)
                    createDatasetCol.set(true)
                }

                tasks.register('importData', Ili2duckdbImport) {
                    dependsOn 'schemaImport'
                    databaseFile layout.buildDirectory.file('db/datasets.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    transferFiles 'transfer/original.xml'
                    datasetNamesFromFiles 'names/id_beta.ref'
                    datasetNameSlice 3, 7
                }
                """));

        run("importData");

        Path database = projectDir.resolve("build/db/datasets.duckdb");
        assertEquals("beta", duckdbScalar(database, "select datasetname from gb2av.t_ili2db_dataset"));
    }

    @Test
    void rejectsTransferFilesAndRepositoryDataIdsTogether() throws Exception {
        writeSettings();
        copyResource("original-gretl/interlis/ili2duckdb/import/KS3-20060703.ili", "KS3-20060703.ili");
        copyResource("original-gretl/interlis/ili2duckdb/import/VOLLZUG_SO0200002401_1531_20180105113131.xml",
                "data.xml");
        writeBuild(duckDbBuild("""
                tasks.register('schemaImport', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/conflict.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                }

                tasks.register('importData', Ili2duckdbImport) {
                    dependsOn 'schemaImport'
                    databaseFile layout.buildDirectory.file('db/conflict.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    transferFiles 'data.xml'
                    repositoryDataIds 'ilidata:dummy'
                }
                """));

        runAndFail("importData");
    }

    @Test
    void rejectsDatasetNameCountMismatch() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ili2duckdb/import", projectDir);
        writeBuild(duckDbBuild("""
                tasks.register('schemaImport', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/mismatch.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    createDatasetCol.set(true)
                }

                tasks.register('importData', Ili2duckdbImport) {
                    dependsOn 'schemaImport'
                    databaseFile layout.buildDirectory.file('db/mismatch.duckdb')
                    modelNames 'GB2AV'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    schema 'gb2av'
                    transferFiles 'VOLLZUG_SO0200002401_1531_20180105113131.xml'
                    datasetNames 'alpha', 'beta'
                }
                """));

        runAndFail("importData");
    }

    private String duckDbBuild(String taskConfiguration) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Ili2duckdbExport
                import ch.so.agi.gretl.tasks.Ili2duckdbImport
                import ch.so.agi.gretl.tasks.Ili2duckdbImportSchema

                %s
                """.formatted(taskConfiguration);
    }

    private Connection duckdb(Path database) throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        return DriverManager.getConnection("jdbc:duckdb:" + database.toAbsolutePath());
    }

    private String duckdbScalar(Path database, String sql) throws Exception {
        try (Connection connection = duckdb(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    private void assertXtfFile(Path file) throws Exception {
        XtfReader reader = new XtfReader(file.toFile());
        try {
            assertInstanceOf(StartTransferEvent.class, reader.read());
            assertInstanceOf(StartBasketEvent.class, reader.read());
            IoxEvent event = reader.read();
            while (event instanceof ObjectEvent) {
                event = reader.read();
            }
            assertInstanceOf(EndBasketEvent.class, event);
            assertInstanceOf(EndTransferEvent.class, reader.read());
        } finally {
            reader.close();
        }
    }
}
