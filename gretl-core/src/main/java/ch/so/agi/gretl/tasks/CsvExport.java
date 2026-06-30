package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine.CsvExportRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.Arrays;

@GretlTaskDoc(name = "CsvExport", description = "Exports a database table into a CSV file.")
public abstract class CsvExport extends AbstractDatabaseTask {
    private final GretlLogger log = LogEnvironment.getLogger(CsvExport.class);

    @OutputFile
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
    public abstract ListProperty<String> getAttributes();

    @Input
    @Optional
    public abstract Property<String> getEncoding();

    @Inject
    public CsvExport() {
        getFirstLineIsHeader().convention(true);
        getAttributes().convention(java.util.Collections.emptyList());
    }

    @GretlDslMethod(required = true, description = "Sets the CSV output file.")
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    public void tableName(String value) { getTableName().set(value); }
    public void firstLineIsHeader(boolean value) { getFirstLineIsHeader().set(value); }
    public void valueDelimiter(String value) { getValueDelimiter().set(value); }
    public void valueSeparator(String value) { getValueSeparator().set(value); }
    public void schemaName(String value) { getSchemaName().set(value); }
    public void attributes(String... values) { getAttributes().set(Arrays.asList(values)); }
    public void encoding(String value) { getEncoding().set(value); }

    @TaskAction
    public void exportData() {
        try {
            new IoxWkfDatabaseEngine().exportCsv(new CsvExportRequest(
                    databaseSpec(),
                    getDataFile().get().getAsFile().toPath(),
                    CsvImport.required(getTableName(), "tableName"),
                    getFirstLineIsHeader().get(),
                    getValueDelimiter().getOrNull(),
                    getValueSeparator().getOrNull(),
                    getSchemaName().getOrNull(),
                    getAttributes().get(),
                    getEncoding().getOrNull()));
        } catch (Exception e) {
            log.error("Exception in CsvExport task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
