package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine;
import ch.so.agi.gretl.internal.ioxwkf.IoxWkfDatabaseEngine.GpkgExportRequest;
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
import java.util.Collections;

@GretlTaskDoc(name = "GpkgExport", description = "Exports one or more database tables into a GeoPackage.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Exportiert eine oder mehrere Datenbank-Tabellen in ein GeoPackage.") })
public abstract class GpkgExport extends AbstractDatabaseTask {
    private final GretlLogger log = LogEnvironment.getLogger(GpkgExport.class);

    @OutputFile
    public abstract RegularFileProperty getDataFile();

    @Input
    public abstract ListProperty<String> getSrcTableName();

    @Input
    public abstract ListProperty<String> getDstTableName();

    @Input
    @Optional
    public abstract Property<String> getSchemaName();

    @Input
    @Optional
    public abstract Property<Integer> getBatchSize();

    @Input
    @Optional
    public abstract Property<Integer> getFetchSize();

    @Inject
    public GpkgExport() {
        getSrcTableName().convention(Collections.emptyList());
        getDstTableName().convention(Collections.emptyList());
    }

    @GretlDslMethod(required = true, description = "Specifies the GeoPackage output file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die GeoPackage-Ausgabedatei fest.") })
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    public void srcTableName(String... values) { getSrcTableName().set(Arrays.asList(values)); }
    public void dstTableName(String... values) { getDstTableName().set(Arrays.asList(values)); }
    public void schemaName(String value) { getSchemaName().set(value); }
    public void batchSize(int value) { getBatchSize().set(value); }
    public void fetchSize(int value) { getFetchSize().set(value); }

    @TaskAction
    public void exportData() {
        try {
            new IoxWkfDatabaseEngine().exportGpkg(new GpkgExportRequest(
                    databaseSpec(),
                    getDataFile().get().getAsFile().toPath(),
                    getSrcTableName().get(),
                    getDstTableName().get(),
                    getSchemaName().getOrNull(),
                    getBatchSize().getOrNull(),
                    getFetchSize().getOrNull()));
        } catch (Exception e) {
            log.error("Exception in GpkgExport task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
