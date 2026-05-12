package ch.so.agi.gretl.geotools.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class RasterReclassify extends GeoToolsTask {

    private static final List<Double> DEFAULT_BREAKS = List.of(0d, 55d, 60d, 65d, 70d, 500d);
    private static final double DEFAULT_NO_DATA = -100d;

    public RasterReclassify() {
        getBreaks().convention(DEFAULT_BREAKS);
        getNoData().convention(DEFAULT_NO_DATA);
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputRaster();

    @OutputFile
    public abstract RegularFileProperty getOutputRaster();

    @Input
    public abstract ListProperty<Double> getBreaks();

    @Input
    public abstract Property<Double> getNoData();

    @TaskAction
    public void execute() {
        List<Double> breaks = getBreaks().get();
        if (breaks == null || breaks.isEmpty()) {
            throw new IllegalStateException("breaks must not be empty");
        }

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("taskName", getName());
        parameters.put("inputRaster", getInputRaster().get().getAsFile().getAbsolutePath());
        parameters.put("outputRaster", getOutputRaster().get().getAsFile().getAbsolutePath());
        parameters.put("noData", String.valueOf(getNoData().get()));
        submitGeoToolsWork("raster-reclassify", parameters, breaks);
    }
}
