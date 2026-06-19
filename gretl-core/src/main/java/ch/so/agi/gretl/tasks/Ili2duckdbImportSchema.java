package ch.so.agi.gretl.tasks;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbConfigBuilder;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "Ili2duckdbImportSchema", description = "Imports an INTERLIS schema into a DuckDB database.")
public abstract class Ili2duckdbImportSchema extends Ili2dbFileSchemaTask {

    @TaskAction
    public void importSchema() {
        Config config = config(Ili2dbFlavor.DUCKDB, Ili2dbOperation.IMPORT_SCHEMA);
        Ili2dbConfigBuilder.applySchemaImport(config, schemaOptions());
        execute(new Ili2dbRequest(Ili2dbFlavor.DUCKDB, Ili2dbOperation.IMPORT_SCHEMA,
                null, dbFilePath(), config, List.of(), List.of(), logFilePath(), failOnException()));
    }
}
