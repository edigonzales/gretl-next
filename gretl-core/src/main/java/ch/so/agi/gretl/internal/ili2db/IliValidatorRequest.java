package ch.so.agi.gretl.internal.ili2db;

import java.util.List;

public record IliValidatorRequest(
        List<String> dataFiles,
        String models,
        String modeldir,
        String configFile,
        String metaConfigFile,
        boolean forceTypeValidation,
        boolean disableAreaValidation,
        boolean multiplicityOff,
        boolean allObjectsAccessible,
        boolean skipPolygonBuilding,
        String logFile,
        String xtflogFile,
        String pluginFolder,
        String proxy,
        Integer proxyPort,
        boolean failOnError
) {
    public IliValidatorRequest {
        dataFiles = dataFiles == null ? List.of() : List.copyOf(dataFiles);
    }
}
