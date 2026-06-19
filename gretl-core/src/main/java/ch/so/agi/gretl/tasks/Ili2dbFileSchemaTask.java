package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;

import java.nio.file.Path;

abstract class Ili2dbFileSchemaTask extends Ili2dbSchemaTask {
    @OutputFile
    public abstract RegularFileProperty getDbfile();

    @GretlDslMethod(required = true, description = "Configures the ili2db file database.")
    public void dbfile(Object dbfile) {
        getDbfile().set(getProject().file(dbfile));
    }

    protected Path dbFilePath() {
        if (!getDbfile().isPresent()) {
            throw new GradleException("dbfile is not configured");
        }
        return getDbfile().get().getAsFile().toPath();
    }
}
