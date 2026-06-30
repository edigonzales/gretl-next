package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterlisValidatorFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void validatesInterlisTransferSuccessfully() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ilivalidator/basic", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.IliValidator

                tasks.register('validate', IliValidator) {
                    dataFiles 'Beispiel2a.xtf'
                    modelDirectories projectDir.toString()
                    logFile layout.buildDirectory.file('logs/ilivalidator.log')
                }
                """);

        run("validate");

        String content = Files.readString(projectDir.resolve("build/logs/ilivalidator.log"));
        assertTrue(content.contains("Info: ...validation done"));
    }

    @Test
    void failsInterlisValidation() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ilivalidator/fail", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.IliValidator

                tasks.register('validate', IliValidator) {
                    dataFiles 'Beispiel2a.xtf'
                    modelDirectories projectDir.toString()
                    logFile layout.buildDirectory.file('logs/ilivalidator.log')
                }
                """);

        runAndFail("validate");
    }

    @Test
    void exposesValidationOkWhenFailOnErrorIsDisabled() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ilivalidator/fail", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.IliValidator

                def validateProvider = tasks.register('validate', IliValidator) {
                    dataFiles 'Beispiel2a.xtf'
                    modelDirectories projectDir.toString()
                    logFile layout.buildDirectory.file('logs/ilivalidator.log')
                    failOnError.set(false)
                }

                tasks.register('assertValidationResult') {
                    dependsOn validateProvider
                    doLast {
                        assert !validateProvider.get().validationOk
                    }
                }
                """);

        run("assertValidationResult");
    }

    @Test
    void loadsMetaConfigAndCustomFunctionsForNgkFixture() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ilivalidator/ngk", projectDir);
        Files.writeString(projectDir.resolve("SO_AFU_Naturgefahren_20240515-gretl-meta.ini"), """
                [ch.ehi.ilivalidator]
                models=SO_AFU_Naturgefahren_20240515
                config=file:%s
                allObjectsAccessible=true
                """.formatted(projectDir.resolve("SO_AFU_Naturgefahren_20240515-gretl.ini").toAbsolutePath()));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.IliValidator

                tasks.register('validate', IliValidator) {
                    dataFiles 'NGK_SO_Testbeddata.xtf'
                    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
                    metaConfigFile 'SO_AFU_Naturgefahren_20240515-gretl-meta.ini'
                    logFile layout.buildDirectory.file('logs/ilivalidator.log')
                }
                """);

        runAndFail("validate");

        String content = Files.readString(projectDir.resolve("build/logs/ilivalidator.log"));
        assertTrue(content.contains("additional model SO_AFU_Naturgefahren_Validierung_20240515"));
        assertTrue(content.contains("tid 701051de-6f2f-476d-81fb-43b8885ae7fc"));
        assertTrue(content.contains("Dateinamen des XTF entsprechen"));
    }

    @Test
    void validatesCsvSuccessfully() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/csvvalidator/ok", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.CsvValidator

                tasks.register('validate', CsvValidator) {
                    dataFiles 'data1.csv'
                    modelNames 'CsvModel'
                    modelDirectories projectDir.toString()
                    firstLineIsHeader.set(false)
                    logFile layout.buildDirectory.file('logs/csvvalidator.log')
                }
                """);

        run("validate");

        assertTrue(Files.exists(projectDir.resolve("build/logs/csvvalidator.log")));
    }

    @Test
    void failsCsvValidationAndWritesDetailedLog() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/csvvalidator/fail", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.CsvValidator

                tasks.register('validate', CsvValidator) {
                    dataFiles 'dataFail.csv'
                    modelNames 'CsvModel'
                    modelDirectories projectDir.toString()
                    firstLineIsHeader.set(false)
                    logFile layout.buildDirectory.file('logs/csvvalidator.log')
                }
                """);

        runAndFail("validate");

        String content = Files.readString(projectDir.resolve("build/logs/csvvalidator.log"));
        assertTrue(content.contains("value <x> is not a number"));
        assertTrue(content.contains("value gruen is not a member of the enumeration"));
    }

    @Test
    void rejectsMultipleCsvInputFiles() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/csvvalidator/ok", projectDir);
        Files.writeString(projectDir.resolve("data2.csv"), Files.readString(projectDir.resolve("data1.csv")));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.CsvValidator

                tasks.register('validate', CsvValidator) {
                    dataFiles 'data1.csv', 'data2.csv'
                    modelNames 'CsvModel'
                    modelDirectories projectDir.toString()
                    firstLineIsHeader.set(false)
                }
                """);

        BuildResult result = runAndFail("validate");

        assertTrue(result.getOutput().contains("CsvValidator accepts exactly one input file."));
    }

    @Test
    void acceptsInterlisFileSets() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/interlis/ilivalidator/fileset", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.IliValidator

                tasks.register('validate', IliValidator) {
                    dataFiles 'Beispiel2a.xtf', 'Beispiel2b.xtf'
                    modelDirectories projectDir.toString()
                    logFile layout.buildDirectory.file('logs/ilivalidator.log')
                }
                """);

        run("validate");

        String content = Files.readString(projectDir.resolve("build/logs/ilivalidator.log"));
        assertTrue(content.contains("Info: ...validation done"));
    }
}
