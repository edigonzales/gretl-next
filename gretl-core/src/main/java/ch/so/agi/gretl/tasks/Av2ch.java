package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.av.Av2chEngine;
import ch.so.agi.gretl.internal.av.Av2chEngine.Av2chRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "Av2ch", description = "Converts Swiss cadastral ITF files to the federal AV model.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Konvertiert Schweizer AV-ITF-Dateien ins Bundes-AV-Modell.") })
public abstract class Av2ch extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Av2ch.class);
    private final ConfigurableFileCollection inputFiles;

    @Input
    @Optional
    public abstract Property<String> getModeldir();

    @Input
    public abstract Property<String> getLanguage();

    @Input
    public abstract Property<Boolean> getZip();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Inject
    public Av2ch() {
        this.inputFiles = getProject().files();
        getLanguage().convention("de");
        getZip().convention(false);
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getInputFiles() {
        return inputFiles;
    }

    @GretlDslMethod(required = true, description = "Specifies ITF files to convert.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die zu konvertierenden ITF-Dateien an.") })
    public void inputFiles(Object... paths) {
        getInputFiles().from(paths);
    }

    @GretlDslMethod(description = "Alias for specifying ITF files to convert.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Alias für die Angabe zu konvertierender ITF-Dateien.") })
    public void inputFile(Object... paths) {
        inputFiles(paths);
    }

    public void setInputFile(Object paths) {
        getInputFiles().setFrom(paths);
    }

    @GretlDslMethod(required = true, description = "Specifies the output directory.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt das Ausgabeverzeichnis fest.") })
    public void outputDirectory(Object path) {
        setDirectory(getOutputDirectory(), path);
    }

    public void modeldir(String value) { getModeldir().set(value); }
    public void language(String value) { getLanguage().set(value); }
    public void zip(boolean value) { getZip().set(value); }

    @TaskAction
    public void runTransformation() {
        try {
            new Av2chEngine().convert(new Av2chRequest(
                    getInputFiles().getFiles().stream().map(file -> file.toPath()).toList(),
                    getOutputDirectory().get().getAsFile().toPath(),
                    getModeldir().getOrNull(),
                    getLanguage().get(),
                    getZip().get()));
        } catch (Exception e) {
            log.error("failed to run Av2ch", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
