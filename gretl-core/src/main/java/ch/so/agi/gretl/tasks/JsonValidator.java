package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.ValidatorExecutionSupport;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "JsonValidator", description = "Validates JSON files with ilivalidator and the JSON reader adapter.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Validiert JSON-Dateien mit ilivalidator und dem JSON-Reader-Adapter.") })
public abstract class JsonValidator extends AbstractInterlisValidatorTask {

    @TaskAction
    public void validateTask() {
        boolean validationOk = new ValidatorExecutionSupport().validate(this);
        setValidationOk(validationOk);
        if (!validationOk && getFailOnError().get()) {
            throw new GradleException("validation failed");
        }
    }
}
