package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2duckdbExport", description = "Exports INTERLIS transfer files from a DuckDB database with ili2duckdb.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Exportiert INTERLIS-Transferdateien aus einer DuckDB-Datenbank mit ili2duckdb.") })
public abstract class Ili2duckdbExport extends AbstractIli2DbExportTask {

    @InputFile
    public RegularFileProperty getInputDatabase() {
        return getDatabaseFile();
    }

    @OutputFiles
    public ConfigurableFileCollection getTransferOutputs() {
        return getTransferFilesCollection();
    }

    @TaskAction
    public void exportData() {
        new Ili2DbExecutionSupport().executeExport(this, Ili2DbFlavor.DUCKDB);
    }
}
