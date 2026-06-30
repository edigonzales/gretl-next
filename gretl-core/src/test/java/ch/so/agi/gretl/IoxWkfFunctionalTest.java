package ch.so.agi.gretl;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IoxWkfFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void convertsCsvToExcelWorkbook() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/iox-wkf/Csv2Excel", projectDir);
        writeBuild(ioxWkfBuild("""
                tasks.register('convertData', Csv2Excel) {
                    csvFile file('20230124_sap_Gebaeude.csv')
                    firstLineIsHeader true
                    valueSeparator ';'
                    encoding 'ISO-8859-1'
                    models 'SO_HBA_Gebaeude_20230111'
                    modeldir projectDir.toString()
                    outputFile layout.buildDirectory.file('buildings.xlsx')
                }
                """));

        run("convertData");

        List<String> values = workbookValues(projectDir.resolve("build/buildings.xlsx"));
        assertTrue(values.contains("EGID"), values.toString());
        assertTrue(values.contains("2605951.2"), values.toString());
        assertTrue(values.contains("2102-07"), values.toString());
    }

    @Test
    void convertsEmptyCsvToExcelWorkbook() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/iox-wkf/Csv2ExcelEmptyFile", projectDir);
        writeBuild(ioxWkfBuild("""
                tasks.register('convertData', Csv2Excel) {
                    csvFile file('superflous_publication_formats.csv')
                    firstLineIsHeader true
                    valueSeparator ';'
                    outputFile layout.buildDirectory.file('empty.xlsx')
                }
                """));

        run("convertData");

        List<String> values = workbookValues(projectDir.resolve("build/empty.xlsx"));
        assertTrue(values.contains("egid"));
        assertTrue(values.contains("xkoordinaten"));
    }

    @Test
    void validatesCsvJsonAndGeoPackageInputs() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/iox-wkf/CsvValidator", projectDir);
        copyResource("original-gretl/iox-wkf/CsvValidatorFail/dataFail.csv", "dataFail.csv");
        copyResourceTree("original-gretl/iox-wkf/JsonValidatorOk", projectDir.resolve("json-ok"));
        copyResourceTree("original-gretl/iox-wkf/JsonValidatorFail", projectDir.resolve("json-fail"));
        copyResourceTree("original-gretl/iox-wkf/GpkgValidator", projectDir.resolve("gpkg-ok"));
        copyResourceTree("original-gretl/iox-wkf/GpkgValidatorFail", projectDir.resolve("gpkg-fail"));
        writeBuild(ioxWkfBuild("""
                tasks.register('validateCsvOk', CsvValidator) {
                    models = 'CsvModel'
                    modeldir = projectDir.toString()
                    dataFiles 'data1.csv'
                    firstLineIsHeader = false
                }

                tasks.register('validateCsvFail', CsvValidator) {
                    models = 'CsvModel'
                    modeldir = projectDir.toString()
                    dataFiles 'dataFail.csv'
                    firstLineIsHeader = false
                    failOnError.set(false)
                    doLast {
                        def result = layout.buildDirectory.file('csv-fail.txt').get().asFile
                        result.parentFile.mkdirs()
                        result.text = validationOk.toString()
                    }
                }

                tasks.register('validateJsonOk', JsonValidator) {
                    models = 'Test2'
                    modeldir = file('json-ok').toString()
                    dataFiles 'json-ok/structAttrList.json'
                }

                tasks.register('validateJsonFail', JsonValidator) {
                    models = 'Test2'
                    modeldir = file('json-fail').toString()
                    dataFiles 'json-fail/structAttrList.json'
                    failOnError.set(false)
                    doLast {
                        def result = layout.buildDirectory.file('json-fail.txt').get().asFile
                        result.parentFile.mkdirs()
                        result.text = validationOk.toString()
                    }
                }

                tasks.register('validateGpkgOk', GpkgValidator) {
                    models = 'GpkgModel'
                    modeldir = file('gpkg-ok').toString()
                    dataFiles 'gpkg-ok/attributes.gpkg'
                    tableName = 'Attributes'
                }

                tasks.register('validateGpkgFail', GpkgValidator) {
                    models = 'GpkgModel'
                    modeldir = file('gpkg-fail').toString()
                    dataFiles 'gpkg-fail/attributes.gpkg'
                    tableName = 'Attributes'
                    failOnError.set(false)
                    doLast {
                        def result = layout.buildDirectory.file('gpkg-fail.txt').get().asFile
                        result.parentFile.mkdirs()
                        result.text = validationOk.toString()
                    }
                }
                """));

        run("validateCsvOk", "validateCsvFail", "validateJsonOk", "validateJsonFail", "validateGpkgOk", "validateGpkgFail");

        assertFalse(Boolean.parseBoolean(Files.readString(projectDir.resolve("build/csv-fail.txt"), StandardCharsets.UTF_8)));
        assertFalse(Boolean.parseBoolean(Files.readString(projectDir.resolve("build/json-fail.txt"), StandardCharsets.UTF_8)));
        assertFalse(Boolean.parseBoolean(Files.readString(projectDir.resolve("build/gpkg-fail.txt"), StandardCharsets.UTF_8)));
    }

    private String ioxWkfBuild(String tasks) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Csv2Excel
                import ch.so.agi.gretl.tasks.CsvValidator
                import ch.so.agi.gretl.tasks.JsonValidator
                import ch.so.agi.gretl.tasks.GpkgValidator

                %s
                """.formatted(tasks);
    }

    private List<String> workbookValues(Path workbookPath) throws Exception {
        DataFormatter formatter = new DataFormatter();
        List<String> values = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(workbookPath.toFile())) {
            workbook.getSheetAt(0).forEach(row -> row.forEach(cell -> {
                String value = formatter.formatCellValue(cell);
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }));
        }
        return values;
    }
}
