package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedParallelExecutionFunctionalTest extends CombinedPluginTestSupport {
    @Test
    void multiProjectBuildWorksWithParallelExecution() throws Exception {
        configureWorkerProject();
        BuildResult result = run("aggregate", "--parallel", "--max-workers=4", "--rerun-tasks");
        assertTrue(result.getOutput().contains(":mixed-a:reclassify"));
        assertTrue(result.getOutput().contains(":mixed-b:reclassify"));
    }

    @Test
    void multipleCoreAndGeoToolsTasksProduceCorrectOutputs() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                tasks.register('coreOne') { doLast { println 'CORE_ONE' } }
                tasks.register('geoOne') { doLast { println 'GEO_ONE' } }
                tasks.register('aggregate') { dependsOn 'coreOne', 'geoOne' }
                """);
        BuildResult result = run("aggregate", "--parallel", "--max-workers=4", "--rerun-tasks");
        assertTrue(result.getOutput().contains("CORE_ONE"));
        assertTrue(result.getOutput().contains("GEO_ONE"));
    }

    @Test
    void workersDoNotCorruptExtractedClasspath() throws Exception {
        configureWorkerProject();
        run("aggregate", "--parallel", "--max-workers=4", "--rerun-tasks");
        assertTrue(Files.size(projectPath("mixed-a/build/geo/reclassified.tif")) > 0);
        assertTrue(Files.size(projectPath("mixed-b/build/geo/reclassified.tif")) > 0);
    }

    @Test
    void serviceRegistrationHasNoRace() throws Exception {
        configureWorkerProject();
        BuildResult result = run("aggregate", "--parallel", "--max-workers=4", "--rerun-tasks");
        assertTrue(!result.getOutput().contains("already registered"));
    }

    @Test
    void workerExtractionHasNoRace() throws Exception {
        configureWorkerProject();
        run("aggregate", "--parallel", "--max-workers=4", "--rerun-tasks");
        assertTrue(Files.isRegularFile(projectPath("mixed-a/build/geo/reclassified.tif")));
        assertTrue(Files.isRegularFile(projectPath("mixed-b/build/geo/reclassified.tif")));
    }

    @Test
    void outputsDoNotLeakBetweenProjects() throws Exception {
        configureWorkerProject();
        run("aggregate", "--parallel", "--max-workers=4", "--rerun-tasks");
        assertTrue(Files.isRegularFile(projectPath("mixed-a/build/generated/raster.asc")));
        assertTrue(Files.isRegularFile(projectPath("mixed-b/build/generated/raster.asc")));
    }

    private void configureWorkerProject() throws Exception {
        writeSettingsWithIncludes("parallel-root", "mixed-a", "mixed-b");
        writeGroovyBuild("tasks.register('aggregate') { dependsOn ':mixed-a:reclassify', ':mixed-b:reclassify' }\n");
        for (String name : new String[] {"mixed-a", "mixed-b"}) {
            copyResourceTree("fixtures/combined-pipeline", projectPath(name + "/input"));
            Files.writeString(projectPath(name + "/build.gradle"), """
                    import ch.so.agi.gretl.tasks.XslTransformer
                    import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                    plugins {
                        id 'ch.so.agi.gretl'
                        id 'ch.so.agi.gretl.geotools'
                    }
                    def generate = tasks.register('generate', XslTransformer) {
                        xslFile 'input/raster-to-asc.xsl'
                        xmlFiles 'input/raster.xml'
                        outDirectory layout.buildDirectory.dir('generated')
                        fileExtension 'asc'
                    }
                    tasks.register('reclassify', RasterReclassify) {
                        inputRaster generate.flatMap { it.outDirectory.file('raster.asc') }
                        outputRaster layout.buildDirectory.file('geo/reclassified.tif')
                    }
                    """);
        }
    }
}
