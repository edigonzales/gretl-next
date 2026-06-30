package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;

public abstract class AbstractIli2DbSchemaImportTask extends AbstractIli2DbTask {

    private final ConfigurableFileCollection iliFileInputs;
    private final RegularFileProperty iliMetaAttrsFile;
    private final Property<Boolean> oneGeomPerTable;
    private final Property<Boolean> setupPgExt;
    private final RegularFileProperty dropScript;
    private final RegularFileProperty createScript;
    private final Property<String> metaConfig;
    private final Property<String> defaultSrsAuth;
    private final Property<String> defaultSrsCode;
    private final Property<Boolean> createSingleEnumTab;
    private final Property<Boolean> createEnumTabs;
    private final Property<Boolean> createEnumTxtCol;
    private final Property<Boolean> createEnumColAsItfCode;
    private final Property<Boolean> createEnumTabsWithId;
    private final Property<Boolean> createImportTabs;
    private final Property<Boolean> beautifyEnumDispName;
    private final Property<Boolean> noSmartMapping;
    private final Property<Boolean> smart1Inheritance;
    private final Property<Boolean> smart2Inheritance;
    private final Property<Boolean> coalesceCatalogueRef;
    private final Property<Boolean> coalesceMultiSurface;
    private final Property<Boolean> coalesceMultiLine;
    private final Property<Boolean> expandMultilingual;
    private final Property<Boolean> expandStruct;
    private final Property<Boolean> coalesceJson;
    private final Property<Boolean> coalesceArray;
    private final Property<Boolean> createTypeConstraint;
    private final Property<Boolean> createFk;
    private final Property<Boolean> createFkIdx;
    private final Property<Boolean> createUnique;
    private final Property<Boolean> createNumChecks;
    private final Property<Boolean> createTextChecks;
    private final Property<Boolean> createDateTimeChecks;
    private final Property<Boolean> createStdCols;
    private final Property<String> tidColumnName;
    private final Property<Long> idSeqMin;
    private final Property<Long> idSeqMax;
    private final Property<Boolean> createTypeDiscriminator;
    private final Property<Boolean> createGeomIdx;
    private final Property<Boolean> disableNameOptimization;
    private final Property<Boolean> nameByTopic;
    private final Property<Integer> maxNameLength;
    private final Property<Boolean> sqlEnableNull;
    private final Property<Boolean> sqlColsAsText;
    private final Property<Boolean> sqlExtRefCols;
    private final Property<Boolean> keepAreaRef;
    private final Property<Boolean> createTidCol;
    private final Property<Boolean> createBasketCol;
    private final Property<Boolean> createDatasetCol;
    private final Property<String> translation;
    private final Property<Boolean> createMetaInfo;
    private Object iliFile;

