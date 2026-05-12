package ch.so.agi.gretl.geotools;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;

public class GretlGeotoolsExtension {

    private final Property<String> defaultCrsCode;
    private final ConfigurableFileCollection workerClasspath;

    public GretlGeotoolsExtension(Project project) {
        this.defaultCrsCode = project.getObjects().property(String.class);
        this.defaultCrsCode.convention("EPSG:4326");
        this.workerClasspath = project.files();
    }

    public Property<String> getDefaultCrsCode() {
        return defaultCrsCode;
    }

    public ConfigurableFileCollection getWorkerClasspath() {
        return workerClasspath;
    }
}
