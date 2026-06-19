package ch.so.agi.gretl.tasks;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ili2db.Ili2dbConfigBuilder;
import ch.so.agi.gretl.internal.ili2db.Ili2dbDatasetResolver;
import ch.so.agi.gretl.internal.ili2db.Ili2dbFlavor;
import ch.so.agi.gretl.internal.ili2db.Ili2dbGpkgImportOptions;
import ch.so.agi.gretl.internal.ili2db.Ili2dbOperation;
import ch.so.agi.gretl.internal.ili2db.Ili2dbRequest;
import ch.so.agi.gretl.internal.ili2db.Ili2dbTransfer;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "Ili2gpkgImport", description = "Imports INTERLIS transfer files into a GeoPackage.")
public abstract class Ili2gpkgImport extends Ili2dbFileDataTask {

    @Input @Optional public abstract Property<Boolean> getCoalesceJson();
    @Input @Optional public abstract Property<Boolean> getNameByTopic();
    @Input @Optional public abstract Property<String> getDefaultSrsCode();
    @Input @Optional public abstract Property<Boolean> getCreateEnumTabs();
    @Input @Optional public abstract Property<Boolean> getCreateMetaInfo();
    @Input @Optional public abstract Property<Boolean> getCreateGeomIdx();

    public void coalesceJson(boolean value) { getCoalesceJson().set(value); }
    public void nameByTopic(boolean value) { getNameByTopic().set(value); }
    public void defaultSrsCode(String value) { getDefaultSrsCode().set(value); }
    public void createEnumTabs(boolean value) { getCreateEnumTabs().set(value); }
    public void createMetaInfo(boolean value) { getCreateMetaInfo().set(value); }
    public void createGeomIdx(boolean value) { getCreateGeomIdx().set(value); }

    @TaskAction
    public void importData() {
        Ili2dbDatasetResolver.ResolvedDataFiles files = resolvedDataFiles();
        if (files.files().isEmpty()) {
            return;
        }
        List<String> datasets = datasets();
        List<Ili2dbTransfer> transfers = Ili2dbDatasetResolver.pairFilesAndDatasets(files.files(), datasets);
        Config config = config(Ili2dbFlavor.GEOPACKAGE, Ili2dbOperation.IMPORT);
        Ili2dbConfigBuilder.applyGpkgImport(config, new Ili2dbGpkgImportOptions(
                getCoalesceJson().getOrElse(false),
                getNameByTopic().getOrElse(false),
                getDefaultSrsCode().getOrNull(),
                getCreateEnumTabs().getOrElse(false),
                getCreateMetaInfo().getOrElse(false),
                getCreateGeomIdx().getOrElse(false)
        ), !datasets.isEmpty());
        execute(new Ili2dbRequest(Ili2dbFlavor.GEOPACKAGE, Ili2dbOperation.IMPORT,
                null, dbFilePath(), config, transfers, List.of(), logFilePath(), failOnException()));
    }
}
