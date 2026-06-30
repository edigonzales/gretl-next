package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine.JsonImportRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "JsonImport", description = "Imports a JSON document into a database text column.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Importiert ein JSON-Dokument in eine Datenbank-Textspalte.") })
public abstract class JsonImport extends AbstractDatabaseTask {
    private final GretlLogger log = LogEnvironment.getLogger(JsonImport.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getJsonFile();

    @Input
    public abstract Property<String> getQualifiedTableName();

    @Input
    public abstract Property<String> getColumnName();

    @Input
    public abstract Property<Boolean> getDeleteAllRows();

    @Inject
    public JsonImport() {
        getDeleteAllRows().convention(false);
    }

    @GretlDslMethod(required = true, description = "Specifies the JSON file to import.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die zu importierende JSON-Datei fest.") })
    public void jsonFile(Object path) {
        setRegularFile(getJsonFile(), path);
    }

    public void qualifiedTableName(String value) { getQualifiedTableName().set(value); }
    public void columnName(String value) { getColumnName().set(value); }
    public void deleteAllRows(boolean value) { getDeleteAllRows().set(value); }

    @TaskAction
    public void importJsonFile() {
        try {
            new IoxWkfDatabaseEngine().importJson(new JsonImportRequest(
                    databaseSpec(),
                    getJsonFile().get().getAsFile().toPath(),
                    CsvImport.required(getQualifiedTableName(), "qualifiedTableName"),
                    CsvImport.required(getColumnName(), "columnName"),
                    getDeleteAllRows().get()));
        } catch (Exception e) {
            log.error("Exception in JsonImport task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
