package ch.so.agi.gretl.geotools;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoToolsPluginFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void appliesPluginAndRegistersDefaultReadTask() throws IOException {
        writeSettings();
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }
                """);

        BuildResult result = run("tasks", "--all");

        assertTrue(result.getOutput().contains("readShapefile"));
    }

    @Test
    void readsShapefileThroughWorkerIsolation() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/shapefile", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                tasks.named('readShapefile') {
                    shapefile 'data/data.shp'
                    crsCode 'EPSG:4326'
                }
                """);

        BuildResult result = run("readShapefile");

        assertTrue(result.getOutput().contains("Feature count:"));
        assertFalse(result.getOutput().contains("GRETL_WORKER|"));
    }

    @Test
    void vectorizesRasterThroughWorkerIsolation() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/vectorize", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                import ch.so.agi.gretl.geotools.tasks.Vectorize

                System.setProperty('org.geotools.coverage.jaiext.enabled', 'true')

                tasks.register('vectorize', Vectorize) {
                    inputRaster 'data/reclass.tif'
                    outputGeopackage layout.buildDirectory.file('vectorized/output.gpkg').get().asFile
                    band 0
                    cellValues 55d, 65d
                }
                """);

        BuildResult result = run("vectorize", "--info");

        assertTrue(Files.exists(projectDir.resolve("build/vectorized/output.gpkg")));
        assertTrue(result.getOutput().contains("GeoToolsWorkerRuntime: Dispatch operation: vectorize"));
        assertFalse(result.getOutput().contains("GRETL_WORKER|"));
    }

    @Test
    void reclassifiesRasterThroughWorkerIsolation() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/raster-reclassify", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                import ch.so.agi.gretl.geotools.tasks.RasterReclassify

                tasks.register('reclassify', RasterReclassify) {
                    inputRaster 'data/Beispiel_Rasterfile.asc'
                    outputRaster layout.buildDirectory.file('reclassified/reclass.tif').get().asFile
                }
                """);

        run("reclassify");

        assertTrue(Files.exists(projectDir.resolve("build/reclassified/reclass.tif")));
    }

    @Test
    void rejectsEmptyVectorizeCellValues() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/vectorize", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                import ch.so.agi.gretl.geotools.tasks.Vectorize

                tasks.register('vectorize', Vectorize) {
                    inputRaster 'data/reclass.tif'
                    outputGeopackage layout.buildDirectory.file('vectorized/output.gpkg').get().asFile
                    cellValues()
                }
                """);

        BuildResult result = runAndFail("vectorize");

        assertTrue(result.getOutput().contains("cellValues must not be empty"));
    }

    @Test
    void rejectsNonIncreasingRasterBreaks() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/raster-reclassify", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                import ch.so.agi.gretl.geotools.tasks.RasterReclassify

                tasks.register('reclassify', RasterReclassify) {
                    inputRaster 'data/Beispiel_Rasterfile.asc'
                    outputRaster layout.buildDirectory.file('reclassified/reclass.tif').get().asFile
                    breaks 0d, 60d, 55d
                }
                """);

        BuildResult result = runAndFail("reclassify");

        assertTrue(result.getOutput().contains("breaks must be strictly increasing"));
    }

    @Test
    void supportsKotlinDslForReadShapefile() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/shapefile", projectDir.resolve("data"));
        writeKotlinBuild("""
                import ch.so.agi.gretl.geotools.tasks.ReadShapefile

                plugins { id("ch.so.agi.gretl.geotools") }

                tasks.named<ReadShapefile>("readShapefile") {
                    shapefile("data/data.shp")
                    crsCode("EPSG:4326")
                }
                """);

        BuildResult result = run("readShapefile");

        assertTrue(result.getOutput().contains("Feature count:"));
    }

    @Test
    void supportsKotlinDslForRasterTasks() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("fixtures/vectorize", projectDir.resolve("vectorize-data"));
        copyResourceTree("fixtures/raster-reclassify", projectDir.resolve("raster-data"));
        writeKotlinBuild("""
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                import ch.so.agi.gretl.geotools.tasks.Vectorize

                plugins { id("ch.so.agi.gretl.geotools") }

                System.setProperty("org.geotools.coverage.jaiext.enabled", "true")

                tasks.register<Vectorize>("vectorize") {
                    inputRaster("vectorize-data/reclass.tif")
                    outputGeopackage(layout.buildDirectory.file("vectorized/output.gpkg").get().asFile)
                    band(0)
                    cellValues(55.0, 65.0)
                }

                tasks.register<RasterReclassify>("reclassify") {
                    inputRaster("raster-data/Beispiel_Rasterfile.asc")
                    outputRaster(layout.buildDirectory.file("reclassified/reclass.tif").get().asFile)
                    breaks(0.0, 55.0, 60.0, 65.0, 70.0, 500.0)
                    noData(-100.0)
                }
                """);

        run("vectorize", "reclassify");

        assertTrue(Files.exists(projectDir.resolve("build/vectorized/output.gpkg")));
        assertTrue(Files.exists(projectDir.resolve("build/reclassified/reclass.tif")));
    }

    private BuildResult run(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(appendStacktrace(arguments))
                .forwardOutput()
                .build();
    }

    private BuildResult runAndFail(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(appendStacktrace(arguments))
                .forwardOutput()
                .buildAndFail();
    }

    private String[] appendStacktrace(String[] arguments) {
        String[] result = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, result, 0, arguments.length);
        result[arguments.length] = "--stacktrace";
        return result;
    }

    private void writeSettings() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'geotools-test'\n", StandardCharsets.UTF_8);
    }

    private void writeBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

    private void writeKotlinBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), content, StandardCharsets.UTF_8);
    }

    private void copyResourceTree(String resourcePath, Path target) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing test resource: " + resourcePath);
        }
        Path source = Path.of(resource.toURI());
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
