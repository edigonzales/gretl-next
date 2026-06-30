package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine.GpkgImportRequest;
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

@GretlTaskDoc(name = "GpkgImport", description = "Imports one GeoPackage table into a database table.")
public abstract class GpkgImport extends AbstractDatabaseTask {
    private final GretlLogger log = LogEnvironment.getLogger(GpkgImport.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDataFile();

    @Input
    public abstract Property<String> getSrcTableName();

    @Input
    public abstract Property<String> getDstTableName();

    @Input
    @Optional
    public abstract Property<String> getSchemaName();

    @Input
    @Optional
    public abstract Property<Integer> getBatchSize();

    @Input
    @Optional
    public abstract Property<Integer> getFetchSize();

    @GretlDslMethod(required = true, description = "Sets the GeoPackage file to import.")
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    public void srcTableName(String value) { getSrcTableName().set(value); }
    public void dstTableName(String value) { getDstTableName().set(value); }
    public void schemaName(String value) { getSchemaName().set(value); }
    public void batchSize(int value) { getBatchSize().set(value); }
    public void fetchSize(int value) { getFetchSize().set(value); }

    @TaskAction
    public void importData() {
        try {
            new IoxWkfDatabaseEngine().importGpkg(new GpkgImportRequest(
                    databaseSpec(),
                    getDataFile().get().getAsFile().toPath(),
                    CsvImport.required(getSrcTableName(), "srcTableName"),
                    CsvImport.required(getDstTableName(), "dstTableName"),
                    getSchemaName().getOrNull(),
                    getBatchSize().getOrNull(),
                    getFetchSize().getOrNull()));
        } catch (Exception e) {
            log.error("Exception in GpkgImport task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
