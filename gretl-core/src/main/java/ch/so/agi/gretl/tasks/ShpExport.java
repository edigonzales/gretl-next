package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine.ShpExportRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "ShpExport", description = "Exports one database table into a Shapefile.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Exportiert eine Datenbank-Tabelle in ein Shapefile.") })
public abstract class ShpExport extends AbstractDatabaseTask {
    private final GretlLogger log = LogEnvironment.getLogger(ShpExport.class);
    private final RegularFileProperty dataFile = getProject().getObjects().fileProperty();
    private final Property<String> tableName = getProject().getObjects().property(String.class);
    private final Property<String> schemaName = getProject().getObjects().property(String.class);
    private final Property<String> encoding = getProject().getObjects().property(String.class);

    @OutputFile
    public RegularFileProperty getDataFile() {
        return dataFile;
    }

    @Input
    public Property<String> getTableName() {
        return tableName;
    }

    @Input
    @Optional
    public Property<String> getSchemaName() {
        return schemaName;
    }

    @Input
    @Optional
    public Property<String> getEncoding() {
        return encoding;
    }

    @GretlDslMethod(required = true, description = "Specifies the Shapefile output path.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt den Shapefile-Ausgabepfad fest.") })
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    @GretlDslMethod(required = true, description = "Specifies the source database table.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die Quell-Datenbanktabelle fest.") })
    public void tableName(String value) { getTableName().set(value); }

    public void schemaName(String value) { getSchemaName().set(value); }
    public void encoding(String value) { getEncoding().set(value); }

    @TaskAction
    public void exportData() {
        try {
            new IoxWkfDatabaseEngine().exportShp(new ShpExportRequest(
                    databaseSpec(),
                    getDataFile().get().getAsFile().toPath(),
                    CsvImport.required(getTableName(), "tableName"),
                    getSchemaName().getOrNull(),
                    getEncoding().getOrNull()));
        } catch (Exception e) {
            log.error("Exception in ShpExport task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
