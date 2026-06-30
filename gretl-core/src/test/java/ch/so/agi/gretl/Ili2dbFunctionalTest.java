package ch.so.agi.gretl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ili2dbFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void validatesValidFilesAndExposesValidationResultForFailures() throws Exception {
        writeSettings();
        copyBeispiel2Resources();
        String invalid = Files.readString(projectDir.resolve("Beispiel2a.xtf"), StandardCharsets.UTF_8)
                .replace("<Art>123456</Art>", "<Art>1234567</Art>");
        Files.writeString(projectDir.resolve("Beispiel2-invalid.xtf"), invalid, StandardCharsets.UTF_8);
        writeBuild(ili2dbBuild("""
                tasks.register('validateOk', IliValidator) {
                    dataFiles 'Beispiel2a.xtf', 'Beispiel2b.xtf'
                    modelNames 'Beispiel2'
                    modelDirectories projectDir.toString()
                    logFile layout.buildDirectory.file('valid.log')
                }

                tasks.register('validateInvalid', IliValidator) {
                    dataFiles 'Beispiel2-invalid.xtf'
                    modelNames 'Beispiel2'
                    modelDirectories projectDir.toString()
                    failOnError.set(false)
                    logFile layout.buildDirectory.file('invalid.log')
                    doLast {
                        layout.buildDirectory.file('validation-result.txt').get().asFile.text = validationOk.toString()
                    }
                }
                """));

        run("validateOk", "validateInvalid");

        assertTrue(Files.readString(projectDir.resolve("build/valid.log")).contains("...validation done"));
        assertEquals("false", Files.readString(projectDir.resolve("build/validation-result.txt")));
        assertTrue(Files.readString(projectDir.resolve("build/invalid.log")).contains("...validation failed"));
    }

    @Test
    void importsGeoPackageWithFileTreeAndDatasets() throws Exception {
        writeSettings();
        copyBeispiel2Resources();
        writeBuild(ili2dbBuild("""
                tasks.register('importGpkg', Ili2gpkgImport) {
                    models 'Beispiel2'
                    modeldir projectDir.toString()
                    defaultSrsCode '2056'
                    transferFiles(fileTree(projectDir) { include 'Beispiel2*.xtf' })
                    dataset(['DatasetA', 'DatasetB'])
                    dbfile layout.buildDirectory.file('Beispiel2.gpkg')
                }
                """));

        run("importGpkg");

        Path gpkg = projectDir.resolve("build/Beispiel2.gpkg");
        assertEquals(4, sqliteInt(gpkg, "SELECT count(*) FROM boflaechen"));
        assertEquals(Set.of("DatasetA", "DatasetB"), sqliteStringSet(gpkg,
                "SELECT datasetname FROM t_ili2db_dataset"));
    }

    @Test
    void importsOriginalGretlAdministrativeGeoPackageFixture() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ili2gpkg/import", projectDir);
        writeBuild(ili2dbBuild("""
                tasks.register('importGpkg', Ili2gpkgImport) {
                    models 'SO_AGI_AV_GB_Administrative_Einteilungen_20180613'
                    modeldir "${projectDir};https://models.interlis.ch;https://models.geo.admin.ch"
                    transferFiles 'ch.so.agi.av_gb_admin_einteilung_edit_2020-08-20.xtf'
                    dbfile layout.buildDirectory.file('ch.so.agi.av_gb_admin_einteilung_edit_2020-08-20.gpkg')
                }
                """));

        run("importGpkg");

        Path gpkg = projectDir.resolve("build/ch.so.agi.av_gb_admin_einteilung_edit_2020-08-20.gpkg");
        assertTrue(sqliteInt(gpkg, "SELECT count(*) FROM t_ili2db_model") > 0);
        assertTrue(sqliteInt(gpkg, "SELECT count(*) FROM grundbuchkreis") > 0);
    }

    @Test
    void importsAndExportsDuckDb() throws Exception {
        writeSettings();
        copyBeispiel2Resources();
        writeBuild(ili2dbBuild("""
                def duckdb = layout.buildDirectory.file('Beispiel2.duckdb')

                tasks.register('schema', Ili2duckdbImportSchema) {
                    databaseFile duckdb
                    modelNames 'Beispiel2'
                    modelDirectories projectDir.toString()
                    schema 'ili'
                    defaultSrsCode.set('2056')
                    createBasketCol.set(true)
                }

                tasks.register('importDuck', Ili2duckdbImport) {
                    dependsOn 'schema'
                    databaseFile duckdb
                    modelNames 'Beispiel2'
                    modelDirectories projectDir.toString()
                    schema 'ili'
                    transferFiles 'Beispiel2a.xtf'
                }

                tasks.register('exportDuck', Ili2duckdbExport) {
                    dependsOn 'importDuck'
                    databaseFile duckdb
                    modelNames 'Beispiel2'
                    modelDirectories projectDir.toString()
                    schema 'ili'
                    dataFiles layout.buildDirectory.file('export.xtf')
                }
                """));

        run("exportDuck");

        Path duckdb = projectDir.resolve("build/Beispiel2.duckdb");
        assertEquals(3, duckdbInt(duckdb, "SELECT count(*) FROM ili.gebaeudeart"));
        assertTrue(Files.readString(projectDir.resolve("build/export.xtf"), StandardCharsets.UTF_8)
                .contains("<TRANSFER"));
    }

    private void copyBeispiel2Resources() throws Exception {
        copyResource("ili2db/Beispiel2.ili", "Beispiel2.ili");
        copyResource("ili2db/Beispiel2a.xtf", "Beispiel2a.xtf");
        copyResource("ili2db/Beispiel2b.xtf", "Beispiel2b.xtf");
    }

    private String ili2dbBuild(String taskDefinition) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.IliValidator
                import ch.so.agi.gretl.tasks.Ili2gpkgImport
                import ch.so.agi.gretl.tasks.Ili2duckdbImportSchema
                import ch.so.agi.gretl.tasks.Ili2duckdbImport
                import ch.so.agi.gretl.tasks.Ili2duckdbExport

                %s
                """.formatted(taskDefinition);
    }

    private int sqliteInt(Path database, String sql) throws Exception {
        try (Connection connection = sqlite(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private Set<String> sqliteStringSet(Path database, String sql) throws Exception {
        try (Connection connection = sqlite(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSetToSet(resultSet);
        }
    }

    private int duckdbInt(Path database, String sql) throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:" + database.toAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private Set<String> resultSetToSet(ResultSet resultSet) throws Exception {
        Set<String> values = new java.util.LinkedHashSet<>();
        while (resultSet.next()) {
            values.add(resultSet.getString(1));
        }
        return values.stream().collect(Collectors.toSet());
    }
}
