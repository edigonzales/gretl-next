package ch.so.agi.gretl;

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
        writeBuild("""
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

                tasks.register('schema', Ili2pgImportSchema) {
                    database pgUrl, pgUser, pgPass
                    models 'Beispiel2'
                    modeldir projectDir.toString()
                    dbschema 'ili2pgtest'
                    defaultSrsCode '2056'
                    createBasketCol true
                }

                tasks.register('importData', Ili2pgImport) {
                    dependsOn 'schema'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    dataFile(files('Beispiel2a.xtf', 'Beispiel2b.xtf'))
                    dataset(['DatasetA', 'DatasetB'])
                }

                tasks.register('replaceA', Ili2pgReplace) {
                    dependsOn 'importData'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    dataFile(files('Beispiel2a.xtf'))
                    dataset('DatasetA')
                }

                tasks.register('updateA', Ili2pgUpdate) {
                    dependsOn 'replaceA'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    dataFile(files('Beispiel2a.xtf'))
                    dataset('DatasetA')
                }

                tasks.register('validateData', Ili2pgValidate) {
                    dependsOn 'updateA'
                    database pgUrl, pgUser, pgPass
                    modeldir projectDir.toString()
                    dbschema 'ili2pgtest'
                    dataset(['DatasetA', 'DatasetB'])
                    logFile layout.buildDirectory.file('validation.log').get().asFile
                }

                tasks.register('exportData', Ili2pgExport) {
                    dependsOn 'validateData'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    dataFile(files(layout.buildDirectory.file('DatasetA-out.xtf').get().asFile,
                            layout.buildDirectory.file('DatasetB-out.xtf').get().asFile))
                    dataset(['DatasetA', 'DatasetB'])
                }

                tasks.register('deleteData', Ili2pgDelete) {
                    dependsOn 'exportData'
                    database pgUrl, pgUser, pgPass
                    dbschema 'ili2pgtest'
                    dataset(['DatasetA', 'DatasetB'])
                }
                """);

        run("deleteData");

        assertTrue(Files.exists(projectDir.resolve("build/DatasetA-out.xtf")));
        assertTrue(Files.exists(projectDir.resolve("build/DatasetB-out.xtf")));
        assertTrue(Files.readString(projectDir.resolve("build/validation.log")).contains("...validation done"));
        assertEquals(0, count("select count(*) from ili2pgtest.boflaechen"));
        assertEquals(Set.of(), stringSet("select datasetname from ili2pgtest.t_ili2db_dataset"));
    }

    private void copyBeispiel2Resources() throws Exception {
        Path source = Path.of("src/test/resources/ili2db");
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
}
