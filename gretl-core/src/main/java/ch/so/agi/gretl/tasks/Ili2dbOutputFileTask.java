package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.internal.ili2db.Ili2dbDatasetResolver;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFiles;

import javax.inject.Inject;

abstract class Ili2dbOutputFileTask extends Ili2dbTask {
    private final ConfigurableFileCollection dataFileOutputs;
    private Object dataFile;

    @Inject
    public Ili2dbOutputFileTask() {
        this.dataFileOutputs = getProject().files();
    }

    @OutputFiles
    public ConfigurableFileCollection getDataFileOutputs() {
        return dataFileOutputs;
    }

    @Internal
    protected Object getDataFileRaw() {
        return dataFile;
    }

    public void setDataFile(Object dataFile) {
        this.dataFile = dataFile;
        dataFileOutputs.setFrom(dataFile);
    }

    @GretlDslMethod(required = true, description = "Configures the export transfer files.")
    public void dataFile(Object dataFile) {
        setDataFile(dataFile);
    }

    protected Ili2dbDatasetResolver.ResolvedOutputFiles resolvedOutputFiles() {
        return Ili2dbDatasetResolver.resolveOutputFiles(getProject(), dataFile);
    }
}
