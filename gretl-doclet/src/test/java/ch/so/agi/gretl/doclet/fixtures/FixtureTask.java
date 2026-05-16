package ch.so.agi.gretl.doclet.fixtures;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "FixtureTask", description = "Fixture description with {@code code}.")
public abstract class FixtureTask extends DefaultTask {
    @Internal
    public abstract Property<String> getSecret();

    @Input
    public abstract Property<String> getIgnoredInput();

    @GretlDslMethod(required = true, description = "Configures {@code input} path.")
    public void inputFile(Object path) {
    }

    @GretlDslMethod(defaultValue = "7", description = "Sets count.")
    public void count(int value) {
    }

    /**
     * Uses javadoc {@code fallback}.
     */
    @GretlDslMethod
    public void fallback(String value) {
    }

    @GretlDslMethod(required = true, description = "Sets labels.")
    public void labels(String... values) {
    }

    @TaskAction
    public void run() {
    }
}
