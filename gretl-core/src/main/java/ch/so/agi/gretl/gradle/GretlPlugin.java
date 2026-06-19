package ch.so.agi.gretl.gradle;

import ch.so.agi.gretl.logging.Ehi2GretlAdapter;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.services.CoreGretlBuildService;
import ch.so.agi.gretl.services.InterlisBuildService;
import ch.so.agi.gretl.tasks.AbstractCoreGretlTask;
import ch.so.agi.gretl.tasks.AbstractInterlisTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public class GretlPlugin implements Plugin<Project> {

    public static final String CORE_SERVICE_NAME = "gretlCoreService";
    public static final String INTERLIS_SERVICE_NAME = "gretlInterlisService";

    @Override
    public void apply(Project project) {
        LogEnvironment.initGradleIntegrated();
        Ehi2GretlAdapter.init();

        Provider<CoreGretlBuildService> coreService = project.getGradle()
                .getSharedServices()
                .registerIfAbsent(CORE_SERVICE_NAME, CoreGretlBuildService.class, spec -> {
                    // Core tasks are allowed to run concurrently by default.
                });

        Provider<InterlisBuildService> interlisService = project.getGradle()
                .getSharedServices()
                .registerIfAbsent(INTERLIS_SERVICE_NAME, InterlisBuildService.class, spec ->
                        spec.getMaxParallelUsages().set(1));

        project.getTasks().withType(AbstractCoreGretlTask.class).configureEach(task -> {
            task.getCoreService().set(coreService);
            task.usesService(coreService);
        });

        project.getTasks().withType(AbstractInterlisTask.class).configureEach(task -> {
            task.getInterlisService().set(interlisService);
            task.usesService(interlisService);
        });
    }
}
