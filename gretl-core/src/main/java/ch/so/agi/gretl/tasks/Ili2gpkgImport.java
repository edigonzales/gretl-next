package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import ch.so.agi.gretl.internal.interlis.Ili2DbFlavor;
import ch.so.agi.gretl.internal.interlis.Ili2DbOperation;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "Ili2gpkgImport", description = "Imports INTERLIS transfer files into a GeoPackage.")
public abstract class Ili2gpkgImport extends AbstractIli2DbTransferTask {

    @Inject
    public Ili2gpkgImport() {
        getCoalesceJson().convention(false);
        getNameByTopic().convention(false);
        getCreateEnumTabs().convention(false);
        getCreateMetaInfo().convention(false);
        getCreateGeomIdx().convention(false);
    }

    @Input
    public abstract Property<Boolean> getCoalesceJson();

    @Input
    public abstract Property<Boolean> getNameByTopic();

    @Input
    @Optional
    public abstract Property<String> getDefaultSrsCode();

    @Input
    public abstract Property<Boolean> getCreateEnumTabs();

    @Input
    public abstract Property<Boolean> getCreateMetaInfo();

    @Input
    public abstract Property<Boolean> getCreateGeomIdx();

    @OutputFile
    public RegularFileProperty getDbfile() {
        return getDatabaseFile();
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getLocalTransferFiles() {
        return getTransferFilesCollection();
    }

    public void coalesceJson(boolean value) { getCoalesceJson().set(value); }
    public void nameByTopic(boolean value) { getNameByTopic().set(value); }
    public void defaultSrsCode(String value) { getDefaultSrsCode().set(value); }
    public void createEnumTabs(boolean value) { getCreateEnumTabs().set(value); }
    public void createMetaInfo(boolean value) { getCreateMetaInfo().set(value); }
    public void createGeomIdx(boolean value) { getCreateGeomIdx().set(value); }

    @TaskAction
    public void importData() {
        new Ili2DbExecutionSupport().executeImport(this, Ili2DbFlavor.GEOPACKAGE, Ili2DbOperation.IMPORT);
    }
}
