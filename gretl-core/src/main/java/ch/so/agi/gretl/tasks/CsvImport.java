package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine.CsvImportRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "CsvImport", description = "Imports a CSV file into a database table.")
public abstract class CsvImport extends AbstractDatabaseTask {
    private final GretlLogger log = LogEnvironment.getLogger(CsvImport.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDataFile();

    @Input
    public abstract Property<String> getTableName();

    @Input
    public abstract Property<Boolean> getFirstLineIsHeader();

    @Input
    @Optional
    public abstract Property<String> getValueDelimiter();

    @Input
    @Optional
    public abstract Property<String> getValueSeparator();

    @Input
    @Optional
    public abstract Property<String> getSchemaName();

    @Input
    @Optional
    public abstract Property<String> getEncoding();

    @Input
    @Optional
    public abstract Property<Integer> getBatchSize();

    @Inject
    public CsvImport() {
        getFirstLineIsHeader().convention(true);
    }

    @GretlDslMethod(required = true, description = "Sets the CSV file to import.")
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    public void tableName(String value) { getTableName().set(value); }
    public void firstLineIsHeader(boolean value) { getFirstLineIsHeader().set(value); }
    public void valueDelimiter(String value) { getValueDelimiter().set(value); }
    public void valueSeparator(String value) { getValueSeparator().set(value); }
    public void schemaName(String value) { getSchemaName().set(value); }
    public void encoding(String value) { getEncoding().set(value); }
    public void batchSize(int value) { getBatchSize().set(value); }

    @TaskAction
    public void importData() {
        try {
            new IoxWkfDatabaseEngine().importCsv(new CsvImportRequest(
                    databaseSpec(),
                    getDataFile().get().getAsFile().toPath(),
                    required(getTableName(), "tableName"),
                    getFirstLineIsHeader().get(),
                    getValueDelimiter().getOrNull(),
                    getValueSeparator().getOrNull(),
                    getSchemaName().getOrNull(),
                    getEncoding().getOrNull(),
                    getBatchSize().getOrNull()));
        } catch (Exception e) {
            log.error("Exception in CsvImport task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    static String required(Property<String> property, String name) {
        if (!property.isPresent() || property.get().isBlank()) {
            throw new GradleException(name + " must not be null or blank");
        }
        return property.get();
    }
}
