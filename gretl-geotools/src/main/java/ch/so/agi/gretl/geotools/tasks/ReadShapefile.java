package ch.so.agi.gretl.geotools.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ReadShapefile extends GeoToolsTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getShapefile();

    @Input
    @Optional
    public abstract Property<String> getCrsCode();

    @TaskAction
    public void run() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("taskName", getName());
        parameters.put("shapefile", getShapefile().get().getAsFile().getAbsolutePath());
        if (getCrsCode().isPresent()) {
            parameters.put("crsCode", getCrsCode().get());
        }
        submitGeoToolsWork("read-shapefile", parameters, null);
    }
}
