package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.interlis.ValidatorExecutionSupport;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "GpkgValidator", description = "Validates GeoPackage files with ilivalidator and the GeoPackage reader adapter.")
public abstract class GpkgValidator extends AbstractInterlisValidatorTask {

    @Input
    public abstract Property<String> getTableName();

    @GretlDslMethod(required = true, description = "Sets the GeoPackage table name to validate.")
    public void tableName(String value) {
        getTableName().set(value);
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
