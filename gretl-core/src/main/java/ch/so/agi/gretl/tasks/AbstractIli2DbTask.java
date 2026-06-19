package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractIli2DbTask extends AbstractInterlisTask {

    @Internal
    public abstract RegularFileProperty getDatabaseFile();

    @Input
    @Optional
    public abstract Property<String> getSchema();

    @Input
    @Optional
    public abstract ListProperty<String> getModelNames();

    @Input
    @Optional
    public abstract ListProperty<String> getModelDirectories();

    @Input
    @Optional
    public abstract ListProperty<String> getBaskets();

    @Input
    @Optional
    public abstract ListProperty<String> getTopics();

    @Input
    public abstract Property<Boolean> getImportTid();

    @Input
    public abstract Property<Boolean> getExportTid();

    @Input
    public abstract Property<Boolean> getImportBid();

    @InputFile
    @Optional
    public abstract RegularFileProperty getPreScript();

    @InputFile
    @Optional
    public abstract RegularFileProperty getPostScript();

    @Input
    public abstract Property<Boolean> getDeleteData();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getLogFile();

    @Input
    public abstract Property<Boolean> getTrace();

    @InputFile
    @Optional
    public abstract RegularFileProperty getValidConfigFile();

    @Input
    public abstract Property<Boolean> getDisableValidation();

    @Input
    public abstract Property<Boolean> getDisableAreaValidation();

    @Input
    public abstract Property<Boolean> getForceTypeValidation();

    @Input
    public abstract Property<Boolean> getSkipPolygonBuilding();

    @Input
    public abstract Property<Boolean> getSkipGeometryErrors();

    @Input
    public abstract Property<Boolean> getIligml20();

    @Input
    public abstract Property<Boolean> getDisableRounding();

    @Input
    public abstract Property<Boolean> getFailOnException();

    @Input
    @Optional
    public abstract Property<String> getProxy();

    @Input
    @Optional
    public abstract Property<Integer> getProxyPort();

    @Inject
    public AbstractIli2DbTask() {
        getModelNames().convention(Collections.emptyList());
        getModelDirectories().convention(Collections.emptyList());
        getBaskets().convention(Collections.emptyList());
        getTopics().convention(Collections.emptyList());
        getImportTid().convention(false);
        getExportTid().convention(false);
        getImportBid().convention(false);
        getDeleteData().convention(false);
        getTrace().convention(false);
        getDisableValidation().convention(false);
        getDisableAreaValidation().convention(false);
        getForceTypeValidation().convention(false);
        getSkipPolygonBuilding().convention(false);
        getSkipGeometryErrors().convention(false);
        getIligml20().convention(false);
        getDisableRounding().convention(false);
        getFailOnException().convention(true);
    }

    @GretlDslMethod(required = true, description = "Configures the DuckDB database file.")
    public void databaseFile(Object path) {
        getDatabaseFile().fileValue(getProject().file(path));
    }

    @GretlDslMethod(description = "Sets the ili2db schema name.")
    public void schema(String name) {
        requireNonBlank("schema", name);
        getSchema().set(name);
    }

    @GretlDslMethod(description = "Adds INTERLIS model names.")
    public void modelNames(String... names) {
        getModelNames().addAll(requireNonBlank("modelNames", names));
    }

    @GretlDslMethod(description = "Adds model directory or repository entries.")
    public void modelDirectories(String... entries) {
        getModelDirectories().addAll(requireNonBlank("modelDirectories", entries));
    }

    @GretlDslMethod(description = "Writes ili2db logs to a text file.")
    public void logFile(Object path) {
        getLogFile().fileValue(getProject().file(path));
    }

    protected static List<String> requireNonBlank(String name, String... values) {
        if (values == null || values.length == 0) {
            throw new GradleException(name + " must not be empty");
        }
        return Arrays.stream(values)
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new GradleException(name + " must not contain null or blank values");
                    }
                    return value;
                })
                .toList();
    }

    protected static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new GradleException(name + " must not be null or blank");
        }
    }
}
