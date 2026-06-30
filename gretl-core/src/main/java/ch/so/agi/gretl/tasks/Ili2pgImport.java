package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import ch.so.agi.gretl.internal.interlis.Ili2DbOperation;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgImport", description = "Imports INTERLIS transfer files into PostgreSQL/PostGIS.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Importiert INTERLIS-Transferdateien in PostgreSQL/PostGIS.") })
public abstract class Ili2pgImport extends AbstractIli2DbTransferTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getLocalTransferFiles() {
        return getTransferFilesCollection();
    }

    @TaskAction
    public void importData() {
        runDataOperation(Ili2DbOperation.IMPORT);
    }

    protected void runDataOperation(Ili2DbOperation operation) {
        new Ili2DbExecutionSupport().executeImport(this, Ili2DbFlavor.POSTGIS, operation);
    }
}
