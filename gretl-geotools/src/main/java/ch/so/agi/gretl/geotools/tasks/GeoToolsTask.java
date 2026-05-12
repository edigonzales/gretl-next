package ch.so.agi.gretl.geotools.tasks;

import ch.so.agi.gretl.geotools.services.GeoToolsBuildService;
import ch.so.agi.gretl.geotools.worker.GeoToolsWorkerAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class GeoToolsTask extends DefaultTask {

    private final ConfigurableFileCollection workerClasspath;

    protected GeoToolsTask() {
        this.workerClasspath = getProject().files();
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @Classpath
    public ConfigurableFileCollection getWorkerClasspath() {
        return workerClasspath;
    }

    @Internal
    public abstract Property<GeoToolsBuildService> getGeoToolsService();

    protected void submitGeoToolsWork(String operation, Map<String, String> parameters, List<Double> values) {
        WorkQueue queue = getWorkerExecutor().classLoaderIsolation(spec -> {
            spec.getClasspath().from(getWorkerClasspath());
        });

        queue.submit(GeoToolsWorkerAction.class, workerParameters -> {
            workerParameters.getOperation().set(operation);
            workerParameters.getParameters().set(parameters == null ? Collections.emptyMap() : parameters);
            workerParameters.getValues().set(values == null ? Collections.emptyList() : values);
        });
    }
}
