package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.LocaleText;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public abstract class AbstractIli2DbExportTask extends AbstractIli2DbFileTask {

    private final Property<Boolean> export3;
    private final ListProperty<String> exportModels;

    @Inject
    public AbstractIli2DbExportTask() {
        ObjectFactory objects = getProject().getObjects();
        this.export3 = objects.property(Boolean.class);
        this.exportModels = objects.listProperty(String.class);
        getExport3().convention(false);
        getExportModels().convention(Collections.emptyList());
    }

    @Input
    public Property<Boolean> getExport3() {
        return export3;
    }

    @Input
    @Optional
    public ListProperty<String> getExportModels() {
        return exportModels;
    }

    @GretlDslMethod(required = true, description = "Specifies INTERLIS transfer output files.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die INTERLIS-Transfer-Ausgabedateien an.") })
    public void dataFiles(Object... paths) {
        getTransferFilesCollection().from(paths);
    }

    public void export3(boolean value) {
        getExport3().set(value);
    }

    @GretlDslMethod(description = "Limits the export to the specified INTERLIS models.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Beschränkt den Export auf die angegebenen INTERLIS-Modelle.") })
    public void exportModels(String... names) {
        getExportModels().addAll(requireNonBlank("exportModels", names));
    }

    public void exportModels(String names) {
        getExportModels().set(List.of(names));
    }
}
