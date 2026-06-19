package ch.so.agi.gretl.internal.ili2db;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iox_j.plugins.IoxPlugin;
import ch.interlis.iox_j.validator.InterlisFunction;
import org.interlis2.validator.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IliValidatorEngine {
    private static final List<String> CUSTOM_FUNCTIONS = List.of(
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

    public boolean validate(IliValidatorRequest request) throws Exception {
        if (request.dataFiles().isEmpty()) {
            return true;
        }
        Settings settings = new Settings();
        applySettings(settings, request);
        settings.setTransientObject(ch.interlis.iox_j.validator.Validator.CONFIG_CUSTOM_FUNCTIONS, customFunctions());
        return new Validator().validate(request.dataFiles().toArray(String[]::new), settings);
    }

    private static void applySettings(Settings settings, IliValidatorRequest request) {
        settings.setValue(Validator.SETTING_DISABLE_STD_LOGGER, Validator.TRUE);
        set(settings, Validator.SETTING_MODELNAMES, request.models());
        set(settings, Validator.SETTING_ILIDIRS, request.modeldir());
        set(settings, Validator.SETTING_CONFIGFILE, request.configFile());
        set(settings, Validator.SETTING_META_CONFIGFILE, request.metaConfigFile());
        if (request.forceTypeValidation()) {
            settings.setValue(Validator.SETTING_FORCE_TYPE_VALIDATION, Validator.TRUE);
        }
        if (request.disableAreaValidation()) {
            settings.setValue(Validator.SETTING_DISABLE_AREA_VALIDATION, Validator.TRUE);
        }
        if (request.multiplicityOff()) {
            settings.setValue(Validator.SETTING_MULTIPLICITY_VALIDATION,
                    ch.interlis.iox_j.validator.ValidationConfig.OFF);
        }
        if (request.allObjectsAccessible()) {
            settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.TRUE);
        }
        if (request.skipPolygonBuilding()) {
            settings.setValue(ch.interlis.iox_j.validator.Validator.CONFIG_DO_ITF_LINETABLES,
                    ch.interlis.iox_j.validator.Validator.CONFIG_DO_ITF_LINETABLES_DO);
        }
        set(settings, Validator.SETTING_LOGFILE, request.logFile());
        set(settings, Validator.SETTING_XTFLOG, request.xtflogFile());
        set(settings, Validator.SETTING_PLUGINFOLDER, request.pluginFolder());
        set(settings, ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_HOST, request.proxy());
        if (request.proxyPort() != null) {
            settings.setValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_PORT, request.proxyPort().toString());
        }
    }

    private static void set(Settings settings, String key, String value) {
        if (value != null) {
            settings.setValue(key, value);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Class> customFunctions() throws Exception {
        Map<String, Class> functions = new HashMap<>();
        for (String className : CUSTOM_FUNCTIONS) {
            Class clazz = Class.forName(className);
            IoxPlugin plugin = (IoxPlugin) clazz.getDeclaredConstructor().newInstance();
            functions.put(((InterlisFunction) plugin).getQualifiedIliName(), clazz);
        }
        return functions;
    }
}
