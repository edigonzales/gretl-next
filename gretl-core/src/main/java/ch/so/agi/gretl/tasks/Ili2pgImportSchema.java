package ch.so.agi.gretl.tasks;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbConfigBuilder;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "Ili2pgImportSchema", description = "Imports an INTERLIS schema into a PostgreSQL/PostGIS database.")
public abstract class Ili2pgImportSchema extends Ili2pgSchemaTask {

    @TaskAction
    public void importSchema() {
        Config config = config(Ili2dbFlavor.POSTGIS, Ili2dbOperation.IMPORT_SCHEMA);
        Ili2dbConfigBuilder.applySchemaImport(config, schemaOptions());
        execute(new Ili2dbRequest(Ili2dbFlavor.POSTGIS, Ili2dbOperation.IMPORT_SCHEMA,
                databaseSpec(), null, config, List.of(), List.of(), logFilePath(), failOnException()));
    }
}
