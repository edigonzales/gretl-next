package ch.so.agi.gretl.tasks;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbDatasetResolver;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import ch.so.agi.gretl.internal.ili2db.Ili2dbTransfer;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "Ili2pgImport", description = "Imports INTERLIS transfer files into PostgreSQL/PostGIS.")
public abstract class Ili2pgImport extends Ili2pgDataFileTask {

    @TaskAction
    public void importData() {
        runDataOperation(Ili2dbOperation.IMPORT);
    }

    protected void runDataOperation(Ili2dbOperation operation) {
        Ili2dbDatasetResolver.ResolvedDataFiles files = resolvedDataFiles();
        if (files.files().isEmpty()) {
            return;
        }
        List<Ili2dbTransfer> transfers = Ili2dbDatasetResolver.pairFilesAndDatasets(files.files(), datasets());
        Config config = config(Ili2dbFlavor.POSTGIS, operation);
        execute(new Ili2dbRequest(Ili2dbFlavor.POSTGIS, operation, databaseSpec(), null,
                config, transfers, List.of(), logFilePath(), failOnException()));
    }
}
