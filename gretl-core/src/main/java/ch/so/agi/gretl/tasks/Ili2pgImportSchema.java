package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgImportSchema", description = "Imports an INTERLIS schema into a PostgreSQL/PostGIS database.")
public abstract class Ili2pgImportSchema extends AbstractIli2DbSchemaImportTask {

    @TaskAction
    public void importSchema() {
        new Ili2DbExecutionSupport().executeSchemaImport(this, Ili2DbFlavor.POSTGIS);
    }
}
