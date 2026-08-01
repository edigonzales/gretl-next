package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageGeoToolsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsShapefileAndResolvesEpsgOffline() throws Exception {
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("shapefile"));
        copyFixture("data.shp", project.path("data/data.shp"));
        copyFixture("data.shx", project.path("data/data.shx"));
        copyFixture("data.dbf", project.path("data/data.dbf"));
        copyFixture("data.prj", project.path("data/data.prj"));
        project.settingsGroovy("rootProject.name = 'shapefile'\n")
                .buildGroovy("""
                        plugins {
                            id 'ch.so.agi.gretl'
                            id 'ch.so.agi.gretl.geotools'
                        }
                        tasks.named('readShapefile') {
                            shapefile 'data/data.shp'
                            crsCode 'EPSG:2056'
                        }
                        """);

        GretlBuildResult result = run(project, "readShapefile", Duration.ofMinutes(4));
        assertTrue(result.successful(), result.output());
        assertTrue(result.output().contains("Feature count:"), result.output());
        assertTrue(result.output().contains("EPSG:2056"), result.output());
    }

    @Test
    void reclassifiesRasterWithWorkerRuntimeOffline() throws Exception {
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("raster"));
        copyFixture("Beispiel_Rasterfile.asc", project.path("data/input.asc"),
                "raster-reclassify/Beispiel_Rasterfile.asc");
        project.settingsGroovy("rootProject.name = 'raster'\n")
                .buildGroovy("""
                        plugins {
                            id 'ch.so.agi.gretl'
                            id 'ch.so.agi.gretl.geotools'
                        }
                        tasks.register('reclassify', ch.so.agi.gretl.geotools.tasks.RasterReclassify) {
                            inputRaster 'data/input.asc'
                            outputRaster 'build/output.tif'
                            breaks 0, 55, 60, 65, 70, 500
                            noData(-100)
                        }
                        """);

        GretlBuildResult result = run(project, "reclassify", Duration.ofMinutes(6));
        assertTrue(result.successful(), result.output());
        assertTrue(Files.size(project.path("build/output.tif")) > 0, result.output());
    }

    private GretlBuildResult run(GradleTestProject project, String task, Duration timeout) {
        return new RuntimeImageBuildExecutor(RuntimeImageDescriptor.fromSystemProperties(),
                new ch.so.agi.gretl.test.docker.DockerCli(),
                new ch.so.agi.gretl.test.docker.ContainerUserResolver(),
                new ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments()).execute(
                GretlBuildRequest.builder(project.directory())
                        .arguments(List.of("--rerun-tasks", task))
                        .timeout(timeout)
                        .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                        .build());
    }

    private void copyFixture(String name, Path target) throws IOException {
        copyFixture(name, target, "shapefile/" + name);
    }

    private void copyFixture(String name, Path target, String relativeFixture) throws IOException {
        Path repository = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (repository != null && !Files.isDirectory(repository.resolve("gretl-geotools"))) {
            repository = repository.getParent();
        }
        if (repository == null) {
            throw new IOException("Cannot locate repository root for GeoTools fixture " + relativeFixture);
        }
        Path source = repository.resolve("gretl-geotools/src/test/resources/fixtures").resolve(relativeFixture);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
