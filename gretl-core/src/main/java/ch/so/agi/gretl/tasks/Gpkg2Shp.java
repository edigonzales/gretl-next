package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ioxwkf.Gpkg2ShpEngine;
import ch.so.agi.gretl.internal.ioxwkf.Gpkg2ShpEngine.Gpkg2ShpRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Gpkg2Shp", description = "Converts ili2gpkg GeoPackage tables to Shapefiles.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Konvertiert ili2gpkg-GeoPackage-Tabellen in Shapefiles.") })
public abstract class Gpkg2Shp extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Gpkg2Shp.class);
    private final RegularFileProperty dataFile = getProject().getObjects().fileProperty();
    private final DirectoryProperty outputDir = getProject().getObjects().directoryProperty();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public RegularFileProperty getDataFile() {
        return dataFile;
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    @GretlDslMethod(required = true, description = "Specifies the GeoPackage file to convert.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die zu konvertierende GeoPackage-Datei fest.") })
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    @GretlDslMethod(required = true, description = "Specifies the Shapefile output directory.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt das Shapefile-Ausgabeverzeichnis fest.") })
    public void outputDir(Object path) {
        setDirectory(getOutputDir(), path);
    }

    @TaskAction
    public void run() {
        try {
            new Gpkg2ShpEngine().convert(new Gpkg2ShpRequest(
                    getDataFile().get().getAsFile().toPath(),
                    getOutputDir().get().getAsFile().toPath()));
        } catch (Exception e) {
            log.error("failed to run Gpkg2Shp", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
