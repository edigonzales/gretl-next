package ch.so.agi.gretl.tasks;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "Ili2pgDelete", description = "Deletes one or more datasets from PostgreSQL/PostGIS.")
public abstract class Ili2pgDelete extends Ili2pgTask {

    @TaskAction
    public void deleteData() {
        List<String> datasets = datasets();
        if (datasets.isEmpty()) {
            throw new GradleException("dataset is not configured");
        }
        Config config = config(Ili2dbFlavor.POSTGIS, Ili2dbOperation.DELETE);
        config.setBasketHandling(Config.BASKET_HANDLING_READWRITE);
        execute(new Ili2dbRequest(Ili2dbFlavor.POSTGIS, Ili2dbOperation.DELETE,
                databaseSpec(), null, config, List.of(), datasets, logFilePath(), failOnException()));
    }
}
