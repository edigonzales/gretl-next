package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ioxwkf.Gpkg2DxfEngine;
import ch.so.agi.gretl.internal.ioxwkf.Gpkg2DxfEngine.Gpkg2DxfRequest;
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

@GretlTaskDoc(name = "Gpkg2Dxf", description = "Converts ili2gpkg GeoPackage tables to DXF files.")
public abstract class Gpkg2Dxf extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Gpkg2Dxf.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDataFile();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @GretlDslMethod(required = true, description = "Sets the GeoPackage file to convert.")
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    @GretlDslMethod(required = true, description = "Sets the DXF output directory.")
    public void outputDir(Object path) {
        setDirectory(getOutputDir(), path);
    }

    @TaskAction
    public void run() {
        try {
            new Gpkg2DxfEngine().convert(new Gpkg2DxfRequest(
                    getDataFile().get().getAsFile().toPath(),
                    getOutputDir().get().getAsFile().toPath()));
        } catch (Exception e) {
            log.error("failed to run Gpkg2Dxf", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
