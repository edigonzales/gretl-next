package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.util.Collections;

public abstract class AbstractIli2DbTransferTask extends AbstractIli2DbFileTask {

    private final ListProperty<String> repositoryDataIds;

    @Inject
    public AbstractIli2DbTransferTask() {
        ObjectFactory objects = getProject().getObjects();
        this.repositoryDataIds = objects.listProperty(String.class);
        getRepositoryDataIds().convention(Collections.emptyList());
    }

    @Input
    @Optional
    public ListProperty<String> getRepositoryDataIds() {
        return repositoryDataIds;
    }

    @GretlDslMethod(required = true, description = "Adds local transfer files.")
    public void transferFiles(Object... paths) {
        getTransferFilesCollection().from(paths);
    }

    @GretlDslMethod(description = "Adds ilidata repository ids for import.")
    public void repositoryDataIds(String... ids) {
        getRepositoryDataIds().addAll(requireNonBlank("repositoryDataIds", ids));
    }

}
