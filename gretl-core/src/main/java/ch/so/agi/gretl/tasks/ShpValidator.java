package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.ValidatorExecutionSupport;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "ShpValidator", description = "Validates one Shapefile with ilivalidator and the Shapefile reader adapter.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Validiert ein Shapefile mit ilivalidator und dem Shapefile-Reader-Adapter.") })
public abstract class ShpValidator extends AbstractInterlisValidatorTask {
    private final Property<String> encoding = getProject().getObjects().property(String.class);

    @Input
    @Optional
    public Property<String> getEncoding() {
        return encoding;
    }

    public void encoding(String value) { getEncoding().set(value); }

    @TaskAction
    public void validateTask() {
        boolean validationOk = new ValidatorExecutionSupport().validate(this);
        setValidationOk(validationOk);
        if (!validationOk && getFailOnError().get()) {
            throw new GradleException("validation failed");
        }
    }
}
