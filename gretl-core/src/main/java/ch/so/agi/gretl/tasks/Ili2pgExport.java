package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgExport", description = "Exports PostgreSQL/PostGIS data to INTERLIS transfer files.")
public abstract class Ili2pgExport extends AbstractIli2DbExportTask {

    @OutputFiles
    public ConfigurableFileCollection getTransferOutputs() {
        return getTransferFilesCollection();
    }

    @TaskAction
    public void exportData() {
        new Ili2DbExecutionSupport().executeExport(this, Ili2DbFlavor.POSTGIS);
    }
}
