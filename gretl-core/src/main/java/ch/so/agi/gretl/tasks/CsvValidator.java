package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.interlis.ValidatorExecutionSupport;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "CsvValidator", description = "Validates one CSV file with ilivalidator and the CSV reader adapter.")
public abstract class CsvValidator extends AbstractInterlisValidatorTask {

    @Input
    public abstract Property<Boolean> getFirstLineIsHeader();

    @Input
    @Optional
    public abstract Property<String> getValueDelimiter();

    @Input
    @Optional
    public abstract Property<String> getValueSeparator();

    @Input
    @Optional
    public abstract Property<String> getEncoding();

    @Inject
    public CsvValidator() {
        getFirstLineIsHeader().convention(true);
    }

    @TaskAction
    public void validateTask() {
        boolean validationOk = new ValidatorExecutionSupport().validate(this);
        setValidationOk(validationOk);
        if (!validationOk && getFailOnError().get()) {
            throw new GradleException("validation failed");
        }
    }
}
