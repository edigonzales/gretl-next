package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.internal.gzip.GzipEngine;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

public abstract class Gzip extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Gzip.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDataFile();

    @OutputFile
    public abstract RegularFileProperty getGzipFile();

    public void dataFile(Object path) {
        getDataFile().set(getProject().file(path));
    }

    public void gzipFile(Object path) {
        getGzipFile().set(getProject().file(path));
    }

    @TaskAction
    public void run() {
        try {
            new GzipEngine().execute(
                    getDataFile().get().getAsFile().toPath(),
                    getGzipFile().get().getAsFile().toPath());
            log.lifecycle("Gzip file written: " + getGzipFile().get().getAsFile().getAbsolutePath());
        } catch (IOException e) {
            throw new GradleException("Could not gzip file: " + e.getMessage(), e);
        }
    }
}
