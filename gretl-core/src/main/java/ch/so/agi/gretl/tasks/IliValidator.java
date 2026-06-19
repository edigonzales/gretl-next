package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.IliValidatorEngine;
import ch.so.agi.gretl.internal.ili2db.IliValidatorRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.util.Comparator;
import java.util.List;

@GretlTaskDoc(name = "IliValidator", description = "Validates INTERLIS transfer files with ilivalidator.")
public abstract class IliValidator extends AbstractCoreGretlTask {
    private final ConfigurableFileCollection dataFiles;
    private final ConfigurableFileCollection configFileInput;
    private final ConfigurableFileCollection metaConfigFileInput;
    private final GretlLogger log;
    private Object configFile;
    private Object metaConfigFile;
    private boolean validationOk = true;

    @Inject
    public IliValidator() {
        this.dataFiles = getProject().files();
        this.configFileInput = getProject().files();
        this.metaConfigFileInput = getProject().files();
        this.log = LogEnvironment.getLogger(IliValidator.class);
        getForceTypeValidation().convention(false);
        getDisableAreaValidation().convention(false);
        getMultiplicityOff().convention(false);
        getAllObjectsAccessible().convention(false);
        getSkipPolygonBuilding().convention(false);
        getFailOnError().convention(true);
    }

    /**
     * Transfer files to validate. An empty collection is not an error.
     */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getDataFiles() {
        return dataFiles;
    }

    @Input
    @Optional
    public abstract Property<String> getModels();

    @Input
    @Optional
    public abstract Property<String> getModeldir();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getConfigFileInput() {
        return configFileInput;
    }

    @Input
    @Optional
    public String getConfigFileReference() {
        return isIliDataReference(configFile) ? configFile.toString() : null;
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getMetaConfigFileInput() {
        return metaConfigFileInput;
    }

    @Input
    @Optional
    public String getMetaConfigFileReference() {
        return isIliDataReference(metaConfigFile) ? metaConfigFile.toString() : null;
    }

    @Input
    @Optional
    public abstract Property<Boolean> getForceTypeValidation();

    @Input
    @Optional
    public abstract Property<Boolean> getDisableAreaValidation();

    @Input
    @Optional
    public abstract Property<Boolean> getMultiplicityOff();

    @Input
    @Optional
    public abstract Property<Boolean> getAllObjectsAccessible();

    @Input
    @Optional
    public abstract Property<Boolean> getSkipPolygonBuilding();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getLogFile();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getXtflogFile();

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getPluginFolder();

    @Input
    @Optional
    public abstract Property<String> getProxy();

    @Input
    @Optional
    public abstract Property<Integer> getProxyPort();

    @Input
    @Optional
    public abstract Property<Boolean> getFailOnError();

    @Internal
    public boolean getValidationOk() {
        return validationOk;
    }

    public void setDataFiles(Object dataFiles) {
        this.dataFiles.setFrom(dataFiles);
    }

    public void setConfigFile(Object configFile) {
        this.configFile = configFile;
        configFileInput.setFrom();
        if (configFile != null && !isIliDataReference(configFile)) {
            configFileInput.from(configFile);
        }
    }

    public void setMetaConfigFile(Object metaConfigFile) {
        this.metaConfigFile = metaConfigFile;
        metaConfigFileInput.setFrom();
        if (metaConfigFile != null && !isIliDataReference(metaConfigFile)) {
            metaConfigFileInput.from(metaConfigFile);
        }
    }

    public void setValidationOk(boolean validationOk) {
        this.validationOk = validationOk;
    }

    @GretlDslMethod(required = true, description = "Adds transfer files to validate.")
    public void dataFiles(Object... dataFiles) {
        this.dataFiles.from(dataFiles);
    }

    public void models(String value) { getModels().set(value); }
    public void modeldir(String value) { getModeldir().set(value); }
    public void configFile(Object value) { setConfigFile(value); }
    public void metaConfigFile(Object value) { setMetaConfigFile(value); }
    public void forceTypeValidation(boolean value) { getForceTypeValidation().set(value); }
    public void disableAreaValidation(boolean value) { getDisableAreaValidation().set(value); }
    public void multiplicityOff(boolean value) { getMultiplicityOff().set(value); }
    public void allObjectsAccessible(boolean value) { getAllObjectsAccessible().set(value); }
    public void skipPolygonBuilding(boolean value) { getSkipPolygonBuilding().set(value); }
    public void logFile(Object value) { getLogFile().set(getProject().file(value)); }
    public void xtflogFile(Object value) { getXtflogFile().set(getProject().file(value)); }
    public void pluginFolder(Object value) { getPluginFolder().set(getProject().file(value)); }
    public void proxy(String value) { getProxy().set(value); }
    public void proxyPort(int value) { getProxyPort().set(value); }
    public void failOnError(boolean value) { getFailOnError().set(value); }

    @TaskAction
    public void validateIli() {
        try {
            validationOk = new IliValidatorEngine().validate(request());
            if (!validationOk && getFailOnError().getOrElse(true)) {
                throw new GradleException("validation failed");
            }
        } catch (Exception e) {
            log.error("Exception in IliValidator task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    private IliValidatorRequest request() {
        return new IliValidatorRequest(
                sortedDataFiles(),
                getModels().getOrNull(),
                getModeldir().getOrNull(),
                resolveReference(configFile),
                resolveReference(metaConfigFile),
                getForceTypeValidation().getOrElse(false),
                getDisableAreaValidation().getOrElse(false),
                getMultiplicityOff().getOrElse(false),
                getAllObjectsAccessible().getOrElse(false),
                getSkipPolygonBuilding().getOrElse(false),
                pathString(getLogFile()),
                pathString(getXtflogFile()),
                getPluginFolder().isPresent() ? getPluginFolder().get().getAsFile().getPath() : null,
                getProxy().getOrNull(),
                getProxyPort().getOrNull(),
                getFailOnError().getOrElse(true)
        );
    }

    private List<String> sortedDataFiles() {
        return dataFiles.getFiles().stream()
                .sorted(Comparator.comparing(File::getPath))
                .map(File::getPath)
                .toList();
    }

    private String resolveReference(Object value) {
        if (value == null) {
            return null;
        }
        if (isIliDataReference(value)) {
            return value.toString();
        }
        return getProject().file(value).getPath();
    }

    private static String pathString(RegularFileProperty property) {
        return property.isPresent() ? property.get().getAsFile().getPath() : null;
    }

    private static boolean isIliDataReference(Object value) {
        return value instanceof String text && text.startsWith("ilidata:");
    }
}
