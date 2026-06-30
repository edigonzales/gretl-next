package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.LocaleText;
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

    @GretlDslMethod(required = true, description = "Specifies local transfer files.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die lokalen Transferdateien an.") })
    public void transferFiles(Object... paths) {
        getTransferFilesCollection().from(paths);
    }

    @GretlDslMethod(description = "Specifies ilidata repository ids for import.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die ilidata-Repository-IDs für den Import an.") })
    public void repositoryDataIds(String... ids) {
        getRepositoryDataIds().addAll(requireNonBlank("repositoryDataIds", ids));
    }

}
