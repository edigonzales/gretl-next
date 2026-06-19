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

@GretlTaskDoc(name = "Ili2duckdbImport", description = "Imports INTERLIS transfer files into DuckDB.")
public abstract class Ili2duckdbImport extends Ili2dbFileDataTask {

    @TaskAction
    public void importData() {
        Ili2dbDatasetResolver.ResolvedDataFiles files = resolvedDataFiles();
        if (files.files().isEmpty()) {
            return;
        }
        List<Ili2dbTransfer> transfers = Ili2dbDatasetResolver.pairFilesAndDatasets(files.files(), datasets());
        Config config = config(Ili2dbFlavor.DUCKDB, Ili2dbOperation.IMPORT);
        execute(new Ili2dbRequest(Ili2dbFlavor.DUCKDB, Ili2dbOperation.IMPORT,
                null, dbFilePath(), config, transfers, List.of(), logFilePath(), failOnException()));
    }
}
