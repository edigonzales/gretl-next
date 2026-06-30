package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;

public abstract class AbstractIli2DbFileTask extends AbstractIli2DbTask {

    private final ConfigurableFileCollection transferFiles;
    private final ConfigurableFileCollection datasetNameFiles;
    private final Property<Boolean> datasetNamesFromTransferFiles;
    private final Property<Integer> datasetNameSliceStart;
    private final Property<Integer> datasetNameSliceEndExclusive;

    @Inject
    public AbstractIli2DbFileTask() {
        ObjectFactory objects = getProject().getObjects();
        this.transferFiles = getProject().files();
        this.datasetNameFiles = getProject().files();
        this.datasetNamesFromTransferFiles = objects.property(Boolean.class);
        this.datasetNameSliceStart = objects.property(Integer.class);
        this.datasetNameSliceEndExclusive = objects.property(Integer.class);
        getDatasetNamesFromTransferFiles().convention(false);
        getDatasetNameSliceEndExclusive().convention(-1);
    }

    @Input
    @Optional
    public Property<Boolean> getDatasetNamesFromTransferFiles() {
        return datasetNamesFromTransferFiles;
    }

    @Input
    @Optional
    public Property<Integer> getDatasetNameSliceStart() {
        return datasetNameSliceStart;
    }

    @Input
    @Optional
    public Property<Integer> getDatasetNameSliceEndExclusive() {
        return datasetNameSliceEndExclusive;
    }

    @Internal
    public ConfigurableFileCollection getTransferFilesCollection() {
        return transferFiles;
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getDatasetNameFilesCollection() {
        return datasetNameFiles;
    }

    @GretlDslMethod(description = "Sets explicit dataset names.")
    public void datasetNames(String... names) {
        getDatasetNames().set(requireNonBlank("datasetNames", names));
    }

    @GretlDslMethod(description = "Derives dataset names from the configured transfer files.")
    public void datasetNamesFromTransferFiles() {
        getDatasetNamesFromTransferFiles().set(true);
    }

    @GretlDslMethod(description = "Derives dataset names from the provided files.")
    public void datasetNamesFromFiles(Object... paths) {
        getDatasetNameFilesCollection().from(paths);
    }

    @GretlDslMethod(description = "Uses a substring starting at start for derived dataset names.")
    public void datasetNameSlice(int start) {
        getDatasetNameSliceStart().set(start);
        getDatasetNameSliceEndExclusive().set(-1);
    }

    @GretlDslMethod(description = "Uses the substring [start, endExclusive) for derived dataset names.")
    public void datasetNameSlice(int start, int endExclusive) {
        if (endExclusive < start) {
            throw new IllegalArgumentException("datasetNameSlice endExclusive must be >= start");
        }
        getDatasetNameSliceStart().set(start);
        getDatasetNameSliceEndExclusive().set(endExclusive);
    }
}
