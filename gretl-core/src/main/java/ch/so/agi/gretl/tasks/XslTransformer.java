package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.internal.xslt.XsltEngine;
import ch.so.agi.gretl.internal.xslt.XsltRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public abstract class XslTransformer extends AbstractCoreGretlTask {
    private final ConfigurableFileCollection xmlFiles;
    private final GretlLogger log;

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getXslFile();

    @Input
    @Optional
    public abstract Property<String> getXslResource();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getXmlFiles() {
        return xmlFiles;
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutDirectory();

    @Input
    public abstract Property<String> getFileExtension();

    @Inject
    public XslTransformer() {
        this.xmlFiles = getProject().files();
        this.log = LogEnvironment.getLogger(XslTransformer.class);
        getFileExtension().convention("xtf");
    }

    public void xslFile(Object path) {
        getXslFile().set(getProject().file(path));
    }

    public void xslResource(String resourceName) {
        getXslResource().set(resourceName);
    }

    public void xmlFiles(Object... paths) {
        getXmlFiles().from(paths);
    }

    public void outDirectory(Object path) {
        getOutDirectory().set(getProject().file(path));
    }

    public void fileExtension(String fileExtension) {
        getFileExtension().set(fileExtension);
    }

    @TaskAction
    public void transform() {
        try {
            new XsltEngine().execute(createRequest());
        } catch (Exception e) {
            log.error("Exception in XslTransformer task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    private XsltRequest createRequest() {
        boolean hasFile = getXslFile().isPresent();
        boolean hasResource = getXslResource().isPresent() && !getXslResource().get().isBlank();
        if (hasFile == hasResource) {
            throw new GradleException("Configure either xslFile or xslResource");
        }
        if (getXmlFiles().isEmpty()) {
            throw new GradleException("xmlFiles must not be empty");
        }

        List<Path> xmlFilePaths = getXmlFiles().getFiles().stream()
                .map(File::toPath)
                .toList();

        return new XsltRequest(
                getName(),
                hasFile ? getXslFile().get().getAsFile().toPath() : null,
                hasResource ? getXslResource().get() : null,
                xmlFilePaths,
                getOutDirectory().get().getAsFile().toPath(),
                getFileExtension().get()
        );
    }
}
