package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedFailurePropagationFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void coreFailurePreventsGeoToolsAndDownstreamCoreExecution() throws Exception {
        configurePipeline();
        Files.delete(projectPath("input/raster.xml"));
        BuildResult result = runAndFail("packageRaster");
        assertTaskNotExecuted(result, ":reclassifyRaster");
        assertTaskNotExecuted(result, ":packageRaster");
        assertTrue(result.getOutput().contains("raster.xml"));
    }

    @Test
    void geoToolsFailurePreventsDownstreamCoreExecution() throws Exception {
        configurePipeline();
        String build = Files.readString(projectPath("build.gradle"), StandardCharsets.UTF_8)
                .replace("breaks 0d, 55d, 60d, 65d, 70d, 500d", "breaks 0d, 60d, 55d");
        Files.writeString(projectPath("build.gradle"), build, StandardCharsets.UTF_8);
        BuildResult result = runAndFail("packageRaster");
        assertTaskNotExecuted(result, ":packageRaster");
        assertTrue(result.getOutput().contains("breaks must be strictly increasing"));
    }

    @Test
    void fixingCoreInputAllowsSubsequentSuccessfulBuild() throws Exception {
        configurePipeline();
        Files.delete(projectPath("input/raster.xml"));
        runAndFail("packageRaster");
        copyResource("fixtures/combined-pipeline/raster.xml", "input/raster.xml");
        assertSuccessfulPipeline(run("packageRaster"));
    }

    @Test
    void fixingGeoToolsConfigurationAllowsSubsequentSuccessfulBuild() throws Exception {
        configurePipeline();
        String broken = Files.readString(projectPath("build.gradle"), StandardCharsets.UTF_8)
                .replace("breaks 0d, 55d, 60d, 65d, 70d, 500d", "breaks 0d, 60d, 55d");
        Files.writeString(projectPath("build.gradle"), broken, StandardCharsets.UTF_8);
        runAndFail("packageRaster");
        Files.writeString(projectPath("build.gradle"), broken.replace("breaks 0d, 60d, 55d", "breaks 0d, 55d, 60d, 65d, 70d, 500d"), StandardCharsets.UTF_8);
        assertSuccessfulPipeline(run("packageRaster"));
    }

    @Test
    void failedWorkerExecutionDoesNotPoisonLaterCoreBuild() throws Exception {
        configurePipeline();
        String broken = Files.readString(projectPath("build.gradle"), StandardCharsets.UTF_8)
                .replace("breaks 0d, 55d, 60d, 65d, 70d, 500d", "breaks 0d, 60d, 55d");
        Files.writeString(projectPath("build.gradle"), broken, StandardCharsets.UTF_8);
        runAndFail("packageRaster");
        Files.writeString(projectPath("build.gradle"), broken.replace("breaks 0d, 60d, 55d", "breaks 0d, 55d, 60d, 65d, 70d, 500d"), StandardCharsets.UTF_8);
        assertSuccessfulPipeline(run("packageRaster"));
    }

    @Test
    void failedGzipLeavesNoPartialOutput() throws Exception {
        configurePipeline();
        Files.delete(projectPath("input/raster.xml"));
        BuildResult result = runAndFail("packageRaster");
        assertFalse(Files.exists(projectPath("build/distribution/reclassified.tif.gz")));
        assertTrue(result.getOutput().contains("raster.xml"));
    }

    @Test
    void downstreamCoreFailurePreservesValidGeoToolsOutput() throws Exception {
        configurePipeline();
        run("reclassifyRaster");
        String build = Files.readString(projectPath("build.gradle"), StandardCharsets.UTF_8)
                .replace("dataFile reclassifyRaster.flatMap { it.outputRaster }",
                        "dataFile layout.buildDirectory.file('missing/input.tif')");
        Files.writeString(projectPath("build.gradle"), build, StandardCharsets.UTF_8);
        runAndFail("packageRaster");
        assertTrue(Files.isRegularFile(projectPath("build/geotools/reclassified.tif")));
    }

    private void configurePipeline() throws Exception {
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
                def reclassifyRaster = tasks.register('reclassifyRaster', RasterReclassify) {
                    inputRaster generateRaster.flatMap { it.outDirectory.file('raster.asc') }
                    outputRaster layout.buildDirectory.file('geotools/reclassified.tif')
                    breaks 0d, 55d, 60d, 65d, 70d, 500d
                    noData(-100d)
                }
                tasks.register('packageRaster', Gzip) {
                    dataFile reclassifyRaster.flatMap { it.outputRaster }
                    gzipFile layout.buildDirectory.file('distribution/reclassified.tif.gz')
                }
                """);
    }

    private void assertSuccessfulPipeline(BuildResult result) {
        TaskOutcome generateOutcome = result.task(":generateRaster").getOutcome();
        assertTrue(generateOutcome == TaskOutcome.SUCCESS || generateOutcome == TaskOutcome.UP_TO_DATE,
                "Unexpected XSLT outcome: " + generateOutcome);
        assertTaskOutcome(result, ":reclassifyRaster", TaskOutcome.SUCCESS);
        assertTaskOutcome(result, ":packageRaster", TaskOutcome.SUCCESS);
    }
}
