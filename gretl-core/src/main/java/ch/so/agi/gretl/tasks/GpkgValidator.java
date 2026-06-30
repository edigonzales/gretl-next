package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.interlis.ValidatorExecutionSupport;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "GpkgValidator", description = "Validates GeoPackage files with ilivalidator and the GeoPackage reader adapter.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Validiert GeoPackage-Dateien mit ilivalidator und dem GeoPackage-Reader-Adapter.") })
public abstract class GpkgValidator extends AbstractInterlisValidatorTask {

    @Input
    public abstract Property<String> getTableName();

    @GretlDslMethod(required = true, description = "Specifies the GeoPackage table name to validate.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt den Namen der zu validierenden GeoPackage-Tabelle fest.") })
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
