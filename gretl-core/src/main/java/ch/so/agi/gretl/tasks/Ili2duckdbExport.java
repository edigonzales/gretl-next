package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.interlis.Ili2DbExecutionSupport;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "Ili2duckdbExport", description = "Exports INTERLIS transfer files from a DuckDB database with ili2duckdb.")
public abstract class Ili2duckdbExport extends AbstractIli2DbTransferTask {

    @Input
    public abstract Property<Boolean> getExport3();

    @Input
    @Optional
    public abstract ListProperty<String> getExportModels();

    @Inject
    public Ili2duckdbExport() {
        getExport3().convention(false);
    }

    @InputFile
    public RegularFileProperty getInputDatabase() {
        return getDatabaseFile();
    }

    @OutputFiles
    public ConfigurableFileCollection getTransferOutputs() {
        return getTransferFilesCollection();
    }

    @GretlDslMethod(description = "Limits export to the specified INTERLIS models.")
    public void exportModels(String... names) {
        getExportModels().addAll(requireNonBlank("exportModels", names));
    }

    @TaskAction
    public void exportData() {
        new Ili2DbExecutionSupport().executeExport(this);
    }
}
