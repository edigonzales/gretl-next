package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.Ili2DbOperation;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgUpdate", description = "Updates PostgreSQL/PostGIS data from INTERLIS transfer files.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Aktualisiert PostgreSQL/PostGIS-Daten aus INTERLIS-Transferdateien.") })
public abstract class Ili2pgUpdate extends Ili2pgImport {

    @Override
    @TaskAction
    public void importData() {
        runDataOperation(Ili2DbOperation.UPDATE);
    }
}
