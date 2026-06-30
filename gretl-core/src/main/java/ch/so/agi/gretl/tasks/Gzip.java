package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.internal.gzip.GzipEngine;
import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
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

@GretlTaskDoc(name = "Gzip", description = "Writes a GZIP-compressed copy of a file.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Schreibt eine GZIP-komprimierte Kopie einer Datei.") })
public abstract class Gzip extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Gzip.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDataFile();

    @OutputFile
    public abstract RegularFileProperty getGzipFile();

    @GretlDslMethod(required = true, description = "Specifies the input file to compress.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die zu komprimierende Eingabedatei fest.") })
    public void dataFile(Object path) {
        setRegularFile(getDataFile(), path);
    }

    @GretlDslMethod(required = true, description = "Specifies the GZIP output file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die GZIP-Ausgabedatei fest.") })
    public void gzipFile(Object path) {
        setRegularFile(getGzipFile(), path);
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
