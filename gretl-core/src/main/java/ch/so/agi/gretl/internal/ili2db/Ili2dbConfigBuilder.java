package ch.so.agi.gretl.internal.ili2db;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;

public final class Ili2dbConfigBuilder {

    private Ili2dbConfigBuilder() {
    }

    public static Config config(Ili2dbFlavor flavor, Ili2dbOperation operation, Ili2dbCommonOptions common) {
        Config config = new Config();
        flavor.init(config);
        config.setFunction(operation.function());
        applyCommon(config, operation, common);
        return config;
    }

    public static void applySchemaImport(Config config, Ili2dbSchemaImportOptions options) {
        if (options == null) {
            return;
        }
        if (options.iliFile() != null) {
            config.setXtffile(options.iliFile());
        }
        if (options.iliMetaAttrs() != null) {
            config.setIliMetaAttrsFile(options.iliMetaAttrs());
        }
        if (options.oneGeomPerTable()) {
            config.setOneGeomPerTable(true);
        }
        if (options.setupPgExt()) {
            config.setSetupPgExt(true);
        }
        if (options.dropscript() != null) {
            config.setDropscript(options.dropscript());
        }
        if (options.createscript() != null) {
            config.setCreatescript(options.createscript());
        }
        if (options.metaConfig() != null) {
            config.setMetaConfigFile(options.metaConfig());
        }
        if (options.defaultSrsAuth() != null) {
            config.setDefaultSrsAuthority("NULL".equalsIgnoreCase(options.defaultSrsAuth()) ? null : options.defaultSrsAuth());
        }
        if (options.defaultSrsCode() != null) {
            config.setDefaultSrsCode(options.defaultSrsCode());
        }
        if (options.createSingleEnumTab()) {
            config.setCreateEnumDefs(Config.CREATE_ENUM_DEFS_SINGLE);
        }
        if (options.createEnumTabs()) {
            config.setCreateEnumDefs(Config.CREATE_ENUM_DEFS_MULTI);
        }
        if (options.createEnumTxtCol()) {
            config.setCreateEnumCols(Config.CREATE_ENUM_TXT_COL);
        }
        if (options.createEnumColAsItfCode()) {
            config.setCreateEnumColAsItfCode(Config.CREATE_ENUMCOL_AS_ITFCODE_YES);
        }
        if (options.createEnumTabsWithId()) {
            config.setCreateEnumDefs(Config.CREATE_ENUM_DEFS_MULTI_WITH_ID);
        }
        if (options.createImportTabs()) {
            config.setCreateImportTabs(true);
        }
        if (options.beautifyEnumDispName()) {
            config.setBeautifyEnumDispName(Config.BEAUTIFY_ENUM_DISPNAME_UNDERSCORE);
        }
        if (options.noSmartMapping()) {
            Ili2db.setNoSmartMapping(config);
        }
        if (options.smart1Inheritance()) {
            config.setInheritanceTrafo(Config.INHERITANCE_TRAFO_SMART1);
        }
        if (options.smart2Inheritance()) {
            config.setInheritanceTrafo(Config.INHERITANCE_TRAFO_SMART2);
        }
        if (options.coalesceCatalogueRef()) {
            config.setCatalogueRefTrafo(Config.CATALOGUE_REF_TRAFO_COALESCE);
        }
        if (options.coalesceMultiSurface()) {
            config.setMultiSurfaceTrafo(Config.MULTISURFACE_TRAFO_COALESCE);
        }
        if (options.coalesceMultiLine()) {
            config.setMultiLineTrafo(Config.MULTILINE_TRAFO_COALESCE);
        }
        if (options.expandMultilingual()) {
            config.setMultilingualTrafo(Config.MULTILINGUAL_TRAFO_EXPAND);
        }
        if (options.expandStruct()) {
            config.setStructTrafo(Config.STRUCT_TRAFO_EXPAND);
        }
        if (options.coalesceJson()) {
            config.setJsonTrafo(Config.JSON_TRAFO_COALESCE);
        }
        if (options.coalesceArray()) {
            config.setArrayTrafo(Config.ARRAY_TRAFO_COALESCE);
        }
        if (options.createTypeConstraint()) {
            config.setCreateTypeConstraint(true);
        }
        if (options.createFk()) {
            config.setCreateFk(Config.CREATE_FK_YES);
        }
        if (options.createFkIdx()) {
            config.setCreateFkIdx(Config.CREATE_FKIDX_YES);
        }
        if (options.createUnique()) {
            config.setCreateUniqueConstraints(true);
        }
        if (options.createNumChecks()) {
            config.setCreateNumChecks(true);
        }
        if (options.createTextChecks()) {
            config.setCreateTextChecks(true);
        }
        if (options.createDateTimeChecks()) {
            config.setCreateDateTimeChecks(true);
        }
        if (options.createStdCols()) {
            config.setCreateStdCols(Config.CREATE_STD_COLS_ALL);
        }
        if (options.tIdName() != null) {
            config.setColT_ID(options.tIdName());
        }
        if (options.idSeqMin() != null) {
            config.setMinIdSeqValue(options.idSeqMin());
        }
        if (options.idSeqMax() != null) {
            config.setMaxIdSeqValue(options.idSeqMax());
        }
        if (options.createTypeDiscriminator()) {
            config.setCreateTypeDiscriminator(Config.CREATE_TYPE_DISCRIMINATOR_ALWAYS);
        }
        if (options.createGeomIdx()) {
            config.setValue(Config.CREATE_GEOM_INDEX, Config.TRUE);
        }
        if (options.disableNameOptimization()) {
            config.setNameOptimization(Config.NAME_OPTIMIZATION_DISABLE);
        }
        if (options.nameByTopic()) {
            config.setNameOptimization(Config.NAME_OPTIMIZATION_TOPIC);
        }
        if (options.maxNameLength() != null) {
            config.setMaxSqlNameLength(options.maxNameLength().toString());
        }
        if (options.sqlEnableNull()) {
            config.setSqlNull(Config.SQL_NULL_ENABLE);
        }
        if (options.sqlColsAsText()) {
            config.setSqlColsAsText(Config.SQL_COLS_AS_TEXT_ENABLE);
        }
        if (options.sqlExtRefCols()) {
            config.setSqlExtRefCols(Config.SQL_EXTREF_ENABLE);
        }
        if (options.keepAreaRef()) {
            config.setAreaRef(Config.AREA_REF_KEEP);
        }
        if (options.createTidCol()) {
            config.setTidHandling(Config.TID_HANDLING_PROPERTY);
        }
        if (options.createBasketCol()) {
            config.setBasketHandling(Config.BASKET_HANDLING_READWRITE);
        }
        if (options.createDatasetCol()) {
            config.setCreateDatasetCols(Config.CREATE_DATASET_COL);
        }
        if (options.translation() != null) {
            config.setIli1Translation(options.translation());
        }
        if (options.createMetaInfo()) {
            config.setCreateMetaInfo(true);
        }
    }