    @Inject
    public AbstractIli2DbSchemaImportTask() {
        ObjectFactory objects = getProject().getObjects();
        this.iliFileInputs = getProject().files();
        this.iliMetaAttrsFile = objects.fileProperty();
        this.oneGeomPerTable = objects.property(Boolean.class);
        this.setupPgExt = objects.property(Boolean.class);
        this.dropScript = objects.fileProperty();
        this.createScript = objects.fileProperty();
        this.metaConfig = objects.property(String.class);
        this.defaultSrsAuth = objects.property(String.class);
        this.defaultSrsCode = objects.property(String.class);
        this.createSingleEnumTab = objects.property(Boolean.class);
        this.createEnumTabs = objects.property(Boolean.class);
        this.createEnumTxtCol = objects.property(Boolean.class);
        this.createEnumColAsItfCode = objects.property(Boolean.class);
        this.createEnumTabsWithId = objects.property(Boolean.class);
        this.createImportTabs = objects.property(Boolean.class);
        this.beautifyEnumDispName = objects.property(Boolean.class);
        this.noSmartMapping = objects.property(Boolean.class);
        this.smart1Inheritance = objects.property(Boolean.class);
        this.smart2Inheritance = objects.property(Boolean.class);
        this.coalesceCatalogueRef = objects.property(Boolean.class);
        this.coalesceMultiSurface = objects.property(Boolean.class);
        this.coalesceMultiLine = objects.property(Boolean.class);
        this.expandMultilingual = objects.property(Boolean.class);
        this.expandStruct = objects.property(Boolean.class);
        this.coalesceJson = objects.property(Boolean.class);
        this.coalesceArray = objects.property(Boolean.class);
        this.createTypeConstraint = objects.property(Boolean.class);
        this.createFk = objects.property(Boolean.class);
        this.createFkIdx = objects.property(Boolean.class);
        this.createUnique = objects.property(Boolean.class);
        this.createNumChecks = objects.property(Boolean.class);
        this.createTextChecks = objects.property(Boolean.class);
        this.createDateTimeChecks = objects.property(Boolean.class);
        this.createStdCols = objects.property(Boolean.class);
        this.tidColumnName = objects.property(String.class);
        this.idSeqMin = objects.property(Long.class);
        this.idSeqMax = objects.property(Long.class);
        this.createTypeDiscriminator = objects.property(Boolean.class);
        this.createGeomIdx = objects.property(Boolean.class);
        this.disableNameOptimization = objects.property(Boolean.class);
        this.nameByTopic = objects.property(Boolean.class);
        this.maxNameLength = objects.property(Integer.class);
        this.sqlEnableNull = objects.property(Boolean.class);
        this.sqlColsAsText = objects.property(Boolean.class);
        this.sqlExtRefCols = objects.property(Boolean.class);
        this.keepAreaRef = objects.property(Boolean.class);
        this.createTidCol = objects.property(Boolean.class);
        this.createBasketCol = objects.property(Boolean.class);
        this.createDatasetCol = objects.property(Boolean.class);
        this.translation = objects.property(String.class);
        this.createMetaInfo = objects.property(Boolean.class);
        getOneGeomPerTable().convention(false);
        getSetupPgExt().convention(false);
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
        getExpandStruct().convention(false);
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

    @Internal
    public Object getIliFileRaw() {
        return iliFile;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public RegularFileProperty getIliMetaAttrsFile() {
        return iliMetaAttrsFile;
    }

    @Input
    public Property<Boolean> getOneGeomPerTable() {
        return oneGeomPerTable;
    }

    @Input
    public Property<Boolean> getSetupPgExt() {
        return setupPgExt;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public RegularFileProperty getDropScript() {
        return dropScript;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public RegularFileProperty getCreateScript() {
        return createScript;
    }

    @Input
    @Optional
    public Property<String> getMetaConfig() {
        return metaConfig;
    }

    @Input
    @Optional
    public Property<String> getDefaultSrsAuth() {
        return defaultSrsAuth;
    }

    @Input
    @Optional
    public Property<String> getDefaultSrsCode() {
        return defaultSrsCode;
    }

    @Input
    public Property<Boolean> getCreateSingleEnumTab() {
        return createSingleEnumTab;
    }

    @Input
    public Property<Boolean> getCreateEnumTabs() {
        return createEnumTabs;
    }

    @Input
    public Property<Boolean> getCreateEnumTxtCol() {
        return createEnumTxtCol;
    }

    @Input
    public Property<Boolean> getCreateEnumColAsItfCode() {
        return createEnumColAsItfCode;
    }

    @Input
    public Property<Boolean> getCreateEnumTabsWithId() {
        return createEnumTabsWithId;
    }

    @Input
    public Property<Boolean> getCreateImportTabs() {
        return createImportTabs;
    }

    @Input
    public Property<Boolean> getBeautifyEnumDispName() {
        return beautifyEnumDispName;
    }

    @Input
    public Property<Boolean> getNoSmartMapping() {
        return noSmartMapping;
    }

    @Input
    public Property<Boolean> getSmart1Inheritance() {
        return smart1Inheritance;
    }

    @Input
    public Property<Boolean> getSmart2Inheritance() {
        return smart2Inheritance;
    }

    @Input
    public Property<Boolean> getCoalesceCatalogueRef() {
        return coalesceCatalogueRef;
    }

    @Input
    public Property<Boolean> getCoalesceMultiSurface() {
        return coalesceMultiSurface;
    }

    @Input
    public Property<Boolean> getCoalesceMultiLine() {
        return coalesceMultiLine;
    }

    @Input
    public Property<Boolean> getExpandMultilingual() {
        return expandMultilingual;
    }

    @Input
    public Property<Boolean> getExpandStruct() {
        return expandStruct;
    }

    @Input
    public Property<Boolean> getCoalesceJson() {
        return coalesceJson;
    }

    @Input
    public Property<Boolean> getCoalesceArray() {
        return coalesceArray;
    }

    @Input
    public Property<Boolean> getCreateTypeConstraint() {
        return createTypeConstraint;
    }

    @Input
    public Property<Boolean> getCreateFk() {
        return createFk;
    }

    @Input
    public Property<Boolean> getCreateFkIdx() {
        return createFkIdx;
    }

    @Input
    public Property<Boolean> getCreateUnique() {
        return createUnique;
    }

    @Input
    public Property<Boolean> getCreateNumChecks() {
        return createNumChecks;
    }

    @Input
    public Property<Boolean> getCreateTextChecks() {
        return createTextChecks;
    }

    @Input
    public Property<Boolean> getCreateDateTimeChecks() {
        return createDateTimeChecks;
    }

    @Input
    public Property<Boolean> getCreateStdCols() {
        return createStdCols;
    }

    @Input
    @Optional
    public Property<String> getTidColumnName() {
        return tidColumnName;
    }

    @Input
    @Optional
    public Property<Long> getIdSeqMin() {
        return idSeqMin;
    }

    @Input
    @Optional
    public Property<Long> getIdSeqMax() {
        return idSeqMax;
    }

    @Input
    public Property<Boolean> getCreateTypeDiscriminator() {
        return createTypeDiscriminator;
    }

    @Input
    public Property<Boolean> getCreateGeomIdx() {
        return createGeomIdx;
    }

    @Input
    public Property<Boolean> getDisableNameOptimization() {
        return disableNameOptimization;
    }

    @Input
    public Property<Boolean> getNameByTopic() {
        return nameByTopic;
    }

    @Input
    @Optional
    public Property<Integer> getMaxNameLength() {
        return maxNameLength;
    }

    @Input
    public Property<Boolean> getSqlEnableNull() {
        return sqlEnableNull;
    }

    @Input
    public Property<Boolean> getSqlColsAsText() {
        return sqlColsAsText;
    }

    @Input
    public Property<Boolean> getSqlExtRefCols() {
        return sqlExtRefCols;
    }

    @Input
    public Property<Boolean> getKeepAreaRef() {
        return keepAreaRef;
    }

    @Input
    public Property<Boolean> getCreateTidCol() {
        return createTidCol;
    }

    @Input
    public Property<Boolean> getCreateBasketCol() {
        return createBasketCol;
    }

    @Input
    public Property<Boolean> getCreateDatasetCol() {
        return createDatasetCol;
    }

    @Input
    @Optional
    public Property<String> getTranslation() {
        return translation;
    }

    @Input
    public Property<Boolean> getCreateMetaInfo() {
        return createMetaInfo;
    }

    @GretlDslMethod(description = "Uses a local INTERLIS model file or repository reference for schema import.")
    public void iliFile(Object path) {
        setIliFile(path);
    }

    public void setIliFile(Object path) {
        this.iliFile = path;
        iliFileInputs.setFrom();
        if (path != null && !(path instanceof String value && isRepositoryReference(value))) {
            iliFileInputs.from(path);
        }
    }

    @GretlDslMethod(description = "Uses a local iliMetaAttrs file for schema import.")
    public void iliMetaAttrsFile(Object path) {
        setRegularFile(getIliMetaAttrsFile(), path);
    }

    public void iliMetaAttrs(Object path) {
        iliMetaAttrsFile(path);
    }

    public void setIliMetaAttrs(Object path) {
        iliMetaAttrs(path);
    }

    public void dropscript(Object path) {
        setRegularFile(getDropScript(), path);
    }

    public void setDropscript(Object path) {
        dropscript(path);
    }

    public void createscript(Object path) {
        setRegularFile(getCreateScript(), path);
    }

    public void setCreatescript(Object path) {
        createscript(path);
    }

    public void metaConfig(String value) {
        getMetaConfig().set(value);
    }

    public void oneGeomPerTable(boolean value) { getOneGeomPerTable().set(value); }
    public void setupPgExt(boolean value) { getSetupPgExt().set(value); }
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
    public void t_id_Name(String value) { getTidColumnName().set(value); }
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

    private static boolean isRepositoryReference(String value) {
        return value.startsWith("ilidata:") || !value.contains(".");
    }
}
