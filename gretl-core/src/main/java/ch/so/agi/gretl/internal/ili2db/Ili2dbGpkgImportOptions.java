package ch.so.agi.gretl.internal.ili2db;

public record Ili2dbGpkgImportOptions(
        boolean coalesceJson,
        boolean nameByTopic,
        String defaultSrsCode,
        boolean createEnumTabs,
        boolean createMetaInfo,
        boolean createGeomIdx
) {
}
