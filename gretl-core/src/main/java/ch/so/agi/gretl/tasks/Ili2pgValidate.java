package ch.so.agi.gretl.tasks;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "Ili2pgValidate", description = "Validates data in PostgreSQL/PostGIS using ili2pg.")
public abstract class Ili2pgValidate extends Ili2pgTask {

    @TaskAction
    public void validateData() {
        Config config = config(Ili2dbFlavor.POSTGIS, Ili2dbOperation.VALIDATE);
        execute(new Ili2dbRequest(Ili2dbFlavor.POSTGIS, Ili2dbOperation.VALIDATE,
                databaseSpec(), null, config, List.of(), datasets(), logFilePath(), failOnException()));
    }
}
