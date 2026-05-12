package ch.so.agi.gretl.geotools.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class Vectorize extends GeoToolsTask {

    public Vectorize() {
        getBand().convention(0);
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputRaster();

    @OutputFile
    public abstract RegularFileProperty getOutputGeopackage();

    @Input
    @Optional
    public abstract Property<Integer> getBand();

    @Input
    public abstract ListProperty<Double> getCellValues();

    @TaskAction
    public void execute() {
        List<Double> cellValues = getCellValues().get();
        if (cellValues == null || cellValues.isEmpty()) {
            throw new IllegalStateException("cellValues must not be empty");
        }
        if (cellValues.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("cellValues must not contain null values");
        }

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("taskName", getName());
        parameters.put("inputRaster", getInputRaster().get().getAsFile().getAbsolutePath());
        parameters.put("outputGeopackage", getOutputGeopackage().get().getAsFile().getAbsolutePath());
        parameters.put("band", String.valueOf(getBand().get()));
        submitGeoToolsWork("vectorize", parameters, cellValues);
    }
}
