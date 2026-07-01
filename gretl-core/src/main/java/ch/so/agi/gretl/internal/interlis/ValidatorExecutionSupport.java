package ch.so.agi.gretl.internal.interlis;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom_j.csv.CsvReader;
import ch.interlis.iox_j.plugins.IoxPlugin;
import ch.interlis.iox_j.validator.InterlisFunction;
import ch.interlis.ioxwkf.dbtools.IoxWkfConfig;
import ch.so.agi.gretl.tasks.AbstractInterlisValidatorTask;
import ch.so.agi.gretl.tasks.CsvValidator;
import ch.so.agi.gretl.tasks.GpkgValidator;
import ch.so.agi.gretl.tasks.IliValidator;
import ch.so.agi.gretl.tasks.JsonValidator;
import ch.so.agi.gretl.tasks.ShpValidator;
import ch.so.agi.gretl.internal.shapefile.ShapefileConstants;
import org.gradle.api.GradleException;
import org.interlis2.validator.Validator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ValidatorExecutionSupport {

    public boolean validate(IliValidator task) {
        if (task.getDataFiles().isEmpty()) {
            return true;
        }

        Settings settings = createSettings(task);
        settings.setTransientObject(ch.interlis.iox_j.validator.Validator.CONFIG_CUSTOM_FUNCTIONS, createCustomFunctions());

        return new Validator().validate(toPaths(task.getDataFiles().getFiles()), settings);
    }

    public boolean validate(CsvValidator task) {
        if (task.getDataFiles().isEmpty()) {
            return true;
        }
        if (task.getDataFiles().getFiles().size() > 1) {
            throw new GradleException("CsvValidator accepts exactly one input file.");
        }

        Settings settings = createSettings(task);
        settings.setValue(IoxWkfConfig.SETTING_FIRSTLINE,
                task.getFirstLineIsHeader().get()
                        ? IoxWkfConfig.SETTING_FIRSTLINE_AS_HEADER
                        : IoxWkfConfig.SETTING_FIRSTLINE_AS_VALUE);
        if (task.getValueDelimiter().isPresent()) {
            settings.setValue(IoxWkfConfig.SETTING_VALUEDELIMITER,
                    requireSingleCharacter("valueDelimiter", task.getValueDelimiter().get()));
        }
        if (task.getValueSeparator().isPresent()) {
            settings.setValue(IoxWkfConfig.SETTING_VALUESEPARATOR,
                    requireSingleCharacter("valueSeparator", task.getValueSeparator().get()));
        }
        if (task.getEncoding().isPresent()) {
            settings.setValue(CsvReader.ENCODING, task.getEncoding().get());
        }

        return new CsvValidatorImpl().validate(toPaths(task.getDataFiles().getFiles()), settings);
    }

    public boolean validate(JsonValidator task) {
        if (task.getDataFiles().isEmpty()) {
            return true;
        }

        Settings settings = createSettings(task);
        settings.setTransientObject(ch.interlis.iox_j.validator.Validator.CONFIG_CUSTOM_FUNCTIONS, createCustomFunctions());
        return new JsonValidatorImpl().validate(toPaths(task.getDataFiles().getFiles()), settings);
    }

    public boolean validate(GpkgValidator task) {
        if (task.getDataFiles().isEmpty()) {
            return true;
        }
        if (!task.getTableName().isPresent() || task.getTableName().get().isBlank()) {
            throw new GradleException("tableName must not be null or blank");
        }

        Settings settings = createSettings(task);
        settings.setValue(IoxWkfConfig.SETTING_GPKGTABLE, task.getTableName().get());
        settings.setTransientObject(ch.interlis.iox_j.validator.Validator.CONFIG_CUSTOM_FUNCTIONS, createCustomFunctions());
        return new GpkgValidatorImpl().validate(toPaths(task.getDataFiles().getFiles()), settings);
    }

    public boolean validate(ShpValidator task) {
        if (task.getDataFiles().isEmpty()) {
            return true;
        }
        if (task.getDataFiles().getFiles().size() > 1) {
            throw new GradleException("ShpValidator accepts exactly one input file.");
        }

        Settings settings = createSettings(task);
        if (task.getEncoding().isPresent()) {
            settings.setValue(ShapefileConstants.ENCODING, task.getEncoding().get());
        }
        settings.setTransientObject(ch.interlis.iox_j.validator.Validator.CONFIG_CUSTOM_FUNCTIONS, createCustomFunctions());
        return new ShpValidatorImpl().validate(toPaths(task.getDataFiles().getFiles()), settings);
    }

    private Settings createSettings(AbstractInterlisValidatorTask task) {
        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_DISABLE_STD_LOGGER, Validator.TRUE);

        if (!task.getModelNames().get().isEmpty()) {
            settings.setValue(Validator.SETTING_MODELNAMES, String.join(";", task.getModelNames().get()));
        }
        if (!task.getModelDirectories().get().isEmpty()) {
            settings.setValue(Validator.SETTING_ILIDIRS, String.join(";", task.getModelDirectories().get()));
        }
        applyMutuallyExclusiveFileReference(settings, Validator.SETTING_CONFIGFILE,
                task.getConfigFile().isPresent() ? task.getConfigFile().get().getAsFile() : null,
                task.getConfigRepositoryId().getOrNull(),
                "configFile",
                "configRepositoryId");
        applyMutuallyExclusiveFileReference(settings, Validator.SETTING_META_CONFIGFILE,
                task.getMetaConfigFile().isPresent() ? task.getMetaConfigFile().get().getAsFile() : null,
                task.getMetaConfigRepositoryId().getOrNull(),
                "metaConfigFile",
                "metaConfigRepositoryId");

        if (task.getForceTypeValidation().get()) {
            settings.setValue(Validator.SETTING_FORCE_TYPE_VALIDATION, Validator.TRUE);
        }
        if (task.getDisableAreaValidation().get()) {
            settings.setValue(Validator.SETTING_DISABLE_AREA_VALIDATION, Validator.TRUE);
        }
        if (task.getMultiplicityOff().get()) {
            settings.setValue(Validator.SETTING_MULTIPLICITY_VALIDATION,
                    ch.interlis.iox_j.validator.ValidationConfig.OFF);
        }
        if (task.getAllObjectsAccessible().get()) {
            settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.TRUE);
        }
        if (task.getSkipPolygonBuilding().get()) {
            settings.setValue(ch.interlis.iox_j.validator.Validator.CONFIG_DO_ITF_LINETABLES,
                    ch.interlis.iox_j.validator.Validator.CONFIG_DO_ITF_LINETABLES_DO);
        }
        if (task.getLogFile().isPresent()) {
            settings.setValue(Validator.SETTING_LOGFILE, task.getLogFile().get().getAsFile().getAbsolutePath());
        }
        if (task.getXtfLogFile().isPresent()) {
            settings.setValue(Validator.SETTING_XTFLOG, task.getXtfLogFile().get().getAsFile().getAbsolutePath());
        }
        if (task.getProxy().isPresent()) {
            settings.setValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_HOST, task.getProxy().get());
        }
        if (task.getProxyPort().isPresent()) {
            settings.setValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_PORT, task.getProxyPort().get().toString());
        }
        return settings;
    }

    private void applyMutuallyExclusiveFileReference(Settings settings, String key, File file, String repositoryId,
                                                     String fileMethod, String repositoryMethod) {
        if (file != null && repositoryId != null) {
            throw new GradleException("Use either " + fileMethod + "(...) or " + repositoryMethod + "(...), not both.");
        }
        if (file != null) {
            settings.setValue(key, file.getAbsolutePath());
        } else if (repositoryId != null) {
            settings.setValue(key, repositoryId);
        }
    }

    private Map<String, Class<?>> createCustomFunctions() {
        List<String> classNames = List.of(
                "ch.so.agi.ilivalidator.ext.IsHttpResourceIoxPlugin",
                "ch.so.agi.ilivalidator.ext.AreaIoxPlugin",
                "ch.so.agi.ilivalidator.ext.LengthIoxPlugin",
                "ch.so.agi.ilivalidator.ext.IsValidDocumentsCycleIoxPlugin",
                "ch.so.agi.ilivalidator.ext.RingSelfIntersectionIoxPlugin",
                "ch.so.agi.ilivalidator.ext.TooFewPointsPolylineIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.GetAreaIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.GetLengthIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.GetInnerRingsCountIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.GetInGroupsIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.IsInsideExternalXtfIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.IsInsideExternalXtfResourceIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.IsInsideIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.UnionIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.PolylinesOverlapIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.FindObjectsIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.FilterIoxPlugin",
                "ch.geowerkstatt.ilivalidator.extensions.functions.ngk.IsInsideAreaByCodeIoxPlugin"
        );

        Map<String, Class<?>> functions = new LinkedHashMap<>();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                IoxPlugin plugin = (IoxPlugin) clazz.getDeclaredConstructor().newInstance();
                functions.put(((InterlisFunction) plugin).getQualifiedIliName(), clazz);
            } catch (Exception e) {
                throw new GradleException("Failed to register ilivalidator custom function " + className, e);
            }
        }
        return functions;
    }

    private String[] toPaths(Iterable<File> files) {
        return toPathList(files).toArray(String[]::new);
    }

    private List<String> toPathList(Iterable<File> files) {
        List<String> result = new ArrayList<>();
        for (File file : files) {
            result.add(file.getAbsolutePath());
        }
        return result;
    }

    private String requireSingleCharacter(String propertyName, String value) {
        if (value.length() != 1) {
            throw new GradleException(propertyName + " must be a single character");
        }
        return value;
    }
}
