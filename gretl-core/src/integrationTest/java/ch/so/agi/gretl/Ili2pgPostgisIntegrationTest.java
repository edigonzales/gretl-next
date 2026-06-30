package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ili2pgPostgisIntegrationTest extends PostgisIntegrationTestSupport {

    @Test
    void runsPostgisIli2dbTaskChain() throws Exception {
        writeSettings();
        copyBeispiel2Resources();
        createOrReplaceSchema("ili2pgtest");
        writeBuild(ili2pgBuild("""
                tasks.register('schema', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'Beispiel2'
                    modeldir projectDir.toString()
                    dbschema 'ili2pgtest'
                    defaultSrsCode '2056'
                    createBasketCol true
                    createTidCol true
                }

                tasks.register('importData', Ili2pgImport) {
                    dependsOn 'schema'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    transferFiles 'Beispiel2a.xtf', 'Beispiel2b.xtf'
                    dataset(['DatasetA', 'DatasetB'])
                    importTid true
                }

                tasks.register('replaceA', Ili2pgReplace) {
                    dependsOn 'importData'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    transferFiles 'Beispiel2a.xtf'
                    dataset('DatasetA')
                    importTid true
                }

                tasks.register('updateA', Ili2pgUpdate) {
                    dependsOn 'replaceA'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    transferFiles 'Beispiel2a.xtf'
                    dataset('DatasetA')
                    importTid true
                }

                tasks.register('validateData', Ili2pgValidate) {
                    dependsOn 'updateA'
                    database pgUrl, pgUser, pgPass
                    models 'Beispiel2'
                    modeldir projectDir.toString()
                    dbschema 'ili2pgtest'
                    dataset 'DatasetA'
                    logFile layout.buildDirectory.file('validation.log')
                }

                tasks.register('exportData', Ili2pgExport) {
                    dependsOn 'validateData'
                    database pgUrl, pgUser, pgPass
                    models 'Beispiel2'
                    modeldir projectDir.toString()
                    dbschema 'ili2pgtest'
                    dataFiles layout.buildDirectory.file('DatasetA-out.xtf')
                    dataset 'DatasetA'
                }

                tasks.register('deleteData', Ili2pgDelete) {
                    dependsOn 'exportData'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    dataset(['DatasetA', 'DatasetB'])
                }
                """));

        run("deleteData");

        assertTrue(Files.exists(projectDir.resolve("build/DatasetA-out.xtf")));
        assertTrue(Files.readString(projectDir.resolve("build/validation.log")).contains("...validate done"));
        assertEquals(0, count("select count(*) from ili2pgtest.boflaechen"));
        assertEquals(Set.of(), stringSet("select datasetname from ili2pgtest.t_ili2db_dataset"));
    }

    @Test
    void exportsMultipleDatasetsToMultipleFiles() throws Exception {
        writeSettings();
        copyFixture("Ili2pgExportDatasets");
        createOrReplaceSchema("beispiel2_export");
        writeBuild(ili2pgBuild("""
                tasks.register('schema', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'Beispiel2'
                    modeldir "%%ILI_FROM_DB;${projectDir}/Ili2pgExportDatasets;http://models.interlis.ch"
                    dbschema 'beispiel2_export'
                    defaultSrsCode '2056'
                    createBasketCol true
                }

                tasks.register('importData', Ili2pgImport) {
                    dependsOn 'schema'
                    database pgUrl, pgUser, pgPass
                    dbschema 'beispiel2_export'
                    transferFiles "${projectDir}/Ili2pgExportDatasets/Beispiel2a.xtf",
                            "${projectDir}/Ili2pgExportDatasets/Beispiel2b.xtf"
                    dataset(['DatasetA', 'DatasetB'])
                }

                tasks.register('exportData', Ili2pgExport) {
                    dependsOn 'importData'
                    database pgUrl, pgUser, pgPass
                    dbschema 'beispiel2_export'
                    dataFiles layout.buildDirectory.file('DatasetA-out.xtf'),
                            layout.buildDirectory.file('DatasetB-out.xtf')
                    dataset(['DatasetA', 'DatasetB'])
                }
                """));

        run("exportData");

        assertTrue(Files.exists(projectDir.resolve("build/DatasetA-out.xtf")));
        assertTrue(Files.exists(projectDir.resolve("build/DatasetB-out.xtf")));
    }

    @Test
    void importsSchemaWithOptionsAndMetaConfigs() throws Exception {
        writeSettings();
        copyFixture("Ili2pgImportSchema");
        copyFixture("Ili2pgImportSchema_Options");
        copyFixture("Ili2pgImportSchema_MetaConfigFile");
        copyFixture("Ili2pgImportSchema_MetaConfigIliData");
        createOrReplaceSchema("gb2av");
        createOrReplaceSchema("afu_abbaustellen_pub");
        createOrReplaceSchema("simple_table_metaconfigfile");
        createOrReplaceSchema("simple_table_ilidata");
        writeBuild(ili2pgBuild("""
                tasks.register('schemaStandard', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'GB2AV'
                    modeldir "${projectDir}/Ili2pgImportSchema;http://models.interlis.ch"
                    dbschema 'gb2av'
                    coalesceJson true
                    createBasketCol true
                }

                tasks.register('schemaOptions', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'SO_AFU_ABBAUSTELLEN_20210630'
                    modeldir "${projectDir}/Ili2pgImportSchema_Options;http://models.interlis.ch"
                    dbschema 'afu_abbaustellen_pub'
                    sqlExtRefCols true
                    sqlColsAsText true
                }

                tasks.register('schemaMetaFile', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    dbschema 'simple_table_metaconfigfile'
                    modeldir "%%ILI_FROM_DB;${projectDir}/Ili2pgImportSchema_MetaConfigFile;https://geo.so.ch/models;http://models.interlis.ch"
                    metaConfig "${projectDir}/Ili2pgImportSchema_MetaConfigFile/simple_table_ini_20240502.ini"
                }

                tasks.register('schemaMetaIlidata', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    dbschema 'simple_table_ilidata'
                    modeldir "%%ILI_FROM_DB;${projectDir}/Ili2pgImportSchema_MetaConfigIliData;https://geo.so.ch/models;http://models.interlis.ch"
                    metaConfig 'ilidata:metaconfig_simple_table_ini_20240502'
                }
                """));

        run("schemaStandard", "schemaOptions", "schemaMetaFile", "schemaMetaIlidata");

        assertTrue(tableCount("gb2av") > 0);
        assertTrue(tableCount("afu_abbaustellen_pub") > 0);
        assertTrue(tableCount("simple_table_metaconfigfile") > 0);
        assertTrue(tableCount("simple_table_ilidata") > 0);
    }

    @Test
    void importsFileTreeWithLegacyDatasetSubstring() throws Exception {
        writeSettings();
        copyFixture("Ili2pgImportFileSet");
        createOrReplaceSchema("beispiel2_fileset");
        writeBuild(ili2pgBuild("""
                tasks.register('schema', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'Beispiel2'
                    defaultSrsCode '2056'
                    modeldir "%%ILI_FROM_DB;${projectDir}/Ili2pgImportFileSet;http://models.interlis.ch"
                    dbschema 'beispiel2_fileset'
                    createBasketCol true
                }

                tasks.register('importData', Ili2pgImport) {
                    dependsOn 'schema'
                    database pgUrl, pgUser, pgPass
                    dbschema 'beispiel2_fileset'
                    transferFiles(fileTree("${projectDir}/Ili2pgImportFileSet") { include '*.xtf' })
                    dataset(['A_Dataset', 'B_Dataset'])
                    datasetSubstring((0..4).toList())
                }
                """));

        run("importData");

        assertEquals(Set.of("A_Da", "B_Da"),
                stringSet("select datasetname from beispiel2_fileset.t_ili2db_dataset"));
        assertEquals(4, count("select count(*) from beispiel2_fileset.boflaechen"));
    }

    @Test
    void replacesRepositoryDataId() throws Exception {
        writeSettings();
        copyFixture("Ili2pgReplaceIlidataFile");
        createOrReplaceSchema("agi_av_mopublic");
        writeBuild(ili2pgBuild("""
                tasks.register('schema', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'SO_AGI_MOpublic_20190424'
                    defaultSrsCode '2056'
                    modeldir "%%ILI_FROM_DB;${projectDir}/Ili2pgReplaceIlidataFile;https://geo.so.ch/models;http://models.interlis.ch"
                    dbschema 'agi_av_mopublic'
                    createBasketCol true
                    createDatasetCol true
                }

                tasks.register('replaceData', Ili2pgReplace) {
                    dependsOn 'schema'
                    database pgUrl, pgUser, pgPass
                    modeldir "%%ILI_FROM_DB;${projectDir}/Ili2pgReplaceIlidataFile;https://geo.so.ch/models;http://models.interlis.ch"
                    dbschema 'agi_av_mopublic'
                    repositoryDataIds 'ilidata:2549.ch.so.agi.av.mopublic'
                    dataset(['2549'])
                }
                """));

        run("replaceData");

        assertEquals(Set.of("2549"), stringSet("select datasetname from agi_av_mopublic.t_ili2db_dataset"));
    }

    @Test
    void validatesSingleMultipleAndGlobalDatasets() throws Exception {
        writeSettings();
        copyFixture("Ili2pgValidateMultipleDatasets");
        createOrReplaceSchema("afu_schutzbauten_v1_multi");
        writeBuild(ili2pgBuild("""
                tasks.register('schema', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'SO_AFU_Schutzbauten_20231212'
                    modeldir "${projectDir}/Ili2pgValidateMultipleDatasets;http://models.interlis.ch"
                    dbschema 'afu_schutzbauten_v1_multi'
                    coalesceMultiSurface true
                    coalesceMultiLine true
                    createBasketCol true
                    createTidCol true
                    defaultSrsAuth 'EPSG'
                    defaultSrsCode '2056'
                    importTid true
                    smart2Inheritance true
                    strokeArcs true
                }

                tasks.register('importDatasets', Ili2pgImport) {
                    dependsOn 'schema'
                    database pgUrl, pgUser, pgPass
                    models 'SO_AFU_Schutzbauten_20231212'
                    modeldir "${projectDir}/Ili2pgValidateMultipleDatasets;http://models.interlis.ch"
                    dbschema 'afu_schutzbauten_v1_multi'
                    transferFiles "${projectDir}/Ili2pgValidateMultipleDatasets/ch.so.agi.testeinzelobjekt_valid1.xtf",
                            "${projectDir}/Ili2pgValidateMultipleDatasets/ch.so.agi.testeinzelobjekt_valid2.xtf"
                    dataset(['agi.testeinzelobjekt1', 'agi.testeinzelobjekt2'])
                }

                tasks.register('validateSingle', Ili2pgValidate) {
                    dependsOn 'importDatasets'
                    database pgUrl, pgUser, pgPass
                    models 'SO_AFU_Schutzbauten_20231212'
                    modeldir "${projectDir}/Ili2pgValidateMultipleDatasets;http://models.interlis.ch"
                    dbschema 'afu_schutzbauten_v1_multi'
                    dataset 'agi.testeinzelobjekt1'
                    logFile layout.buildDirectory.file('validation-single.log')
                }

                tasks.register('validateMultiple', Ili2pgValidate) {
                    dependsOn 'importDatasets'
                    database pgUrl, pgUser, pgPass
                    models 'SO_AFU_Schutzbauten_20231212'
                    modeldir "${projectDir}/Ili2pgValidateMultipleDatasets;http://models.interlis.ch"
                    dbschema 'afu_schutzbauten_v1_multi'
                    dataset(['agi.testeinzelobjekt1', 'agi.testeinzelobjekt2'])
                    logFile layout.buildDirectory.file('validation-multiple.log')
                }

                tasks.register('validateGlobal', Ili2pgValidate) {
                    dependsOn 'importDatasets'
                    database pgUrl, pgUser, pgPass
                    models 'SO_AFU_Schutzbauten_20231212'
                    modeldir "${projectDir}/Ili2pgValidateMultipleDatasets;http://models.interlis.ch"
                    dbschema 'afu_schutzbauten_v1_multi'
                    logFile layout.buildDirectory.file('validation-global.log')
                }
                """));

        run("validateSingle", "validateMultiple");
        BuildResult failed = runAndFail("validateGlobal");

        assertTrue(Files.readString(projectDir.resolve("build/validation-single.log")).contains("...validate done"));
        assertTrue(Files.readString(projectDir.resolve("build/validation-multiple.log")).contains("...validate done"));
        assertTrue(failed.getOutput().contains("failed to run ili2db"));
    }

    @Test
    void rejectsExportDatasetCountMismatch() throws Exception {
        writeSettings();
        copyBeispiel2Resources();
        createOrReplaceSchema("ili2pg_mismatch");
        writeBuild(ili2pgBuild("""
                tasks.register('schema', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'Beispiel2'
                    modeldir projectDir.toString()
                    dbschema 'ili2pg_mismatch'
                    defaultSrsCode '2056'
                    createBasketCol true
                }

                tasks.register('importData', Ili2pgImport) {
                    dependsOn 'schema'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pg_mismatch'
                    transferFiles 'Beispiel2a.xtf', 'Beispiel2b.xtf'
                    dataset(['DatasetA', 'DatasetB'])
                }

                tasks.register('exportMismatch', Ili2pgExport) {
                    dependsOn 'importData'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pg_mismatch'
                    dataFiles layout.buildDirectory.file('only-one.xtf')
                    dataset(['DatasetA', 'DatasetB'])
                }
                """));

        BuildResult failed = runAndFail("exportMismatch");

        assertTrue(failed.getOutput().contains("number of dataset names (2) doesn't match number of files (1)"));
    }

    private String ili2pgBuild(String taskDefinitions) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Ili2pgImportSchema
                import ch.so.agi.gretl.tasks.Ili2pgImport
                import ch.so.agi.gretl.tasks.Ili2pgReplace
                import ch.so.agi.gretl.tasks.Ili2pgUpdate
                import ch.so.agi.gretl.tasks.Ili2pgValidate
                import ch.so.agi.gretl.tasks.Ili2pgExport
                import ch.so.agi.gretl.tasks.Ili2pgDelete

                def pgUrl = providers.gradleProperty('pgUrl').get()
                def pgUser = providers.gradleProperty('pgUser').get()
                def pgPass = providers.gradleProperty('pgPass').get()

                %s
                """.formatted(taskDefinitions);
    }

    private void copyBeispiel2Resources() throws Exception {
        Path source = Path.of("src/test/resources/fixtures/interlis/ili2db/beispiel2");
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    Files.copy(path, projectDir.resolve(path.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void copyFixture(String name) throws Exception {
        copyResourceTree("fixtures/interlis/ili2pg/" + name, projectDir.resolve(name));
    }

    private int tableCount(String schemaName) throws Exception {
        return count("select count(*) from information_schema.tables where table_schema = '" + schemaName + "'");
    }
}
