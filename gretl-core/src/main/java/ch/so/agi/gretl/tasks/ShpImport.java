package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine.ShpImportRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "ShpImport", description = "Imports one Shapefile into a database table.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Importiert ein Shapefile in eine Datenbank-Tabelle.") })
public abstract class ShpImport extends AbstractDatabaseTask {
    private final GretlLogger log = LogEnvironment.getLogger(ShpImport.class);
    private final RegularFileProperty dataFile = getProject().getObjects().fileProperty();
    private final Property<String> tableName = getProject().getObjects().property(String.class);
    private final Property<String> schemaName = getProject().getObjects().property(String.class);
    private final Property<String> encoding = getProject().getObjects().property(String.class);
    private final Property<Integer> batchSize = getProject().getObjects().property(Integer.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
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

    @Input
    @Optional
    public Property<Integer> getBatchSize() {
        return batchSize;
    }

    @GretlDslMethod(required = true, description = "Specifies the Shapefile to import.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt das zu importierende Shapefile fest.") })
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    @GretlDslMethod(required = true, description = "Specifies the target database table.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die Ziel-Datenbanktabelle fest.") })
    public void tableName(String value) { getTableName().set(value); }

    public void schemaName(String value) { getSchemaName().set(value); }
    public void encoding(String value) { getEncoding().set(value); }
    public void batchSize(int value) { getBatchSize().set(value); }

    @TaskAction
    public void importData() {
        try {
            new IoxWkfDatabaseEngine().importShp(new ShpImportRequest(
                    databaseSpec(),
                    getDataFile().get().getAsFile().toPath(),
                    CsvImport.required(getTableName(), "tableName"),
                    getSchemaName().getOrNull(),
                    getEncoding().getOrNull(),
                    getBatchSize().getOrNull()));
        } catch (Exception e) {
            log.error("Exception in ShpImport task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
