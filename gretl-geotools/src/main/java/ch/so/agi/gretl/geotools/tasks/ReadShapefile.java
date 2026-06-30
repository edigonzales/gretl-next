package ch.so.agi.gretl.geotools.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.geotools.internal.operations.ReadShapefileRequest;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "ReadShapefile", description = "Reads a shapefile through the GeoTools worker runtime and logs basic diagnostics.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Liest ein Shapefile über die GeoTools-Worker-Laufzeitumgebung und protokolliert grundlegende Diagnosedaten.") })
public abstract class ReadShapefile extends GeoToolsTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getShapefile();

    @Input
    @Optional
    public abstract Property<String> getCrsCode();

    @GretlDslMethod(required = true, description = "Configures the input shapefile.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert das Eingabe-Shapefile.") })
    public void shapefile(Object path) {
        getShapefile().set(getProject().file(path));
    }

    @GretlDslMethod(description = "Configures the CRS code used for reading the shapefile.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den CRS-Code zum Lesen des Shapefiles.") })
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
