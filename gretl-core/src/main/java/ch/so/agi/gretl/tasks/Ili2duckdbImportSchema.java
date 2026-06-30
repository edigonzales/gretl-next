package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2duckdbImportSchema", description = "Imports an INTERLIS schema into a DuckDB database with ili2duckdb.")
public abstract class Ili2duckdbImportSchema extends AbstractIli2DbSchemaImportTask {

    @OutputFile
    public RegularFileProperty getOutputDatabase() {
        return getDatabaseFile();
    }

    @TaskAction
    public void importSchema() {
        new Ili2DbExecutionSupport().executeSchemaImport(this, Ili2DbFlavor.DUCKDB);
    }
}
