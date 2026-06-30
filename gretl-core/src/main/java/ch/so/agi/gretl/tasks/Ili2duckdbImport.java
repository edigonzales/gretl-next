package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import ch.so.agi.gretl.internal.interlis.Ili2DbOperation;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2duckdbImport", description = "Imports INTERLIS transfer files or ilidata repository ids into a DuckDB database with ili2duckdb.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Importiert INTERLIS-Transferdateien oder ilidata-Repository-IDs in eine DuckDB-Datenbank mit ili2duckdb.") })
public abstract class Ili2duckdbImport extends AbstractIli2DbTransferTask {

    @OutputFile
    public RegularFileProperty getOutputDatabase() {
        return getDatabaseFile();
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getLocalTransferFiles() {
        return getTransferFilesCollection();
    }

    @TaskAction
    public void importData() {
        new Ili2DbExecutionSupport().executeImport(this, Ili2DbFlavor.DUCKDB, Ili2DbOperation.IMPORT);
    }
}
