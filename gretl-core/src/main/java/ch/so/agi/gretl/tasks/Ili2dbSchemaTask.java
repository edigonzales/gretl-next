package ch.so.agi.gretl.tasks;

import ch.ehi.basics.view.GenericFileFilter;
import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.internal.ili2db.Ili2dbSchemaImportOptions;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;

abstract class Ili2dbSchemaTask extends Ili2dbTask {
    private final ConfigurableFileCollection iliFileInputs;
    private Object iliFile;

    @Inject
    public Ili2dbSchemaTask() {
        this.iliFileInputs = getProject().files();
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getIliFileInputs() {
        return iliFileInputs;
    }

    @Input
    @Optional
    public String getIliFileReference() {
        if (iliFile instanceof String value && isRepositoryReference(value)) {
            return value;
        }
        return null;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getIliMetaAttrs();

    @Input
    @Optional
    public abstract Property<Boolean> getOneGeomPerTable();
    @Input @Optional public abstract Property<Boolean> getSetupPgExt();
    @InputFile @Optional @PathSensitive(PathSensitivity.RELATIVE) public abstract RegularFileProperty getDropscript();
    @InputFile @Optional @PathSensitive(PathSensitivity.RELATIVE) public abstract RegularFileProperty getCreatescript();
    @Input @Optional public abstract Property<String> getMetaConfig();
    @Input @Optional public abstract Property<String> getDefaultSrsAuth();
    @Input @Optional public abstract Property<String> getDefaultSrsCode();
    @Input @Optional public abstract Property<Boolean> getCreateSingleEnumTab();
    @Input @Optional public abstract Property<Boolean> getCreateEnumTabs();
    @Input @Optional public abstract Property<Boolean> getCreateEnumTxtCol();
    @Input @Optional public abstract Property<Boolean> getCreateEnumColAsItfCode();
    @Input @Optional public abstract Property<Boolean> getCreateEnumTabsWithId();
    @Input @Optional public abstract Property<Boolean> getCreateImportTabs();
    @Input @Optional public abstract Property<Boolean> getBeautifyEnumDispName();
    @Input @Optional public abstract Property<Boolean> getNoSmartMapping();
    @Input @Optional public abstract Property<Boolean> getSmart1Inheritance();
    @Input @Optional public abstract Property<Boolean> getSmart2Inheritance();
    @Input @Optional public abstract Property<Boolean> getCoalesceCatalogueRef();
    @Input @Optional public abstract Property<Boolean> getCoalesceMultiSurface();
    @Input @Optional public abstract Property<Boolean> getCoalesceMultiLine();
    @Input @Optional public abstract Property<Boolean> getExpandMultilingual();
    @Input @Optional public abstract Property<Boolean> getExpandStruct();
    @Input @Optional public abstract Property<Boolean> getCoalesceJson();
    @Input @Optional public abstract Property<Boolean> getCoalesceArray();
    @Input @Optional public abstract Property<Boolean> getCreateTypeConstraint();
    @Input @Optional public abstract Property<Boolean> getCreateFk();
    @Input @Optional public abstract Property<Boolean> getCreateFkIdx();
    @Input @Optional public abstract Property<Boolean> getCreateUnique();
    @Input @Optional public abstract Property<Boolean> getCreateNumChecks();
    @Input @Optional public abstract Property<Boolean> getCreateTextChecks();
    @Input @Optional public abstract Property<Boolean> getCreateDateTimeChecks();
    @Input @Optional public abstract Property<Boolean> getCreateStdCols();
    @Input @Optional public abstract Property<String> getT_id_Name();
    @Input @Optional public abstract Property<Long> getIdSeqMin();
    @Input @Optional public abstract Property<Long> getIdSeqMax();
    @Input @Optional public abstract Property<Boolean> getCreateTypeDiscriminator();
    @Input @Optional public abstract Property<Boolean> getCreateGeomIdx();
    @Input @Optional public abstract Property<Boolean> getDisableNameOptimization();
    @Input @Optional public abstract Property<Boolean> getNameByTopic();
    @Input @Optional public abstract Property<Integer> getMaxNameLength();
    @Input @Optional public abstract Property<Boolean> getSqlEnableNull();
    @Input @Optional public abstract Property<Boolean> getSqlColsAsText();
    @Input @Optional public abstract Property<Boolean> getSqlExtRefCols();
    @Input @Optional public abstract Property<Boolean> getKeepAreaRef();
    @Input @Optional public abstract Property<Boolean> getCreateTidCol();
    @Input @Optional public abstract Property<Boolean> getCreateBasketCol();
    @Input @Optional public abstract Property<Boolean> getCreateDatasetCol();
    @Input @Optional public abstract Property<String> getTranslation();
    @Input @Optional public abstract Property<Boolean> getCreateMetaInfo();

    public void setIliFile(Object iliFile) {
        this.iliFile = iliFile;
        iliFileInputs.setFrom();
        if (iliFile != null && !(iliFile instanceof String value && isRepositoryReference(value))) {
            iliFileInputs.from(iliFile);
        }
    }

    @GretlDslMethod(description = "Configures a local ili file or repository reference.")
    public void iliFile(Object iliFile) {
        setIliFile(iliFile);
    }

    public void iliMetaAttrs(Object value) { getIliMetaAttrs().set(getProject().file(value)); }
    public void dropscript(Object value) { getDropscript().set(getProject().file(value)); }
    public void createscript(Object value) { getCreatescript().set(getProject().file(value)); }
    public void oneGeomPerTable(boolean value) { getOneGeomPerTable().set(value); }
    public void setupPgExt(boolean value) { getSetupPgExt().set(value); }
    public void metaConfig(String value) { getMetaConfig().set(value); }
    public void defaultSrsAuth(String value) { getDefaultSrsAuth().set(value); }
    public void defaultSrsCode(String value) { getDefaultSrsCode().set(value); }
    public void createSingleEnumTab(boolean value) { getCreateSingleEnumTab().set(value); }
    public void createEnumTabs(boolean value) { getCreateEnumTabs().set(value); }
    public void createEnumTxtCol(boolean value) { getCreateEnumTxtCol().set(value); }
    public void createEnumColAsItfCode(boolean value) { getCreateEnumColAsItfCode().set(value); }
    public void createEnumTabsWithId(boolean value) { getCreateEnumTabsWithId().set(value); }
    public void createImportTabs(boolean value) { getCreateImportTabs().set(value); }
    public void beautifyEnumDispName(boolean value) { getBeautifyEnumDispName().set(value); }
    public void noSmartMapping(boolean value) { getNoSmartMapping().set(value); }
    public void smart1Inheritance(boolean value) { getSmart1Inheritance().set(value); }
    public void smart2Inheritance(boolean value) { getSmart2Inheritance().set(value); }
    public void coalesceCatalogueRef(boolean value) { getCoalesceCatalogueRef().set(value); }
    public void coalesceMultiSurface(boolean value) { getCoalesceMultiSurface().set(value); }
    public void coalesceMultiLine(boolean value) { getCoalesceMultiLine().set(value); }
    public void expandMultilingual(boolean value) { getExpandMultilingual().set(value); }
    public void expandStruct(boolean value) { getExpandStruct().set(value); }
    public void coalesceJson(boolean value) { getCoalesceJson().set(value); }
    public void coalesceArray(boolean value) { getCoalesceArray().set(value); }
    public void createTypeConstraint(boolean value) { getCreateTypeConstraint().set(value); }
    public void createFk(boolean value) { getCreateFk().set(value); }
    public void createFkIdx(boolean value) { getCreateFkIdx().set(value); }
    public void createUnique(boolean value) { getCreateUnique().set(value); }
    public void createNumChecks(boolean value) { getCreateNumChecks().set(value); }
    public void createTextChecks(boolean value) { getCreateTextChecks().set(value); }
    public void createDateTimeChecks(boolean value) { getCreateDateTimeChecks().set(value); }
    public void createStdCols(boolean value) { getCreateStdCols().set(value); }
    public void t_id_Name(String value) { getT_id_Name().set(value); }
    public void idSeqMin(long value) { getIdSeqMin().set(value); }
    public void idSeqMax(long value) { getIdSeqMax().set(value); }
    public void createTypeDiscriminator(boolean value) { getCreateTypeDiscriminator().set(value); }
    public void createGeomIdx(boolean value) { getCreateGeomIdx().set(value); }
    public void disableNameOptimization(boolean value) { getDisableNameOptimization().set(value); }
    public void nameByTopic(boolean value) { getNameByTopic().set(value); }
    public void maxNameLength(int value) { getMaxNameLength().set(value); }
    public void sqlEnableNull(boolean value) { getSqlEnableNull().set(value); }
    public void sqlColsAsText(boolean value) { getSqlColsAsText().set(value); }
    public void sqlExtRefCols(boolean value) { getSqlExtRefCols().set(value); }
    public void keepAreaRef(boolean value) { getKeepAreaRef().set(value); }
    public void createTidCol(boolean value) { getCreateTidCol().set(value); }
    public void createBasketCol(boolean value) { getCreateBasketCol().set(value); }
    public void createDatasetCol(boolean value) { getCreateDatasetCol().set(value); }
    public void translation(String value) { getTranslation().set(value); }
    public void createMetaInfo(boolean value) { getCreateMetaInfo().set(value); }

    protected Ili2dbSchemaImportOptions schemaOptions() {
        return new Ili2dbSchemaImportOptions(
                resolveReference(iliFile),
                pathString(getIliMetaAttrs()),
                b(getOneGeomPerTable()),
                b(getSetupPgExt()),
                pathString(getDropscript()),
                pathString(getCreatescript()),
                resolveMetaConfig(),
                getDefaultSrsAuth().getOrNull(),
                getDefaultSrsCode().getOrNull(),
                b(getCreateSingleEnumTab()),
                b(getCreateEnumTabs()),
                b(getCreateEnumTxtCol()),
                b(getCreateEnumColAsItfCode()),
                b(getCreateEnumTabsWithId()),
                b(getCreateImportTabs()),
                b(getBeautifyEnumDispName()),
                b(getNoSmartMapping()),
                b(getSmart1Inheritance()),
                b(getSmart2Inheritance()),
                b(getCoalesceCatalogueRef()),
                b(getCoalesceMultiSurface()),
                b(getCoalesceMultiLine()),
                b(getExpandMultilingual()),
                b(getExpandStruct()),
                b(getCoalesceJson()),
                b(getCoalesceArray()),
                b(getCreateTypeConstraint()),
                b(getCreateFk()),
                b(getCreateFkIdx()),
                b(getCreateUnique()),
                b(getCreateNumChecks()),
                b(getCreateTextChecks()),
                b(getCreateDateTimeChecks()),
                b(getCreateStdCols()),
                getT_id_Name().getOrNull(),
                getIdSeqMin().getOrNull(),
                getIdSeqMax().getOrNull(),
                b(getCreateTypeDiscriminator()),
                b(getCreateGeomIdx()),
                b(getDisableNameOptimization()),
                b(getNameByTopic()),
                getMaxNameLength().getOrNull(),
                b(getSqlEnableNull()),
                b(getSqlColsAsText()),
                b(getSqlExtRefCols()),
                b(getKeepAreaRef()),
                b(getCreateTidCol()),
                b(getCreateBasketCol()),
                b(getCreateDatasetCol()),
                getTranslation().getOrNull(),
                b(getCreateMetaInfo())
        );
    }

    protected static boolean b(Property<Boolean> property) {
        return property.getOrElse(false);
    }

    private String resolveReference(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text && isRepositoryReference(text)) {
            return text;
        }
        return getProject().file(value).getPath();
    }

    private String resolveMetaConfig() {
        String value = getMetaConfig().getOrNull();
        if (value == null) {
            return null;
        }
        return value.startsWith("ilidata:") ? value : getProject().file(value).getAbsolutePath();
    }

    private static boolean isRepositoryReference(String value) {
        return value.startsWith("ilidata:") || GenericFileFilter.getFileExtension(value) == null;
    }

    private static String pathString(RegularFileProperty property) {
        return property.isPresent() ? property.get().getAsFile().getPath() : null;
    }
}
