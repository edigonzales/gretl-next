package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
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

@GretlTaskDoc(name = "Av2ch", description = "Converts Swiss cadastral ITF files to the federal AV model.")
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

    @GretlDslMethod(required = true, description = "Adds ITF files to convert.")
    public void inputFiles(Object... paths) {
        getInputFiles().from(paths);
    }

    @GretlDslMethod(description = "Alias for adding ITF files to convert.")
    public void inputFile(Object... paths) {
        inputFiles(paths);
    }

    public void setInputFile(Object paths) {
        getInputFiles().setFrom(paths);
    }

    @GretlDslMethod(required = true, description = "Sets the output directory.")
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
