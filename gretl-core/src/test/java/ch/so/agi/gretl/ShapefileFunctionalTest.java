package ch.so.agi.gretl;

import ch.so.agi.gretl.internal.shapefile.core.DbfReader;
import ch.so.agi.gretl.internal.shapefile.core.ShapeType;
import ch.so.agi.gretl.internal.shapefile.core.ShpReader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapefileFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void convertsGeoPackageTablesToShapefiles() throws Exception {
        writeSettings();
        copyResource("fixtures/iox-wkf/Gpkg2Shp/ch.so.agi_av_gb_administrative_einteilungen_2020-08-20.gpkg", "data.gpkg");
        writeBuild(shapefileBuild("""
                tasks.register('convert', Gpkg2Shp) {
                    dataFile = file('data.gpkg')
                    outputDir = layout.buildDirectory.dir('shp')
                }
                """));

        run("convert");

        assertRecordCount(projectDir.resolve("build/shp/nachfuehrngskrise_gemeinde.shp"), 109, ShapeType.POLYGON);
        assertRecordCount(projectDir.resolve("build/shp/grundbuchkreise_grundbuchkreis.shp"), 127, ShapeType.POLYGON);
        assertTrue(Files.readString(projectDir.resolve("build/shp/grundbuchkreise_grundbuchkreis.prj")).contains("CH1903+"));
        try (DbfReader dbf = DbfReader.open(projectDir.resolve("build/shp/grundbuchkreise_grundbuchkreis.dbf"), StandardCharsets.UTF_8)) {
            assertTrue(dbf.fields().stream().anyMatch(field -> field.name().equals("plz")));
        }
    }

    @Test
    void convertsGeoPackageTablesWithoutGeometryToNullShapeShapefiles() throws Exception {
        writeSettings();
        copyResource("fixtures/iox-wkf/Gpkg2Shp/aggloprogramme.gpkg", "data.gpkg");
        writeBuild(shapefileBuild("""
                tasks.register('convert', Gpkg2Shp) {
                    dataFile = file('data.gpkg')
                    outputDir = layout.buildDirectory.dir('shp')
                }
                """));

        run("convert");

        Path shp = projectDir.resolve("build/shp/massnahmen.shp");
        assertRecordCount(shp, 451, ShapeType.NULL);
        try (DbfReader dbf = DbfReader.open(shp.resolveSibling("massnahmen.dbf"), StandardCharsets.UTF_8)) {
            assertEquals(34, dbf.fields().size());
        }
    }

    @Test
    void trimsLongGeoPackageStringsForShapefileDbfFields() throws Exception {
        writeSettings();
        copyResource("fixtures/iox-wkf/Gpkg2Shp/wanderwege.gpkg", "data.gpkg");
        writeBuild(shapefileBuild("""
                tasks.register('convert', Gpkg2Shp) {
                    dataFile = file('data.gpkg')
                    outputDir = layout.buildDirectory.dir('shp')
                }
                """));

        run("convert");

        Path dbfPath = projectDir.resolve("build/shp/wanderwege_route.dbf");
        try (DbfReader dbf = DbfReader.open(dbfPath, StandardCharsets.UTF_8)) {
            boolean found = false;
            var record = dbf.readNext();
            while (record.isPresent()) {
                if (record.get().values().stream().anyMatch(value -> value.contains("TRUNCATED"))) {
                    found = true;
                }
                record = dbf.readNext();
            }
            assertTrue(found);
        }
    }

    @Test
    void validatesShapefileAndExposesValidationResult() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/iox-wkf/ShpValidator", projectDir.resolve("ok"));
        copyResourceTree("fixtures/iox-wkf/ShpValidatorFail", projectDir.resolve("fail"));
        writeBuild(shapefileBuild("""
                tasks.register('validateOk', ShpValidator) {
                    models = 'ShpModel'
                    modeldir = file('ok').toString()
                    dataFiles = files('ok/data.shp')
                }

                tasks.register('validateFail', ShpValidator) {
                    models = 'ShpModel'
                    modeldir = file('fail').toString()
                    dataFiles = files('fail/data.shp')
                    logFile = layout.buildDirectory.file('shpvalidator.log')
                    failOnError.set(false)
                    doLast {
                        def result = layout.buildDirectory.file('shp-fail.txt').get().asFile
                        result.parentFile.mkdirs()
                        result.text = validationOk.toString()
                    }
                }
                """));

        run("validateOk", "validateFail");

        assertFalse(Boolean.parseBoolean(Files.readString(projectDir.resolve("build/shp-fail.txt"), StandardCharsets.UTF_8)));
        String log = Files.readString(projectDir.resolve("build/shpvalidator.log"), StandardCharsets.UTF_8);
        assertTrue(log.contains("value rot is not a member of the enumeration in attribute aenum"), log);
        assertTrue(log.contains("...validation failed"), log);
    }

    private String shapefileBuild(String tasks) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Gpkg2Shp
                import ch.so.agi.gretl.tasks.ShpValidator

                %s
                """.formatted(tasks);
    }

    private void assertRecordCount(Path shpPath, int expectedCount, ShapeType expectedShapeType) throws Exception {
        try (ShpReader shp = ShpReader.open(shpPath)) {
            assertEquals(expectedShapeType, shp.header().shapeType());
            int count = 0;
            while (shp.readNext().isPresent()) {
                count++;
            }
            assertEquals(expectedCount, count);
        }
    }
}
