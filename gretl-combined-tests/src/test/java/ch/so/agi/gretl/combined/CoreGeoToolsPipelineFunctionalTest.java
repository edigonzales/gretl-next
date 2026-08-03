package ch.so.agi.gretl.combined;

import ch.so.agi.gretl.combined.assertions.AsciiGridAssertions;
import ch.so.agi.gretl.combined.assertions.GeoTiffAssertions;
import ch.so.agi.gretl.combined.assertions.GzipAssertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreGeoToolsPipelineFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void executesPipelineWithGroovyDsl() throws Exception {
        configureGroovyPipeline();

        BuildResult result = run("packageRaster", "--info");

        assertPipelineOutcomes(result, TaskOutcome.SUCCESS);
        assertPipelineOutputs();
        assertNoCombinedPluginWarnings(result);
    }

    @Test
    void infersDependenciesFromProviders() throws Exception {
        configureGroovyPipeline();

        BuildResult result = run("packageRaster");

        assertPipelineOutcomes(result, TaskOutcome.SUCCESS);
        assertTrue(result.getOutput().indexOf(":generateRaster")
                        < result.getOutput().indexOf(":reclassifyRaster"));
        assertTrue(result.getOutput().indexOf(":reclassifyRaster")
                        < result.getOutput().indexOf(":packageRaster"));
    }

    @Test
    void producesCorrectAsciiGrid() throws Exception {
        configureGroovyPipeline();
        run("generateRaster");

        AsciiGridAssertions.AsciiGrid grid = AsciiGridAssertions.read(
                projectPath("build/generated/raster.asc"));
        AsciiGridAssertions.assertDimensions(grid, 4, 3);
        AsciiGridAssertions.assertCellSize(grid, 1d);
        AsciiGridAssertions.assertNoData(grid, -9999d);
        AsciiGridAssertions.assertValues(grid, expectedInputValues());
        assertEquals("2600000", grid.headers().get("xllcorner"));
        assertEquals("1200000", grid.headers().get("yllcorner"));
    }

    @Test
    void producesCorrectReclassifiedGeoTiff() throws Exception {
        configureGroovyPipeline();
        run("reclassifyRaster");

        GeoTiffAssertions.RasterSummary summary = GeoTiffAssertions.read(
                projectPath("build/geotools/reclassified.tif"));
        GeoTiffAssertions.assertDimensions(summary, 4, 3);
        assertEquals(1, summary.bands());
        GeoTiffAssertions.assertCrs(summary, "EPSG:2056");
        GeoTiffAssertions.assertNoData(summary, -100d);
        GeoTiffAssertions.assertBandValues(summary, 0, expectedClasses());
        assertTrue(summary.envelope().getWidth() > 0);
        assertTrue(summary.envelope().getHeight() > 0);
    }

    @Test
    void packagesExactGeoTiffBytesIntoGzip() throws Exception {
        configureGroovyPipeline();
        run("packageRaster");

        GzipAssertions.assertHeaderIsValid(projectPath("build/distribution/reclassified.tif.gz"));
        GzipAssertions.assertDecompressesToFile(
                projectPath("build/distribution/reclassified.tif.gz"),
                projectPath("build/geotools/reclassified.tif"));
    }

    @Test
    void configurationCacheCanBeStoredAndReused() throws Exception {
        configureGroovyPipeline();

        BuildResult first = run("packageRaster", "--configuration-cache", "--info");
        BuildResult second = run("packageRaster", "--configuration-cache", "--info");

        assertPipelineOutcomes(first, TaskOutcome.SUCCESS);
        assertPipelineOutcomes(second, TaskOutcome.UP_TO_DATE);
        assertTrue(first.getOutput().contains("Configuration cache entry stored")
                        || first.getOutput().contains("Configuration cache entry reused"), first.getOutput());
        assertTrue(second.getOutput().contains("Configuration cache entry reused"), second.getOutput());
    }

    @Test
    void appliesBothPluginOrdersWithoutDuplicateRegistration() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl.geotools'
                    id 'ch.so.agi.gretl'
                }
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.tasks.XslTransformer
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                tasks.register('inspectCombined') {
                    doLast {
                        assert pluginManager.hasPlugin('ch.so.agi.gretl')
                        assert pluginManager.hasPlugin('ch.so.agi.gretl.geotools')
                        assert tasks.withType(Gzip).size() == 1
                        assert tasks.withType(XslTransformer).isEmpty()
                        assert tasks.withType(RasterReclassify).isEmpty()
                        assert tasks.names.count { it == 'readShapefile' } == 1
                        println 'COMBINED_PLUGIN_REAPPLICATION=OK'
                    }
                }
                pluginManager.apply('ch.so.agi.gretl')
                pluginManager.apply('ch.so.agi.gretl.geotools')
                tasks.register('coreCanary', Gzip)
                """);

        BuildResult result = run("inspectCombined");

        assertTrue(result.getOutput().contains("COMBINED_PLUGIN_REAPPLICATION=OK"));
        assertFalse(result.getOutput().contains("already registered"));
    }

    private void configureGroovyPipeline() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/combined-pipeline", projectPath("input"));
        writeGroovyBuild("""
                import ch.so.agi.gretl.tasks.XslTransformer
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify

                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }

                def generateRaster = tasks.register('generateRaster', XslTransformer) {
                    xslFile 'input/raster-to-asc.xsl'
                    xmlFiles 'input/raster.xml'
                    outDirectory layout.buildDirectory.dir('generated')
                    fileExtension 'asc'
                }

                def generatedRaster = generateRaster.flatMap { it.outDirectory.file('raster.asc') }

                def reclassifyRaster = tasks.register('reclassifyRaster', RasterReclassify) {
                    inputRaster.set(generatedRaster)
                    outputRaster layout.buildDirectory.file('geotools/reclassified.tif')
                    breaks 0d, 55d, 60d, 65d, 70d, 500d
                    noData(-100d)
                }

                tasks.register('packageRaster', Gzip) {
                    dataFile(reclassifyRaster.flatMap { it.outputRaster })
                    gzipFile layout.buildDirectory.file('distribution/reclassified.tif.gz')
                }
                """);
    }

    private void assertPipelineOutcomes(BuildResult result, TaskOutcome outcome) {
        assertTaskOutcome(result, ":generateRaster", outcome);
        assertTaskOutcome(result, ":reclassifyRaster", outcome);
        assertTaskOutcome(result, ":packageRaster", outcome);
    }

    private void assertPipelineOutputs() {
        assertTrue(Files.isRegularFile(projectPath("build/generated/raster.asc")));
        assertTrue(Files.isRegularFile(projectPath("build/geotools/reclassified.tif")));
        assertTrue(Files.isRegularFile(projectPath("build/distribution/reclassified.tif.gz")));
    }

    private double[][] expectedInputValues() {
        return new double[][] {
                {10, 56, 61, 71},
                {54, 59, 64, 499},
                {-9999, 0, 70, 500}
        };
    }

    private double[][] expectedClasses() {
        return new double[][] {
                {0, 55, 60, 70},
                {0, 55, 60, 70},
                {-100, 0, 70, 70}
        };
    }
}
