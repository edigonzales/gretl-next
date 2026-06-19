package ch.so.agi.gretl.internal.ili2db;

import java.nio.file.Path;

public record Ili2dbCommonOptions(
        String dbschema,
        String proxy,
        Integer proxyPort,
        String modeldir,
        String models,
        String baskets,
        String topics,
        boolean importTid,
        boolean exportTid,
        boolean importBid,
        Path preScript,
        Path postScript,
        boolean deleteData,
        Path logFile,
        boolean trace,
        Path validConfigFile,
        boolean disableValidation,
        boolean disableAreaValidation,
        boolean forceTypeValidation,
        boolean strokeArcs,
        boolean skipPolygonBuilding,
        boolean skipGeometryErrors,
        boolean iligml20,
        boolean disableRounding
) {
}
