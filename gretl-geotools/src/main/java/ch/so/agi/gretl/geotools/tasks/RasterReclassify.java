package ch.so.agi.gretl.geotools.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
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

@GretlTaskDoc(name = "RasterReclassify", description = "Reclassifies raster values into a new raster.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Reclassifiziert Rasterwerte in ein neues Raster.") })
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

    @GretlDslMethod(required = true, description = "Configures the input raster file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Eingabe-Rasterdatei.") })
    public void inputRaster(Object path) {
        getInputRaster().set(getProject().file(path));
    }

    @GretlDslMethod(required = true, description = "Configures the output raster file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Ausgabe-Rasterdatei.") })
    public void outputRaster(Object path) {
        getOutputRaster().set(getProject().file(path));
    }

    @GretlDslMethod(defaultValue = "0, 55, 60, 65, 70, 500", description = "Specifies strictly increasing class break values.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die streng monoton steigenden Klassenbruchwerte an.") })
    public void breaks(Number... values) {
        getBreaks().set(toDoubleList(values));
    }

    @GretlDslMethod(defaultValue = "-100", description = "Specifies the no-data value for the output raster.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt den No-Data-Wert für das Ausgabe-Raster fest.") })
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
