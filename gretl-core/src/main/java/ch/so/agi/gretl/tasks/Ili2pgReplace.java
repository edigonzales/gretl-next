package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgReplace", description = "Replaces datasets in PostgreSQL/PostGIS with INTERLIS transfer files.")
public abstract class Ili2pgReplace extends Ili2pgImport {

    @Override
    @TaskAction
    public void importData() {
        runDataOperation(Ili2dbOperation.REPLACE);
    }
}
