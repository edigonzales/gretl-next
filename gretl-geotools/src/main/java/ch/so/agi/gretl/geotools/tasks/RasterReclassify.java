package ch.so.agi.gretl.geotools.tasks;

import ch.so.agi.gretl.geotools.internal.operations.RasterReclassifyRequest;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.util.List;
import java.util.Objects;

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

    public void inputRaster(Object path) {
        getInputRaster().set(getProject().file(path));
    }

    public void outputRaster(Object path) {
        getOutputRaster().set(getProject().file(path));
    }

    public void breaks(Number... values) {
        getBreaks().set(toDoubleList(values));
    }

    public void noData(Number value) {
        getNoData().set(value.doubleValue());
    }

    @TaskAction
    public void execute() {
        List<Double> breaks = getBreaks().get();
        validateBreaks(breaks);
        submitGeoToolsWork(new RasterReclassifyRequest(
                getName(),
                getInputRaster().get().getAsFile().toPath(),
                getOutputRaster().get().getAsFile().toPath(),
                getNoData().get(),
                breaks));
    }

    private static List<Double> toDoubleList(Number... values) {
        if (values == null) {
            throw new IllegalArgumentException("breaks must not be null");
        }
        return java.util.Arrays.stream(values)
                .map(value -> {
                    if (value == null) {
                        throw new IllegalArgumentException("breaks must not contain null values");
                    }
                    return value.doubleValue();
                })
                .toList();
    }

    private static void validateBreaks(List<Double> breaks) {
        if (breaks == null || breaks.size() < 2) {
            throw new IllegalStateException("breaks must contain at least two values");
        }
        if (breaks.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("breaks must not contain null values");
        }
        for (int i = 1; i < breaks.size(); i++) {
            if (!(breaks.get(i) > breaks.get(i - 1))) {
                throw new IllegalStateException("breaks must be strictly increasing");
            }
        }
    }
}
