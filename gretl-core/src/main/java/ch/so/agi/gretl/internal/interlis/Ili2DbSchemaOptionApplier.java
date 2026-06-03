package ch.so.agi.gretl.internal.interlis;

import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.tasks.AbstractIli2DbSchemaImportTask;

final class Ili2DbSchemaOptionApplier {

    void apply(AbstractIli2DbSchemaImportTask task, Config settings) {
        if (task.getOneGeomPerTable().get()) {
            settings.setOneGeomPerTable(true);
        }
        if (task.getDropScript().isPresent()) {
            settings.setDropscript(task.getDropScript().get().getAsFile().getAbsolutePath());
        }
        if (task.getCreateScript().isPresent()) {
            settings.setCreatescript(task.getCreateScript().get().getAsFile().getAbsolutePath());
        }
        if (task.getDefaultSrsAuth().isPresent()) {
            String auth = task.getDefaultSrsAuth().get();
            settings.setDefaultSrsAuthority("NULL".equalsIgnoreCase(auth) ? null : auth);
        }
        if (task.getDefaultSrsCode().isPresent()) {
            settings.setDefaultSrsCode(task.getDefaultSrsCode().get());
        }
        if (task.getCreateSingleEnumTab().get()) {
            settings.setCreateEnumDefs(settings.CREATE_ENUM_DEFS_SINGLE);
        }
        if (task.getCreateEnumTabs().get()) {
            settings.setCreateEnumDefs(settings.CREATE_ENUM_DEFS_MULTI);
        }
        if (task.getCreateEnumTxtCol().get()) {
            settings.setCreateEnumCols(settings.CREATE_ENUM_TXT_COL);
        }
        if (task.getCreateEnumColAsItfCode().get()) {
            settings.setCreateEnumColAsItfCode(settings.CREATE_ENUMCOL_AS_ITFCODE_YES);
        }
        if (task.getCreateEnumTabsWithId().get()) {
            settings.setCreateEnumDefs(Config.CREATE_ENUM_DEFS_MULTI_WITH_ID);
        }
        if (task.getCreateImportTabs().get()) {
            settings.setCreateImportTabs(true);
        }
        if (task.getBeautifyEnumDispName().get()) {
            settings.setBeautifyEnumDispName(settings.BEAUTIFY_ENUM_DISPNAME_UNDERSCORE);
        }
        if (task.getNoSmartMapping().get()) {
            Ili2db.setNoSmartMapping(settings);
        }
        if (task.getSmart1Inheritance().get()) {
            settings.setInheritanceTrafo(settings.INHERITANCE_TRAFO_SMART1);
        }
        if (task.getSmart2Inheritance().get()) {
            settings.setInheritanceTrafo(settings.INHERITANCE_TRAFO_SMART2);
        }
        if (task.getCoalesceCatalogueRef().get()) {
            settings.setCatalogueRefTrafo(settings.CATALOGUE_REF_TRAFO_COALESCE);
        }
        if (task.getCoalesceMultiSurface().get()) {
            settings.setMultiSurfaceTrafo(settings.MULTISURFACE_TRAFO_COALESCE);
        }
        if (task.getCoalesceMultiLine().get()) {
            settings.setMultiLineTrafo(settings.MULTILINE_TRAFO_COALESCE);
        }
        if (task.getExpandMultilingual().get()) {
            settings.setMultilingualTrafo(settings.MULTILINGUAL_TRAFO_EXPAND);
        }
        if (task.getCoalesceJson().get()) {
            settings.setJsonTrafo(settings.JSON_TRAFO_COALESCE);
        }
        if (task.getCoalesceArray().get()) {
            settings.setArrayTrafo(settings.ARRAY_TRAFO_COALESCE);
        }
        if (task.getCreateTypeConstraint().get()) {
            settings.setCreateTypeConstraint(true);
        }
        if (task.getCreateFk().get()) {
            settings.setCreateFk(settings.CREATE_FK_YES);
        }
        if (task.getCreateFkIdx().get()) {
            settings.setCreateFkIdx(settings.CREATE_FKIDX_YES);
        }
        if (task.getCreateUnique().get()) {
            settings.setCreateUniqueConstraints(true);
        }
        if (task.getCreateNumChecks().get()) {
            settings.setCreateNumChecks(true);
        }
        if (task.getCreateTextChecks().get()) {
            settings.setCreateTextChecks(true);
        }
        if (task.getCreateDateTimeChecks().get()) {
            settings.setCreateDateTimeChecks(true);
        }
        if (task.getCreateStdCols().get()) {
            settings.setCreateStdCols(settings.CREATE_STD_COLS_ALL);
        }
        if (task.getTidColumnName().isPresent()) {
            settings.setColT_ID(task.getTidColumnName().get());
        }
        if (task.getIdSeqMin().isPresent()) {
            settings.setMinIdSeqValue(task.getIdSeqMin().get());
        }
        if (task.getIdSeqMax().isPresent()) {
            settings.setMaxIdSeqValue(task.getIdSeqMax().get());
        }
        if (task.getCreateTypeDiscriminator().get()) {
            settings.setCreateTypeDiscriminator(settings.CREATE_TYPE_DISCRIMINATOR_ALWAYS);
        }
        if (task.getCreateGeomIdx().get()) {
            settings.setValue(Config.CREATE_GEOM_INDEX, Config.TRUE);
        }
        if (task.getDisableNameOptimization().get()) {
            settings.setNameOptimization(settings.NAME_OPTIMIZATION_DISABLE);
        }
        if (task.getNameByTopic().get()) {
            settings.setNameOptimization(settings.NAME_OPTIMIZATION_TOPIC);
        }
        if (task.getMaxNameLength().isPresent()) {
            settings.setMaxSqlNameLength(task.getMaxNameLength().get().toString());
        }
        if (task.getSqlEnableNull().get()) {
            settings.setSqlNull(settings.SQL_NULL_ENABLE);
        }
        if (task.getSqlColsAsText().get()) {
            settings.setSqlColsAsText(settings.SQL_COLS_AS_TEXT_ENABLE);
        }
        if (task.getSqlExtRefCols().get()) {
            settings.setSqlExtRefCols(settings.SQL_EXTREF_ENABLE);
        }
        if (task.getKeepAreaRef().get()) {
            settings.setAreaRef(settings.AREA_REF_KEEP);
        }
        if (task.getCreateTidCol().get()) {
            settings.setTidHandling(settings.TID_HANDLING_PROPERTY);
        }
        if (task.getCreateBasketCol().get()) {
            settings.setBasketHandling(settings.BASKET_HANDLING_READWRITE);
        }
        if (task.getCreateDatasetCol().get()) {
            settings.setCreateDatasetCols(settings.CREATE_DATASET_COL);
        }
        if (task.getTranslation().isPresent()) {
            settings.setIli1Translation(task.getTranslation().get());
        }
        if (task.getCreateMetaInfo().get()) {
            settings.setCreateMetaInfo(true);
        }
    }
}
