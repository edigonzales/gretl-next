package ch.so.agi.gretl.gradle;

import ch.so.agi.gretl.logging.Ehi2GretlAdapter;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.services.InterlisBuildService;
import ch.so.agi.gretl.tasks.AbstractInterlisTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public class GretlPlugin implements Plugin<Project> {

    public static final String INTERLIS_SERVICE_NAME = "gretlInterlisService";

    @Override
    public void apply(Project project) {
        LogEnvironment.initGradleIntegrated();
        Ehi2GretlAdapter.init();

        Provider<InterlisBuildService> interlisService = project.getGradle()
                .getSharedServices()
                .registerIfAbsent(INTERLIS_SERVICE_NAME, InterlisBuildService.class, spec ->
                        spec.getMaxParallelUsages().set(1));

        project.getTasks().withType(AbstractInterlisTask.class).configureEach(task -> {
            task.getInterlisService().set(interlisService);
            task.usesService(interlisService);
        });
    }
}
