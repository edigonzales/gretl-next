package ch.so.agi.gretl.geotools;

import ch.so.agi.gretl.geotools.internal.EmbeddedWorkerClasspath;
import ch.so.agi.gretl.geotools.services.GeoToolsBuildService;
import ch.so.agi.gretl.geotools.tasks.GeoToolsTask;
import ch.so.agi.gretl.geotools.tasks.ReadShapefile;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Set;

public class GretlGeotoolsPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "gretlGeotools";
    public static final String GEOTOOLS_SERVICE_NAME = "gretlGeoToolsService";

    @Override
    public void apply(Project project) {
        GretlGeotoolsExtension extension = project.getExtensions()
                .create(EXTENSION_NAME, GretlGeotoolsExtension.class, project);

        Provider<Set<File>> embeddedWorkerClasspath = project.getProviders()
                .provider(() -> EmbeddedWorkerClasspath.resolve(project));
        extension.getWorkerClasspath().from(project.files(embeddedWorkerClasspath));

        Provider<GeoToolsBuildService> service = project.getGradle()
                .getSharedServices()
                .registerIfAbsent(GEOTOOLS_SERVICE_NAME, GeoToolsBuildService.class, spec -> {
                    spec.getMaxParallelUsages().set(1);
                });

        project.getTasks().withType(GeoToolsTask.class).configureEach(task -> {
            task.getWorkerClasspath().from(extension.getWorkerClasspath());
            task.getGeoToolsService().set(service);
            task.usesService(service);
        });

        project.getTasks().register("readShapefile", ReadShapefile.class, task -> {
            task.setGroup("gretl");
            task.setDescription("Reads a shapefile through the classloader-isolated GeoTools worker.");
            task.getShapefile().convention(project.getLayout().getProjectDirectory().file("data/example.shp"));
            task.getCrsCode().convention(extension.getDefaultCrsCode());
        });
    }
}
