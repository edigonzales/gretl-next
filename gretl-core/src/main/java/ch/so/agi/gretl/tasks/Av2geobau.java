package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.av.Av2geobauEngine;
import ch.so.agi.gretl.internal.av.Av2geobauEngine.Av2geobauRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "Av2geobau", description = "Converts cadastral ITF files to GeoBau DXF files.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Konvertiert AV-ITF-Dateien in GeoBau-DXF-Dateien.") })
public abstract class Av2geobau extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Av2geobau.class);
    private final ConfigurableFileCollection itfFiles;

    @Input
    @Optional
    public abstract Property<String> getModeldir();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getLogFile();

    @Input
    @Optional
    public abstract Property<String> getProxy();

    @Input
    @Optional
    public abstract Property<Integer> getProxyPort();

    @Input
    public abstract Property<Boolean> getZip();

    @OutputDirectory
    public abstract DirectoryProperty getDxfDirectory();

    @Inject
    public Av2geobau() {
        this.itfFiles = getProject().files();
        getZip().convention(false);
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getItfFiles() {
        return itfFiles;
    }

    @GretlDslMethod(required = true, description = "Specifies ITF files to convert.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt die zu konvertierenden ITF-Dateien an.") })
    public void itfFiles(Object... paths) {
        getItfFiles().from(paths);
    }

    public void setItfFiles(Object paths) {
        getItfFiles().setFrom(paths);
    }

    @GretlDslMethod(required = true, description = "Specifies the DXF output directory.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt das DXF-Ausgabeverzeichnis fest.") })
    public void dxfDirectory(Object path) {
        setDirectory(getDxfDirectory(), path);
    }

    @GretlDslMethod(description = "Specifies the optional conversion log file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die optionale Konvertierungs-Protokolldatei fest.") })
    public void logFile(Object path) {
        setRegularFile(getLogFile(), path);
    }

    public void modeldir(String value) { getModeldir().set(value); }
    public void proxy(String value) { getProxy().set(value); }
    public void proxyPort(int value) { getProxyPort().set(value); }
    public void zip(boolean value) { getZip().set(value); }

    @TaskAction
    public void runTransformation() {
        try {
            new Av2geobauEngine().convert(new Av2geobauRequest(
                    getItfFiles().getFiles().stream().map(file -> file.toPath()).toList(),
                    getDxfDirectory().get().getAsFile().toPath(),
                    getModeldir().getOrNull(),
                    getLogFile().isPresent() ? getLogFile().get().getAsFile().toPath() : null,
                    getProxy().getOrNull(),
                    getProxyPort().getOrNull(),
                    getZip().get()));
        } catch (Exception e) {
            log.error("failed to run Av2geobau", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
