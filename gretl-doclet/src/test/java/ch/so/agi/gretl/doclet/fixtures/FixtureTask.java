package ch.so.agi.gretl.doclet.fixtures;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "FixtureTask", description = "Fixture description with {@code code}.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Fixture-Beschreibung mit {@code Code}.") })
public abstract class FixtureTask extends DefaultTask {
    @Internal
    public abstract Property<String> getSecret();

    @Input
    public abstract Property<String> getIgnoredInput();

    @GretlDslMethod(required = true, description = "Configures {@code input} path.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den {@code input}-Pfad.") })
    public void inputFile(Object path) {
    }

    @GretlDslMethod(defaultValue = "7", description = "Sets count.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Setzt den Zaehlwert.") })
    public void count(int value) {
    }

    /**
     * Uses javadoc {@code fallback}.
     */
    @GretlDslMethod
    public void fallback(String value) {
    }

    @GretlDslMethod(required = true, description = "Sets labels.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Setzt die Beschriftungen.") })
    public void labels(String... values) {
    }

    @TaskAction
    public void run() {
    }
}
