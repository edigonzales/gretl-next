package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
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
import java.util.Collections;

public abstract class AbstractInterlisValidatorTask extends AbstractInterlisTask {

    private final ConfigurableFileCollection dataFiles;
    private boolean validationOk = true;

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    public ConfigurableFileCollection getDataFiles() {
        return dataFiles;
    }

    @Input
    @Optional
    public abstract ListProperty<String> getModelNames();

    @Input
    @Optional
    public abstract ListProperty<String> getModelDirectories();

    @InputFile
    @Optional
    public abstract RegularFileProperty getConfigFile();

    @Input
    @Optional
    public abstract Property<String> getConfigRepositoryId();

    @InputFile
    @Optional
    public abstract RegularFileProperty getMetaConfigFile();

    @Input
    @Optional
    public abstract Property<String> getMetaConfigRepositoryId();

    @Input
    public abstract Property<Boolean> getForceTypeValidation();

    @Input
    public abstract Property<Boolean> getDisableAreaValidation();

    @Input
    public abstract Property<Boolean> getMultiplicityOff();

    @Input
    public abstract Property<Boolean> getAllObjectsAccessible();

    @Input
    public abstract Property<Boolean> getSkipPolygonBuilding();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getLogFile();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getXtfLogFile();

    @Input
    @Optional
    public abstract Property<String> getProxy();

    @Input
    @Optional
    public abstract Property<Integer> getProxyPort();

    @Input
    public abstract Property<Boolean> getFailOnError();

    @Inject
    public AbstractInterlisValidatorTask() {
        this.dataFiles = getProject().files();
        getModelNames().convention(Collections.emptyList());
        getModelDirectories().convention(Collections.emptyList());
        getForceTypeValidation().convention(false);
        getDisableAreaValidation().convention(false);
        getMultiplicityOff().convention(false);
        getAllObjectsAccessible().convention(false);
        getSkipPolygonBuilding().convention(false);
        getFailOnError().convention(true);
    }

    @GretlDslMethod(required = true, description = "Adds data files to validate.")
    public void dataFiles(Object... paths) {
        getDataFiles().from(paths);
    }

    @GretlDslMethod(description = "Adds INTERLIS model names.")
    public void modelNames(String... names) {
        getModelNames().addAll(AbstractIli2DbTask.requireNonBlank("modelNames", names));
    }

    @GretlDslMethod(description = "Adds model directory or repository entries.")
    public void modelDirectories(String... entries) {
        getModelDirectories().addAll(AbstractIli2DbTask.requireNonBlank("modelDirectories", entries));
    }

    @GretlDslMethod(description = "Uses a local validation config file.")
    public void configFile(Object path) {
        getConfigFile().fileValue(getProject().file(path));
    }

    @GretlDslMethod(description = "Uses a validation config from an ilidata repository id.")
    public void configRepositoryId(String id) {
        AbstractIli2DbTask.requireNonBlank("configRepositoryId", id);
        getConfigRepositoryId().set(id);
    }

    @GretlDslMethod(description = "Uses a local meta config file.")
    public void metaConfigFile(Object path) {
        getMetaConfigFile().fileValue(getProject().file(path));
    }

    @GretlDslMethod(description = "Uses a meta config from an ilidata repository id.")
    public void metaConfigRepositoryId(String id) {
        AbstractIli2DbTask.requireNonBlank("metaConfigRepositoryId", id);
        getMetaConfigRepositoryId().set(id);
    }

    @GretlDslMethod(description = "Writes validator text logs to a file.")
    public void logFile(Object path) {
        getLogFile().fileValue(getProject().file(path));
    }

    @GretlDslMethod(description = "Writes validator IliVErrors logs to a file.")
    public void xtfLogFile(Object path) {
        getXtfLogFile().fileValue(getProject().file(path));
    }

    @Internal
    public boolean getValidationOk() {
        return validationOk;
    }

    protected void setValidationOk(boolean validationOk) {
        this.validationOk = validationOk;
    }
}
