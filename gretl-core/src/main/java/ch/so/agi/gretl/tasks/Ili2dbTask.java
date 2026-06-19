package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.internal.ili2db.Ili2dbCommonOptions;
import ch.so.agi.gretl.internal.ili2db.Ili2dbConfigBuilder;
import ch.so.agi.gretl.internal.ili2db.Ili2dbDatasetResolver;
import ch.so.agi.gretl.internal.ili2db.Ili2dbExecutionEngine;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;

abstract class Ili2dbTask extends AbstractCoreGretlTask {
    private final ConfigurableFileCollection datasetFiles;
    private final GretlLogger log;
    private Object dataset;

    @Inject
    public Ili2dbTask() {
        this.datasetFiles = getProject().files();
        this.log = LogEnvironment.getLogger(getClass());
    }

    @Input
    @Optional
    public abstract Property<String> getDbschema();

    @Input
    @Optional
    public abstract Property<String> getProxy();

    @Input
    @Optional
    public abstract Property<Integer> getProxyPort();

    @Input
    @Optional
    public abstract Property<String> getModeldir();

    @Input
    @Optional
    public abstract Property<String> getModels();

    @Input
    @Optional
    public abstract Property<String> getBaskets();

    @Input
    @Optional
    public abstract Property<String> getTopics();

    @Input
    @Optional
    public abstract Property<Boolean> getImportTid();

    @Input
    @Optional
    public abstract Property<Boolean> getExportTid();

    @Input
    @Optional
    public abstract Property<Boolean> getImportBid();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract org.gradle.api.file.RegularFileProperty getPreScript();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract org.gradle.api.file.RegularFileProperty getPostScript();

    @Input
    @Optional
    public abstract Property<Boolean> getDeleteData();

    @OutputFile
    @Optional
    public abstract org.gradle.api.file.RegularFileProperty getLogFile();

    @Input
    @Optional
    public abstract Property<Boolean> getTrace();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract org.gradle.api.file.RegularFileProperty getValidConfigFile();

    @Input
    @Optional
    public abstract Property<Boolean> getDisableValidation();

    @Input
    @Optional
    public abstract Property<Boolean> getDisableAreaValidation();

    @Input
    @Optional
    public abstract Property<Boolean> getForceTypeValidation();

    @Input
    @Optional
    public abstract Property<Boolean> getStrokeArcs();

    @Input
    @Optional
    public abstract Property<Boolean> getSkipPolygonBuilding();

    @Input
    @Optional
    public abstract Property<Boolean> getSkipGeometryErrors();

    @Input
    @Optional
    public abstract Property<Boolean> getIligml20();

    @Input
    @Optional
    public abstract Property<Boolean> getDisableRounding();

    @Input
    @Optional
    public abstract Property<Boolean> getFailOnException();

    @Input
    @Optional
    public abstract ListProperty<Integer> getDatasetSubstring();

    @Input
    @Optional
    public List<String> getDatasetNames() {
        return Ili2dbDatasetResolver.resolveDatasets(dataset, getDatasetSubstring().getOrElse(List.of()));
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getDatasetFiles() {
        return datasetFiles;
    }

    @Internal
    protected Object getDatasetRaw() {
        return dataset;
    }

    public void setDataset(Object dataset) {
        this.dataset = dataset;
        datasetFiles.setFrom();
        if (dataset instanceof org.gradle.api.file.FileCollection fileCollection) {
            datasetFiles.from(fileCollection);
        }
    }

    @GretlDslMethod(description = "Configures one or more ili2db dataset names.")
    public void dataset(Object dataset) {
        setDataset(dataset);
    }

    @GretlDslMethod(description = "Configures substring indexes used to derive dataset names.")
    public void datasetSubstring(Iterable<Integer> datasetSubstring) {
        getDatasetSubstring().set(datasetSubstring);
    }

    public void dbschema(String value) { getDbschema().set(value); }
    public void proxy(String value) { getProxy().set(value); }
    public void proxyPort(int value) { getProxyPort().set(value); }
    public void modeldir(String value) { getModeldir().set(value); }
    public void models(String value) { getModels().set(value); }
    public void baskets(String value) { getBaskets().set(value); }
    public void topics(String value) { getTopics().set(value); }
    public void importTid(boolean value) { getImportTid().set(value); }
    public void exportTid(boolean value) { getExportTid().set(value); }
    public void importBid(boolean value) { getImportBid().set(value); }
    public void preScript(Object value) { getPreScript().set(getProject().file(value)); }
    public void postScript(Object value) { getPostScript().set(getProject().file(value)); }
    public void deleteData(boolean value) { getDeleteData().set(value); }
    public void logFile(Object value) { getLogFile().set(getProject().file(value)); }
    public void trace(boolean value) { getTrace().set(value); }
    public void validConfigFile(Object value) { getValidConfigFile().set(getProject().file(value)); }
    public void disableValidation(boolean value) { getDisableValidation().set(value); }
    public void disableAreaValidation(boolean value) { getDisableAreaValidation().set(value); }
    public void forceTypeValidation(boolean value) { getForceTypeValidation().set(value); }
    public void strokeArcs(boolean value) { getStrokeArcs().set(value); }
    public void skipPolygonBuilding(boolean value) { getSkipPolygonBuilding().set(value); }
    public void skipGeometryErrors(boolean value) { getSkipGeometryErrors().set(value); }
    public void iligml20(boolean value) { getIligml20().set(value); }
    public void disableRounding(boolean value) { getDisableRounding().set(value); }
    public void failOnException(boolean value) { getFailOnException().set(value); }

    protected Ili2dbCommonOptions commonOptions() {
        return new Ili2dbCommonOptions(
                getDbschema().getOrNull(),
                getProxy().getOrNull(),
                getProxyPort().getOrNull(),
                getModeldir().getOrNull(),
                getModels().getOrNull(),
                getBaskets().getOrNull(),
                getTopics().getOrNull(),
                getImportTid().getOrElse(false),
                getExportTid().getOrElse(false),
                getImportBid().getOrElse(false),
                pathOrNull(getPreScript()),
                pathOrNull(getPostScript()),
                getDeleteData().getOrElse(false),
                pathOrNull(getLogFile()),
                getTrace().getOrElse(false),
                pathOrNull(getValidConfigFile()),
                getDisableValidation().getOrElse(false),
                getDisableAreaValidation().getOrElse(false),
                getForceTypeValidation().getOrElse(false),
                getStrokeArcs().getOrElse(false),
                getSkipPolygonBuilding().getOrElse(false),
                getSkipGeometryErrors().getOrElse(false),
                getIligml20().getOrElse(false),
                getDisableRounding().getOrElse(false)
        );
    }

    protected ch.ehi.ili2db.gui.Config config(Ili2dbFlavor flavor, Ili2dbOperation operation) {
        return Ili2dbConfigBuilder.config(flavor, operation, commonOptions());
    }

    protected List<String> datasets() {
        return getDatasetNames();
    }

    protected boolean failOnException() {
        return getFailOnException().getOrElse(true);
    }

    protected Path logFilePath() {
        return pathOrNull(getLogFile());
    }

    protected void execute(Ili2dbRequest request) {
        try {
            new Ili2dbExecutionEngine().execute(request);
        } catch (Exception e) {
            log.error("Exception in " + getClass().getSimpleName() + " task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    protected static Path pathOrNull(org.gradle.api.file.RegularFileProperty property) {
        return property.isPresent() ? property.get().getAsFile().toPath() : null;
    }
}
