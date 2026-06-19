package ch.so.agi.gretl.tasks;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbConfigBuilder;
import ch.so.agi.gretl.internal.ili2db.Ili2dbDatasetResolver;
import ch.so.agi.gretl.internal.ili2db.Ili2dbExportOptions;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import ch.so.agi.gretl.internal.ili2db.Ili2dbTransfer;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "Ili2duckdbExport", description = "Exports DuckDB data to INTERLIS transfer files.")
public abstract class Ili2duckdbExport extends Ili2dbFileOutputTask {

    @Input
    @Optional
    public abstract Property<Boolean> getExport3();

    @Input
    @Optional
    public abstract Property<String> getExportModels();

    public void export3(boolean value) { getExport3().set(value); }
    public void exportModels(String value) { getExportModels().set(value); }

    @TaskAction
    public void exportData() {
        Ili2dbDatasetResolver.ResolvedOutputFiles files = resolvedOutputFiles();
        if (files.files().isEmpty()) {
            return;
        }
        List<Ili2dbTransfer> transfers = Ili2dbDatasetResolver.pairFilesAndDatasets(files.files(), datasets());
        Config config = config(Ili2dbFlavor.DUCKDB, Ili2dbOperation.EXPORT);
        Ili2dbConfigBuilder.applyExport(config, new Ili2dbExportOptions(getExport3().getOrElse(false), getExportModels().getOrNull()));
        execute(new Ili2dbRequest(Ili2dbFlavor.DUCKDB, Ili2dbOperation.EXPORT,
                null, dbFilePath(), config, transfers, List.of(), logFilePath(), failOnException()));
    }
}
