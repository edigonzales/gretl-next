package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "Ili2pgValidate", description = "Validates data in PostgreSQL/PostGIS using ili2pg.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Validiert Daten in PostgreSQL/PostGIS mit ili2pg.") })
public abstract class Ili2pgValidate extends AbstractIli2DbTask {

    @TaskAction
    public void validateData() {
        new Ili2DbExecutionSupport().executeValidate(this, Ili2DbFlavor.POSTGIS);
    }
}
