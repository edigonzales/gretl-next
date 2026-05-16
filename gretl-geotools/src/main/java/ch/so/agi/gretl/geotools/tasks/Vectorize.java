package ch.so.agi.gretl.geotools.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.geotools.internal.operations.VectorizeRequest;
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

import java.util.List;
import java.util.Objects;

@GretlTaskDoc(name = "Vectorize", description = "Vectorizes selected raster cell values into a GeoPackage.")
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

    @GretlDslMethod(required = true, description = "Configures the input raster file.")
    public void inputRaster(Object path) {
        getInputRaster().set(getProject().file(path));
    }

    @GretlDslMethod(required = true, description = "Configures the output GeoPackage file.")
    public void outputGeopackage(Object path) {
        getOutputGeopackage().set(getProject().file(path));
    }

    @GretlDslMethod(defaultValue = "0", description = "Sets the raster band index to read.")
    public void band(int band) {
        getBand().set(band);
    }

    @GretlDslMethod(required = true, description = "Sets the raster cell values to vectorize.")
    public void cellValues(Number... values) {
        getCellValues().set(toDoubleList(values));
    }

    @TaskAction
    public void execute() {
        List<Double> cellValues = getCellValues().get();
        if (cellValues == null || cellValues.isEmpty()) {
            throw new IllegalStateException("cellValues must not be empty");
        }
        if (cellValues.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("cellValues must not contain null values");
        }

        submitGeoToolsWork(new VectorizeRequest(
                getName(),
                getInputRaster().get().getAsFile().toPath(),
                getOutputGeopackage().get().getAsFile().toPath(),
                getBand().get(),
                cellValues));
    }

    private static List<Double> toDoubleList(Number... values) {
        if (values == null) {
            throw new IllegalArgumentException("cellValues must not be null");
        }
        return java.util.Arrays.stream(values)
                .map(value -> {
                    if (value == null) {
                        throw new IllegalArgumentException("cellValues must not contain null values");
                    }
                    return value.doubleValue();
                })
                .toList();
    }
}
