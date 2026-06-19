package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

public abstract class AbstractIli2DbSchemaImportTask extends AbstractIli2DbTask {

    @InputFile
    @Optional
    public abstract RegularFileProperty getIliFile();

    @InputFile
    @Optional
    public abstract RegularFileProperty getIliMetaAttrsFile();

    @Input
    public abstract Property<Boolean> getOneGeomPerTable();

    @InputFile
    @Optional
    public abstract RegularFileProperty getDropScript();

    @InputFile
    @Optional
    public abstract RegularFileProperty getCreateScript();

    @Input
    @Optional
    public abstract Property<String> getDefaultSrsAuth();

    @Input
    @Optional
    public abstract Property<String> getDefaultSrsCode();

    @Input
    public abstract Property<Boolean> getCreateSingleEnumTab();

    @Input
    public abstract Property<Boolean> getCreateEnumTabs();

    @Input
    public abstract Property<Boolean> getCreateEnumTxtCol();

    @Input
    public abstract Property<Boolean> getCreateEnumColAsItfCode();

    @Input
    public abstract Property<Boolean> getCreateEnumTabsWithId();

    @Input
    public abstract Property<Boolean> getCreateImportTabs();

    @Input
    public abstract Property<Boolean> getBeautifyEnumDispName();

    @Input
    public abstract Property<Boolean> getNoSmartMapping();

    @Input
    public abstract Property<Boolean> getSmart1Inheritance();

    @Input
    public abstract Property<Boolean> getSmart2Inheritance();

    @Input
    public abstract Property<Boolean> getCoalesceCatalogueRef();

    @Input
    public abstract Property<Boolean> getCoalesceMultiSurface();

    @Input
    public abstract Property<Boolean> getCoalesceMultiLine();

    @Input
    public abstract Property<Boolean> getExpandMultilingual();

    @Input
    public abstract Property<Boolean> getCoalesceJson();

    @Input
    public abstract Property<Boolean> getCoalesceArray();

    @Input
    public abstract Property<Boolean> getCreateTypeConstraint();

    @Input
    public abstract Property<Boolean> getCreateFk();

    @Input
    public abstract Property<Boolean> getCreateFkIdx();

    @Input
    public abstract Property<Boolean> getCreateUnique();

    @Input
    public abstract Property<Boolean> getCreateNumChecks();

    @Input
    public abstract Property<Boolean> getCreateTextChecks();

    @Input
    public abstract Property<Boolean> getCreateDateTimeChecks();

    @Input
    public abstract Property<Boolean> getCreateStdCols();

    @Input
    @Optional
    public abstract Property<String> getTidColumnName();

    @Input
    @Optional
    public abstract Property<Long> getIdSeqMin();

    @Input
    @Optional
    public abstract Property<Long> getIdSeqMax();

    @Input
    public abstract Property<Boolean> getCreateTypeDiscriminator();

    @Input
    public abstract Property<Boolean> getCreateGeomIdx();

    @Input
    public abstract Property<Boolean> getDisableNameOptimization();

    @Input
    public abstract Property<Boolean> getNameByTopic();

    @Input
    @Optional
    public abstract Property<Integer> getMaxNameLength();

    @Input
    public abstract Property<Boolean> getSqlEnableNull();

    @Input
    public abstract Property<Boolean> getSqlColsAsText();

    @Input
    public abstract Property<Boolean> getSqlExtRefCols();

    @Input
    public abstract Property<Boolean> getKeepAreaRef();

    @Input
    public abstract Property<Boolean> getCreateTidCol();

    @Input
    public abstract Property<Boolean> getCreateBasketCol();

    @Input
    public abstract Property<Boolean> getCreateDatasetCol();

    @Input
    @Optional
    public abstract Property<String> getTranslation();

    @Input
    public abstract Property<Boolean> getCreateMetaInfo();

    @Inject
    public AbstractIli2DbSchemaImportTask() {
        getOneGeomPerTable().convention(false);
        getCreateSingleEnumTab().convention(false);
        getCreateEnumTabs().convention(false);
        getCreateEnumTxtCol().convention(false);
        getCreateEnumColAsItfCode().convention(false);
        getCreateEnumTabsWithId().convention(false);
        getCreateImportTabs().convention(false);
        getBeautifyEnumDispName().convention(false);
        getNoSmartMapping().convention(false);
        getSmart1Inheritance().convention(false);
        getSmart2Inheritance().convention(false);
        getCoalesceCatalogueRef().convention(false);
        getCoalesceMultiSurface().convention(false);
        getCoalesceMultiLine().convention(false);
        getExpandMultilingual().convention(false);
        getCoalesceJson().convention(false);
        getCoalesceArray().convention(false);
        getCreateTypeConstraint().convention(false);
        getCreateFk().convention(false);
        getCreateFkIdx().convention(false);
        getCreateUnique().convention(false);
        getCreateNumChecks().convention(false);
        getCreateTextChecks().convention(false);
        getCreateDateTimeChecks().convention(false);
        getCreateStdCols().convention(false);
        getCreateTypeDiscriminator().convention(false);
        getCreateGeomIdx().convention(false);
        getDisableNameOptimization().convention(false);
        getNameByTopic().convention(false);
        getSqlEnableNull().convention(false);
        getSqlColsAsText().convention(false);
        getSqlExtRefCols().convention(false);
        getKeepAreaRef().convention(false);
        getCreateTidCol().convention(false);
        getCreateBasketCol().convention(false);
        getCreateDatasetCol().convention(false);
        getCreateMetaInfo().convention(false);
    }

    @GretlDslMethod(description = "Uses a local INTERLIS model file for schema import.")
    public void iliFile(Object path) {
        getIliFile().fileValue(getProject().file(path));
    }

    @GretlDslMethod(description = "Uses a local iliMetaAttrs file for schema import.")
    public void iliMetaAttrsFile(Object path) {
        getIliMetaAttrsFile().fileValue(getProject().file(path));
    }
}
