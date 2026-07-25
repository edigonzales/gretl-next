package ch.so.agi.gretl.doclet.fixtures;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import org.gradle.api.DefaultTask;

abstract class InheritedFixtureBaseTask extends DefaultTask {
    @GretlDslMethod(required = true, description = "Configures an inherited input.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert eine geerbte Eingabe.") })
    public void inheritedInput(String value) {
    }

    @GretlDslMethod(description = "Configures one inherited option.")
    public void inheritedOption(String value) {
    }

    @GretlDslMethod(description = "Configures two inherited options.")
    public void inheritedOption(String value, int count) {
    }

    @GretlDslMethod(description = "Configures a method overridden by the derived task.")
    public void overridden(String value) {
    }

    @GretlDslMethod(description = "Configures a method hidden by the derived task.")
    public void hidden(String value) {
    }

    public void internalHelper(String value) {
    }
}

@GretlTaskDoc(name = "InheritedFixtureTask", description = "Fixture task with inherited DSL methods.")
public abstract class InheritedFixtureTask extends InheritedFixtureBaseTask {
    @GretlDslMethod(description = "Configures a local method.")
    public void localMethod(String value) {
    }

    @Override
    @GretlDslMethod(description = "Configures the derived override.")
    public void overridden(String value) {
    }

    @Override
    public void hidden(String value) {
    }
}
