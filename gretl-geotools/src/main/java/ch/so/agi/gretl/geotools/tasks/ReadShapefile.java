package ch.so.agi.gretl.geotools.tasks;

import ch.so.agi.gretl.geotools.internal.operations.ReadShapefileRequest;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

public abstract class ReadShapefile extends GeoToolsTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getShapefile();

    @Input
    @Optional
    public abstract Property<String> getCrsCode();

    public void shapefile(Object path) {
        getShapefile().set(getProject().file(path));
    }

    public void crsCode(String crsCode) {
        getCrsCode().set(crsCode);
    }

    @TaskAction
    public void run() {
        submitGeoToolsWork(new ReadShapefileRequest(
                getName(),
                getShapefile().get().getAsFile().toPath(),
                getCrsCode().getOrNull()));
    }
}
