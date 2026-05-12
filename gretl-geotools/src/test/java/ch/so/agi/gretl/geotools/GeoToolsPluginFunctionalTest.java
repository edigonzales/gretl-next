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
        copyResourceTree("data/shapefile", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                tasks.named('readShapefile') {
                    shapefile.set(layout.projectDirectory.file('data/data.shp'))
                    crsCode.set('EPSG:4326')
                }
                """);

        BuildResult result = run("readShapefile");

        assertTrue(result.getOutput().contains("Feature count:"));
    }

    @Test
    void vectorizesRasterThroughWorkerIsolation() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("data/vectorize", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                import ch.so.agi.gretl.geotools.tasks.Vectorize

                System.setProperty('org.geotools.coverage.jaiext.enabled', 'true')

                tasks.register('vectorize', Vectorize) {
                    inputRaster.set(layout.projectDirectory.file('data/reclass.tif'))
                    outputGeopackage.set(layout.buildDirectory.file('vectorized/output.gpkg'))
                    band.set(0)
                    cellValues.set([55d, 65d])
                }
                """);

        run("vectorize", "--info");

        assertTrue(Files.exists(projectDir.resolve("build/vectorized/output.gpkg")));
    }

    @Test
    void reclassifiesRasterThroughWorkerIsolation() throws IOException, URISyntaxException {
        writeSettings();
        copyResourceTree("data/raster-reclassify", projectDir.resolve("data"));
        writeBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }

                import ch.so.agi.gretl.geotools.tasks.RasterReclassify

                tasks.register('reclassify', RasterReclassify) {
                    inputRaster.set(layout.projectDirectory.file('data/Beispiel_Rasterfile.asc'))
                    outputRaster.set(layout.buildDirectory.file('reclassified/reclass.tif'))
                }
                """);

        run("reclassify");

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