    public static void applyExport(Config config, Ili2dbExportOptions options) {
        if (options == null) {
            return;
        }
        if (options.export3()) {
            config.setVer3_export(true);
        }
        if (options.exportModels() != null) {
            config.setExportModels(options.exportModels());
        }
    }

    public static void applyGpkgImport(Config config, Ili2dbGpkgImportOptions options, boolean hasDatasets) {
        config.setDoImplicitSchemaImport(true);
        if (hasDatasets) {
            config.setBasketHandling(Config.BASKET_HANDLING_READWRITE);
        }
        if (options == null) {
            return;
        }
        if (options.coalesceJson()) {
            config.setJsonTrafo(Config.JSON_TRAFO_COALESCE);
        }
        if (options.nameByTopic()) {
            config.setNameOptimization(Config.NAME_OPTIMIZATION_TOPIC);
        }
        if (options.defaultSrsCode() != null) {
            config.setDefaultSrsCode(options.defaultSrsCode());
        }
        if (options.createEnumTabs()) {
            config.setCreateEnumDefs(Config.CREATE_ENUM_DEFS_MULTI);
        }
        if (options.createMetaInfo()) {
            config.setCreateMetaInfo(true);
        }
        if (options.createGeomIdx()) {
            config.setValue(Config.CREATE_GEOM_INDEX, Config.TRUE);
        }
    }

    private static void applyCommon(Config config, Ili2dbOperation operation, Ili2dbCommonOptions options) {
        if (options == null) {
            return;
        }
        if (options.proxy() != null) {
            config.setValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_HOST, options.proxy());
        }
        if (options.proxyPort() != null) {
            config.setValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_PORT, options.proxyPort().toString());
        }
        if (options.dbschema() != null) {
            config.setDbschema(options.dbschema());
        }
        if (options.modeldir() != null) {
            config.setModeldir(options.modeldir());
        }
        if (options.models() != null) {
            config.setModels(options.models());
        }
        if (options.baskets() != null) {
            config.setBaskets(options.baskets());
        }
        if (options.topics() != null) {
            config.setTopics(options.topics());
        }
        if (options.importTid()) {
            config.setImportTid(true);
        }
        if (options.exportTid()) {
            config.setExportTid(true);
        }
        if (options.importBid()) {
            config.setImportBid(true);
        }
        if (options.preScript() != null) {
            config.setPreScript(options.preScript().toString());
        }
        if (options.postScript() != null) {
            config.setPostScript(options.postScript().toString());
        }
        if (options.deleteData()) {
            config.setDeleteMode(Config.DELETE_DATA);
        }
        if (!operation.usesExternalFileLogger() && options.logFile() != null) {
            config.setLogfile(options.logFile().toString());
        }
        if (options.trace()) {
            EhiLogger.getInstance().setTraceFilter(false);
        }
        if (options.validConfigFile() != null) {
            config.setValidConfigFile(options.validConfigFile().toString());
        }
        if (options.disableValidation()) {
            config.setValidation(false);
        }
        if (options.disableAreaValidation()) {
            config.setDisableAreaValidation(true);
        }
        if (options.forceTypeValidation()) {
            config.setOnlyMultiplicityReduction(true);
        }
        if (options.strokeArcs()) {
            config.setStrokeArcs(Config.STROKE_ARCS_ENABLE);
        }
        if (options.skipPolygonBuilding()) {
            Ili2db.setSkipPolygonBuilding(config);
        }
        if (options.skipGeometryErrors()) {
            config.setSkipGeometryErrors(true);
        }
        if (options.iligml20()) {
            config.setTransferFileFormat(Config.ILIGML20);
        }
        if (options.disableRounding()) {
            config.setDisableRounding(true);
        }
    }
}
