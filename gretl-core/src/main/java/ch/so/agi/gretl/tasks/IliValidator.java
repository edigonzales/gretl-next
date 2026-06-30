package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.ValidatorExecutionSupport;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "IliValidator", description = "Validates INTERLIS transfer files with ilivalidator.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Validiert INTERLIS-Transferdateien mit ilivalidator.") })
public abstract class IliValidator extends AbstractInterlisValidatorTask {

    @TaskAction
    public void validateTask() {
        boolean validationOk = new ValidatorExecutionSupport().validate(this);
        setValidationOk(validationOk);
        if (!validationOk && getFailOnError().get()) {
            throw new GradleException("validation failed");
        }
    }
}
