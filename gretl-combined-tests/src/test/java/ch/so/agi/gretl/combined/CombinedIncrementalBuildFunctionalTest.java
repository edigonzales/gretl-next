package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedIncrementalBuildFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void secondPipelineRunIsUpToDate() throws Exception {
        configurePipeline();
        assertPipeline(run("packageRaster"), TaskOutcome.SUCCESS);
        assertPipeline(run("packageRaster"), TaskOutcome.UP_TO_DATE);
    }

    @Test
    void changingCoreInputInvalidatesEntirePipeline() throws Exception {
        configurePipeline();
        run("packageRaster");
        Files.writeString(projectPath("input/raster.xml"),
                Files.readString(projectPath("input/raster.xml"), StandardCharsets.UTF_8).replace("10 56", "11 56"),
                StandardCharsets.UTF_8);
        assertPipeline(run("packageRaster"), TaskOutcome.SUCCESS);
    }

    @Test
    void deletingGeoToolsOutputRerunsGeoToolsAndDownstreamCore() throws Exception {
        configurePipeline();
        run("packageRaster");
        Files.delete(projectPath("build/geotools/reclassified.tif"));
        BuildResult result = run("packageRaster");
        assertTaskOutcome(result, ":generateRaster", TaskOutcome.UP_TO_DATE);
        assertTaskOutcome(result, ":reclassifyRaster", TaskOutcome.SUCCESS);
        assertTaskOutcome(result, ":packageRaster", TaskOutcome.SUCCESS);
    }

    @Test
    void changingBreaksRerunsGeoToolsAndDownstreamCore() throws Exception {
        configurePipeline();
        run("packageRaster");
        String build = Files.readString(projectPath("build.gradle"), StandardCharsets.UTF_8)
                .replace("breaks 0d, 55d, 60d, 65d, 70d, 500d", "breaks 0d, 55d, 60d, 65d, 70d, 600d");
        Files.writeString(projectPath("build.gradle"), build, StandardCharsets.UTF_8);
        BuildResult result = run("packageRaster");
        assertTaskOutcome(result, ":generateRaster", TaskOutcome.UP_TO_DATE);
        assertTaskOutcome(result, ":reclassifyRaster", TaskOutcome.SUCCESS);
        assertTaskOutcome(result, ":packageRaster", TaskOutcome.SUCCESS);
    }

    @Test
    void deletingOnlyGzipOutputRerunsOnlyGzip() throws Exception {
        configurePipeline();
        run("packageRaster");
        Files.delete(projectPath("build/distribution/reclassified.tif.gz"));
        BuildResult result = run("packageRaster");
        assertTaskOutcome(result, ":generateRaster", TaskOutcome.UP_TO_DATE);
        assertTaskOutcome(result, ":reclassifyRaster", TaskOutcome.UP_TO_DATE);
        assertTaskOutcome(result, ":packageRaster", TaskOutcome.SUCCESS);
    }

    @Test
    void changingOnlyGzipDestinationDoesNotRerunUpstreamTasks() throws Exception {
        configurePipeline();
        run("packageRaster");
        String build = Files.readString(projectPath("build.gradle"), StandardCharsets.UTF_8)
                .replace("distribution/reclassified.tif.gz", "distribution/reclassified-copy.tif.gz");
        Files.writeString(projectPath("build.gradle"), build, StandardCharsets.UTF_8);
        BuildResult result = run("packageRaster");
        assertTaskOutcome(result, ":generateRaster", TaskOutcome.UP_TO_DATE);
        assertTaskOutcome(result, ":reclassifyRaster", TaskOutcome.UP_TO_DATE);
        assertTaskOutcome(result, ":packageRaster", TaskOutcome.SUCCESS);
    }

    @Test
    void cleanRemovesAllGeneratedPipelineOutputs() throws Exception {
        configurePipeline();
        run("packageRaster");
        run("clean");
        assertFalse(Files.exists(projectPath("build/generated/raster.asc")));
        assertFalse(Files.exists(projectPath("build/geotools/reclassified.tif")));
        assertFalse(Files.exists(projectPath("build/distribution/reclassified.tif.gz")));
    }

    @Test
    void rerunTasksExecutesEntirePipeline() throws Exception {
        configurePipeline();
        run("packageRaster");
        assertPipeline(run("packageRaster", "--rerun-tasks"), TaskOutcome.SUCCESS);
    }

    private void configurePipeline() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/combined-pipeline", projectPath("input"));
        writeGroovyBuild("""
                import ch.so.agi.gretl.tasks.XslTransformer
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                plugins {
                    id 'base'
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

    private void assertPipeline(BuildResult result, TaskOutcome expected) {
        assertTaskOutcome(result, ":generateRaster", expected);
        assertTaskOutcome(result, ":reclassifyRaster", expected);
        assertTaskOutcome(result, ":packageRaster", expected);
        assertTrue(Files.isRegularFile(projectPath("build/distribution/reclassified.tif.gz")));
    }
}
