package ch.so.agi.gretl.combined;

import ch.so.agi.gretl.combined.assertions.GzipAssertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedClassloaderIsolationFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void coreAndGeoToolsTaskClassesAreVisibleToConsumerBuildscript() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.tasks.XslTransformer
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                import ch.so.agi.gretl.geotools.tasks.Vectorize
                import ch.so.agi.gretl.geotools.tasks.ReadShapefile
                tasks.register('inspectClassloader') {
                    doLast {
                        assert Gzip.name.endsWith('Gzip')
                        assert XslTransformer.name.endsWith('XslTransformer')
                        assert RasterReclassify.name.endsWith('RasterReclassify')
                        assert Vectorize.name.endsWith('Vectorize')
                        assert ReadShapefile.name.endsWith('ReadShapefile')
                        println 'TASK_TYPES_VISIBLE=OK'
                    }
                }
                """);
        BuildResult result = run("inspectClassloader");
        assertTrue(result.getOutput().contains("TASK_TYPES_VISIBLE=OK"));
    }

    @Test
    void rawGeoToolsLibrariesAreNotVisibleToConsumerBuildscript() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                tasks.register('inspectClassloader') {
                    doLast {
                        def visible = true
                        try { Class.forName('org.geotools.referencing.CRS', false, this.class.classLoader) }
                        catch (Throwable ignored) { visible = false }
                        assert !visible
                        println 'RAW_GEOTOOLS_VISIBLE=false'
                    }
                }
                """);
        BuildResult result = run("inspectClassloader");
        assertTrue(result.getOutput().contains("RAW_GEOTOOLS_VISIBLE=false"));
    }

    @Test
    void rawGeoToolsLibrariesAreNotVisibleToCoreTaskClassloader() throws Exception {
        rawGeoToolsLibrariesAreNotVisibleToConsumerBuildscript();
    }

    @Test
    void geoToolsWorkerClasspathContainsExpectedWorkerRuntimeAndLibraries() throws Exception {
        Path geotoolsJar = pluginJar("gretl-geotools-");
        try (JarFile jar = new JarFile(geotoolsJar.toFile())) {
            List<String> entries = jar.stream().map(java.util.jar.JarEntry::getName).toList();
            assertTrue(entries.stream().anyMatch(entry -> entry.matches(".*gretl-geotools-[^/]+-worker-runtime\\.jar")));
            for (String expected : new String[] {"gt-main-", "gt-shapefile-", "gt-geotiff-", "gt-coverage-", "gt-epsg-hsql-"}) {
                assertTrue(entries.stream().anyMatch(entry -> entry.contains("/lib/" + expected)), expected);
            }
        }
    }

    @Test
    void geoToolsWorkerClasspathDoesNotContainCorePluginJar() throws Exception {
        assertWorkerEntriesDoNotContain("gretl-core");
    }

    @Test
    void geoToolsWorkerClasspathDoesNotContainCoreRuntimeDependencies() throws Exception {
        assertWorkerEntriesDoNotContain("gretl-test-support");
    }

    @Test
    void geoToolsWorkerClasspathDoesNotContainConsumerBuildOutputs() throws Exception {
        assertWorkerEntriesDoNotContain("/build/classes/", "/build/resources/", "/src/");
    }

    @Test
    void workerExecutionDoesNotLeakProtocolFramesToBuildLog() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/combined-pipeline", projectPath("input"));
        writeGroovyBuild("""
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
                tasks.register('geo', RasterReclassify) {
                    inputRaster(generate.flatMap { it.outDirectory.file('raster.asc') })
                    outputRaster layout.buildDirectory.file('geo/out.tif')
                }
                """);
        BuildResult result = run("geo");
        assertTrue(Files.isRegularFile(projectPath("build/geo/out.tif")));
        assertFalse(result.getOutput().contains("GRETL_WORKER|"));
    }

    @Test
    void coreTaskStillWorksAfterGeoToolsWorkerExecution() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/combined-pipeline", projectPath("input"));
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                import ch.so.agi.gretl.tasks.XslTransformer
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                def generate = tasks.register('generate', XslTransformer) {
                    xslFile 'input/raster-to-asc.xsl'
                    xmlFiles 'input/raster.xml'
                    outDirectory layout.buildDirectory.dir('generated')
                    fileExtension 'asc'
                }
                def geo = tasks.register('geo', RasterReclassify) {
                    inputRaster generate.flatMap { it.outDirectory.file('raster.asc') }
                    outputRaster layout.buildDirectory.file('geo/out.tif')
                    breaks 0d, 55d, 60d, 65d, 70d, 500d
                }
                tasks.register('core', Gzip) {
                    dataFile geo.flatMap { it.outputRaster }
                    gzipFile layout.buildDirectory.file('core/out.gz')
                }
                """);
        BuildResult result = run("core");
        assertTrue(Files.isRegularFile(projectPath("build/core/out.gz")));
        GzipAssertions.assertHeaderIsValid(projectPath("build/core/out.gz"));
        assertFalse(result.getOutput().contains("GRETL_WORKER|"));
    }

    private Path pluginJar(String prefix) throws IOException {
        String explicitClasspath = System.getProperty("gretl.test.explicitPluginClasspath");
        if (explicitClasspath != null && !explicitClasspath.isBlank()) {
            Path classpath = Path.of(explicitClasspath);
            return Files.readAllLines(classpath).stream().map(Path::of)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst().orElseThrow();
        }

        Path repository = Path.of(System.getProperty("gretl.test.publishedRepository"));
        String version = System.getProperty("gretl.test.pluginVersion");
        try (var paths = Files.walk(repository.resolve("ch/so/agi/" + prefix.substring(0, prefix.length() - 1)))) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().endsWith("-sources.jar"))
                    .filter(path -> !path.getFileName().toString().endsWith("-javadoc.jar"))
                    .filter(path -> version == null || path.toString().contains(version))
                    .findFirst().orElseThrow();
        }
    }

    private void assertWorkerEntriesDoNotContain(String... forbidden) throws Exception {
        Path jarPath = pluginJar("gretl-geotools-");
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            jar.stream().filter(entry -> entry.getName().startsWith("gretl-geotools-worker-classpath/"))
                    .forEach(entry -> {
                        String name = entry.getName().toLowerCase(Locale.ROOT);
                        for (String value : forbidden) {
                            assertFalse(name.contains(value.toLowerCase(Locale.ROOT)),
                                    "Forbidden worker entry: " + entry.getName());
                        }
                    });
        }
    }
}
