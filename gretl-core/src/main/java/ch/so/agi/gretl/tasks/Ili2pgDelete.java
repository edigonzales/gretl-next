package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgDelete", description = "Deletes one or more datasets from PostgreSQL/PostGIS.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Löscht einen oder mehrere Datasets aus PostgreSQL/PostGIS.") })
public abstract class Ili2pgDelete extends AbstractIli2DbTask {

    @TaskAction
    public void deleteData() {
        new Ili2DbExecutionSupport().executeDelete(this, Ili2DbFlavor.POSTGIS);
    }
}
