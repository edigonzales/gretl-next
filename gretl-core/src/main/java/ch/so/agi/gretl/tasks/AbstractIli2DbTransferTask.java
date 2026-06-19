package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.util.Collections;

public abstract class AbstractIli2DbTransferTask extends AbstractIli2DbTask {

    private final ConfigurableFileCollection transferFiles;
    private final ConfigurableFileCollection datasetNameFiles;

    @Input
    @Optional
    public abstract ListProperty<String> getRepositoryDataIds();

    @Input
    @Optional
    public abstract ListProperty<String> getDatasetNames();

    @Input
    @Optional
    public abstract Property<Boolean> getDatasetNamesFromTransferFiles();

    @Input
    @Optional
    public abstract Property<Integer> getDatasetNameSliceStart();

    @Input
    @Optional
    public abstract Property<Integer> getDatasetNameSliceEndExclusive();

    @Inject
    public AbstractIli2DbTransferTask() {
        this.transferFiles = getProject().files();
        this.datasetNameFiles = getProject().files();
        getRepositoryDataIds().convention(Collections.emptyList());
        getDatasetNames().convention(Collections.emptyList());
        getDatasetNamesFromTransferFiles().convention(false);
        getDatasetNameSliceEndExclusive().convention(-1);
    }

    @Internal
    public ConfigurableFileCollection getTransferFilesCollection() {
        return transferFiles;
    }

    @Internal
    public ConfigurableFileCollection getDatasetNameFilesCollection() {
        return datasetNameFiles;
    }

    @GretlDslMethod(required = true, description = "Adds local transfer files.")
    public void transferFiles(Object... paths) {
        getTransferFilesCollection().from(paths);
    }

    @GretlDslMethod(description = "Adds ilidata repository ids for import.")
    public void repositoryDataIds(String... ids) {
        getRepositoryDataIds().addAll(requireNonBlank("repositoryDataIds", ids));
    }

    @GretlDslMethod(description = "Sets explicit dataset names.")
    public void datasetNames(String... names) {
        getDatasetNames().set(requireNonBlank("datasetNames", names));
    }

    @GretlDslMethod(description = "Derives dataset names from the configured local transfer files.")
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
