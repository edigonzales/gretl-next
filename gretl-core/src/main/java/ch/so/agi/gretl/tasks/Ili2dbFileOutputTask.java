package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.nio.file.Path;

abstract class Ili2dbFileOutputTask extends Ili2dbOutputFileTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
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
