package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.LocaleText;
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
    private final RegularFileProperty logFile;
    private final RegularFileProperty xtfLogFile;
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
    public RegularFileProperty getLogFile() {
        return logFile;
    }

    @OutputFile
    @Optional
    public RegularFileProperty getXtfLogFile() {
        return xtfLogFile;
    }

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
        this.logFile = getProject().getObjects().fileProperty();
        this.xtfLogFile = getProject().getObjects().fileProperty();
        getModelNames().convention(Collections.emptyList());
        getModelDirectories().convention(Collections.emptyList());
        getForceTypeValidation().convention(false);
        getDisableAreaValidation().convention(false);
        getMultiplicityOff().convention(false);
        getAllObjectsAccessible().convention(false);
        getSkipPolygonBuilding().convention(false);
        getFailOnError().convention(true);
    }

    @GretlDslMethod(required = true, description = "Specifies data files to validate.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die zu validierenden Datendateien an.") })
    public void dataFiles(Object... paths) {
        getDataFiles().from(paths);
    }

    public void setDataFiles(Object paths) {
        getDataFiles().setFrom(paths);
    }

    @GretlDslMethod(description = "Specifies the INTERLIS model names.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die INTERLIS-Modellnamen an.") })
    public void modelNames(String... names) {
        getModelNames().addAll(AbstractIli2DbTask.requireNonBlank("modelNames", names));
    }

    @GretlDslMethod(description = "Alias for configuring the validator models.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Alias für die Konfiguration der Validator-Modelle.") })
    public void models(String value) {
        getModelNames().set(java.util.List.of(value));
    }

    public void setModels(String value) {
        models(value);
    }

    @GretlDslMethod(description = "Specifies model directory or repository entries.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die Modellverzeichnis- oder Repository-Einträge an.") })
    public void modelDirectories(String... entries) {
        getModelDirectories().addAll(AbstractIli2DbTask.requireNonBlank("modelDirectories", entries));
    }

    @GretlDslMethod(description = "Alias for configuring the validator modeldir option.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Alias für die Konfiguration der Validator-Option modeldir.") })
    public void modeldir(String value) {
        getModelDirectories().set(java.util.List.of(value));
    }

    public void setModeldir(String value) {
        modeldir(value);
    }

    @GretlDslMethod(description = "Uses a local validation configuration file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Verwendet eine lokale Validierungskonfigurationsdatei.") })
    public void configFile(Object path) {
        setRegularFile(getConfigFile(), path);
    }

    @GretlDslMethod(description = "Uses a validation configuration from an ilidata repository id.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Verwendet eine Validierungskonfiguration aus einer ilidata-Repository-ID.") })
    public void configRepositoryId(String id) {
        AbstractIli2DbTask.requireNonBlank("configRepositoryId", id);
        getConfigRepositoryId().set(id);
    }

    @GretlDslMethod(description = "Uses a local meta configuration file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Verwendet eine lokale Metakonfigurationsdatei.") })
    public void metaConfigFile(Object path) {
        setRegularFile(getMetaConfigFile(), path);
    }

    @GretlDslMethod(description = "Uses a meta configuration from an ilidata repository id.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Verwendet eine Metakonfiguration aus einer ilidata-Repository-ID.") })
    public void metaConfigRepositoryId(String id) {
        AbstractIli2DbTask.requireNonBlank("metaConfigRepositoryId", id);
        getMetaConfigRepositoryId().set(id);
    }

    @GretlDslMethod(description = "Writes validator log messages to a text file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Schreibt Validator-Protokollmeldungen in eine Textdatei.") })
    public void logFile(Object path) {
        setRegularFile(getLogFile(), path);
    }

    @GretlDslMethod(description = "Writes IliVErrors log messages to a file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Schreibt IliVErrors-Protokollmeldungen in eine Datei.") })
    public void xtfLogFile(Object path) {
        setRegularFile(getXtfLogFile(), path);
    }

    @Internal
    public boolean getValidationOk() {
        return validationOk;
    }

    protected void setValidationOk(boolean validationOk) {
        this.validationOk = validationOk;
    }
}
