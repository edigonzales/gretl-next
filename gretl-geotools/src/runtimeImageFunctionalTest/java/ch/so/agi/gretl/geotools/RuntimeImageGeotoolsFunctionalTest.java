package ch.so.agi.gretl.geotools;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("runtime-image")
@Tag("runtime-image-smoke")
class RuntimeImageGeotoolsFunctionalTest {
    @TempDir
    Path project;

    @Test
    void reclassifiesRasterAndVectorizesIt() throws Exception {
        copyResourceTree("fixtures/vectorize", project.resolve("vector-data"));
        copyResourceTree("fixtures/raster-reclassify", project.resolve("raster-data"));
        writeSettings();
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                import ch.so.agi.gretl.geotools.tasks.Vectorize
                System.setProperty('org.geotools.coverage.jaiext.enabled', 'true')
                tasks.register('reclassify', RasterReclassify) {
                    inputRaster 'raster-data/Beispiel_Rasterfile.asc'
                    outputRaster layout.buildDirectory.file('reclassified/reclass.tif').get().asFile
                    breaks 0d, 55d, 60d, 65d, 70d, 500d
                    noData(-100d)
                }
                tasks.register('vectorize', Vectorize) {
                    dependsOn 'reclassify'
                    inputRaster 'vector-data/reclass.tif'
                    outputGeopackage layout.buildDirectory.file('vectorized/output.gpkg').get().asFile
                    band 0
                    cellValues 55d, 65d
                }
                """);

        GretlBuildResult result = run("reclassify", "vectorize");
        assertTrue(result.successful(), result.output());
        assertTrue(Files.size(project.resolve("build/reclassified/reclass.tif")) > 0);
        assertTrue(Files.size(project.resolve("build/vectorized/output.gpkg")) > 0);
    }

    private GretlBuildResult run(String... arguments) {
        RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(
                RuntimeImageDescriptor.fromSystemProperties(), new DockerCli(), new ContainerUserResolver(),
                new RuntimeImageLifecycleArguments());
        List<String> requested = new ArrayList<>(List.of("--rerun-tasks"));
        requested.addAll(Arrays.asList(arguments));
        return executor.execute(GretlBuildRequest.builder(project)
                .arguments(requested)
                .timeout(Duration.ofMinutes(5))
                .runtimeImageOptions(RuntimeImageRunOptions.defaults())
                .build());
    }

    private void writeSettings() throws IOException {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-geotools'\n");
    }

    private void copyResourceTree(String resourcePath, Path target) throws IOException, URISyntaxException {
        Path source = Path.of(getClass().getClassLoader().getResource(resourcePath).toURI());
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                Path destination = target.resolve(source.relativize(path));
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }
}
