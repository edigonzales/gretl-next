package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.s3.S3Engine;
import ch.so.agi.gretl.internal.s3.S3UploadRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@GretlTaskDoc(name = "S3Upload", description = "Uploads files to an S3 bucket.")
public abstract class S3Upload extends S3Task {
    private final GretlLogger log = LogEnvironment.getLogger(S3Upload.class);
    private final ConfigurableFileCollection sourceFiles;
    private String acl;
    private String contentType;
    private Map<String, String> metadata = new LinkedHashMap<>();

    @Inject
    public S3Upload() {
        this.sourceFiles = getProject().files();
    }

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDir();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSourceFile();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getSourceFiles() {
        return sourceFiles;
    }

    @Input
    public String getAcl() {
        return acl;
    }

    @Input
    @Optional
    public String getContentType() {
        return contentType;
    }

    @Input
    @Optional
    public Map<String, String> getMetadata() {
        return metadata;
    }

    private void configureSourceDir(Object sourceDir) {
        getSourceDir().set(getProject().file(sourceDir));
    }

    @GretlDslMethod(description = "Configures a directory whose direct files are uploaded.")
    public void sourceDir(Object sourceDir) {
        configureSourceDir(sourceDir);
    }

    private void configureSourceFile(Object sourceFile) {
        getSourceFile().set(getProject().file(sourceFile));
    }

    @GretlDslMethod(description = "Configures one file to upload.")
    public void sourceFile(Object sourceFile) {
        configureSourceFile(sourceFile);
    }

    private void configureSourceFiles(Object sourceFiles) {
        getSourceFiles().setFrom(sourceFiles);
    }

    @GretlDslMethod(description = "Configures a file collection to upload.")
    public void sourceFiles(Object sourceFiles) {
        configureSourceFiles(sourceFiles);
    }

    public void setAcl(String acl) {
        this.acl = acl;
    }

    @GretlDslMethod(required = true, description = "Configures the S3 canned ACL.")
    public void acl(String acl) {
        setAcl(acl);
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @GretlDslMethod(description = "Configures the uploaded content type.")
    public void contentType(String contentType) {
        setContentType(contentType);
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public void setMetaData(Map<String, String> metadata) {
        setMetadata(metadata);
    }

    @GretlDslMethod(description = "Configures S3 object metadata.")
    public void metadata(Map<String, String> metadata) {
        setMetadata(metadata);
    }

    public void metaData(Map<String, String> metadata) {
        setMetadata(metadata);
    }

    @TaskAction
    public void upload() {
        try {
            new S3Engine().upload(new S3UploadRequest(
                    connectionSpec(),
                    uploadFiles(),
                    acl,
                    contentType,
                    metadata));
        } catch (Exception e) {
            log.error("Exception in S3Upload task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    private List<Path> uploadFiles() {
        List<Path> files = new java.util.ArrayList<>();
        if (getSourceFile().isPresent()) {
            files.add(getSourceFile().get().getAsFile().toPath());
        }
        if (getSourceDir().isPresent()) {
            File[] children = getSourceDir().get().getAsFile().listFiles(File::isFile);
            if (children != null) {
                java.util.Arrays.stream(children)
                        .sorted(Comparator.comparing(File::getName))
                        .map(File::toPath)
                        .forEach(files::add);
            }
        }
        getSourceFiles().getFiles().stream()
                .filter(File::isFile)
                .sorted(Comparator.comparing(File::getName))
                .map(File::toPath)
                .forEach(files::add);
        return files;
    }
}
