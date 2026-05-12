package ch.so.agi.gretl.gradle;

import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.services.CoreGretlBuildService;
import ch.so.agi.gretl.tasks.AbstractCoreGretlTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public class GretlPlugin implements Plugin<Project> {

    public static final String CORE_SERVICE_NAME = "gretlCoreService";

    @Override
    public void apply(Project project) {
        LogEnvironment.initGradleIntegrated();

        Provider<CoreGretlBuildService> coreService = project.getGradle()
                .getSharedServices()
                .registerIfAbsent(CORE_SERVICE_NAME, CoreGretlBuildService.class, spec -> {
                    // Core tasks are allowed to run concurrently by default.
                });

        project.getTasks().withType(AbstractCoreGretlTask.class).configureEach(task -> {
            task.getCoreService().set(coreService);
            task.usesService(coreService);
        });
    }
}
