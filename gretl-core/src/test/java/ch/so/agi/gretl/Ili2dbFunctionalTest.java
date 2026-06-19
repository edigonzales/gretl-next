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
                    dataFiles(files('Beispiel2a.xtf', 'Beispiel2b.xtf'))
                    modelNames 'Beispiel2'
                    modelDirectories projectDir.toString()
                    logFile layout.buildDirectory.file('valid.log').get().asFile
                }

                tasks.register('validateInvalid', IliValidator) {
                    dataFiles(files('Beispiel2-invalid.xtf'))
                    modelNames 'Beispiel2'
                    modelDirectories projectDir.toString()
                    failOnError.set(false)
                    logFile layout.buildDirectory.file('invalid.log').get().asFile
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
                    dataFile(fileTree(projectDir) { include 'Beispiel2*.xtf' })
                    dataset(['DatasetA', 'DatasetB'])
                    dbfile layout.buildDirectory.file('Beispiel2.gpkg').get().asFile
                }
                """));

        run("importGpkg");

        Path gpkg = projectDir.resolve("build/Beispiel2.gpkg");
        assertEquals(4, sqliteInt(gpkg, "SELECT count(*) FROM boflaechen"));
        assertEquals(Set.of("DatasetA", "DatasetB"), sqliteStringSet(gpkg,
                "SELECT datasetname FROM t_ili2db_dataset"));
    }

    @Test
    void importsAndExportsDuckDb() throws Exception {
        writeSettings();
        copyBeispiel2Resources();
        writeBuild(ili2dbBuild("""
                def duckdb = layout.buildDirectory.file('Beispiel2.duckdb').get().asFile

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
                    transferFiles layout.buildDirectory.file('export.xtf').get().asFile
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
