package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.internal.ili2db.Ili2dbDatasetResolver;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;

abstract class Ili2dbDataFileTask extends Ili2dbTask {
    private final ConfigurableFileCollection dataFileInputs;
    private Object dataFile;

    @Inject
    public Ili2dbDataFileTask() {
        this.dataFileInputs = getProject().files();
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getDataFileInputs() {
        return dataFileInputs;
    }

    @Input
    @Optional
    public List<String> getDataFileIdentifiers() {
        return resolvedDataFiles().identifiers();
    }

    @Internal
    protected Object getDataFileRaw() {
        return dataFile;
    }

    public void setDataFile(Object dataFile) {
        this.dataFile = dataFile;
        dataFileInputs.setFrom();
        if (!isIliDataReference(dataFile)) {
            dataFileInputs.from(dataFile);
        }
    }

    @GretlDslMethod(required = true, description = "Configures local transfer files or ilidata IDs.")
    public void dataFile(Object dataFile) {
        setDataFile(dataFile);
    }

    protected Ili2dbDatasetResolver.ResolvedDataFiles resolvedDataFiles() {
        return Ili2dbDatasetResolver.resolveDataFiles(getProject(), dataFile);
    }

    protected List<Path> localDataFiles() {
        return resolvedDataFiles().localFiles();
    }

    private static boolean isIliDataReference(Object value) {
        if (value instanceof String text) {
            return text.startsWith("ilidata:");
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (!(item instanceof String text) || !text.startsWith("ilidata:")) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
