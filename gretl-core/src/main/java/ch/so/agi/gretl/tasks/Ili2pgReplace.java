package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.Ili2DbOperation;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgReplace", description = "Replaces datasets in PostgreSQL/PostGIS with INTERLIS transfer files.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Ersetzt Datasets in PostgreSQL/PostGIS mit INTERLIS-Transferdateien.") })
public abstract class Ili2pgReplace extends Ili2pgImport {

    @Override
    @TaskAction
    public void importData() {
        runDataOperation(Ili2DbOperation.REPLACE);
    }
}
