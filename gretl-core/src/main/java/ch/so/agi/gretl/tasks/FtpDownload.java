package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ftp.FtpDownloadRequest;
import ch.so.agi.gretl.internal.ftp.FtpEngine;
import ch.so.agi.gretl.internal.ftp.FtpFileType;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@GretlTaskDoc(name = "FtpDownload", description = "Downloads files from an FTP server.")
public abstract class FtpDownload extends FtpTask {
    private final GretlLogger log = LogEnvironment.getLogger(FtpDownload.class);
    private String remoteDir;
    private Object remoteFile;
    private String fileType = "ASCII";

    @OutputDirectory
    public abstract DirectoryProperty getLocalDir();

    @Input
    public String getRemoteDir() {
        return remoteDir;
    }

    @Internal
    public Object getRemoteFile() {
        return remoteFile;
    }

    @Input
    @Optional
    public List<String> getRemoteFileNames() {
        return remoteFiles(remoteFile);
    }

    @Input
    @Optional
    public String getFileType() {
        return fileType;
    }

    private void configureLocalDir(Object localDir) {
        setDirectory(getLocalDir(), localDir);
    }

    @GretlDslMethod(required = true, description = "Configures the local download directory.")
    public void localDir(Object localDir) {
        configureLocalDir(localDir);
    }

    public void setRemoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
    }

    @GretlDslMethod(required = true, description = "Configures the remote FTP directory.")
    public void remoteDir(String remoteDir) {
        setRemoteDir(remoteDir);
    }

    public void setRemoteFile(Object remoteFile) {
        this.remoteFile = remoteFile;
    }

    @GretlDslMethod(description = "Configures one or more remote filenames or wildcard patterns.")
    public void remoteFile(Object remoteFile) {
        setRemoteFile(remoteFile);
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    @GretlDslMethod(description = "Configures ASCII or BINARY file transfer mode.")
    public void fileType(String fileType) {
        setFileType(fileType);
    }

    @TaskAction
    public void download() {
        try {
            new FtpEngine().download(new FtpDownloadRequest(
                    connectionSpec(),
                    getLocalDir().get().getAsFile().toPath(),
                    remoteDir,
                    remoteFiles(remoteFile),
                    FtpFileType.from(fileType)));
        } catch (Exception e) {
            log.error("Exception in FtpDownload task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    static List<String> remoteFiles(Object remoteFile) {
        if (remoteFile == null) {
            return List.of();
        }
        if (remoteFile instanceof String value) {
            return List.of(value);
        }
        if (remoteFile instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                if (item instanceof File file) {
                    values.add(file.getName());
                } else {
                    values.add(String.valueOf(item));
                }
            }
            return values;
        }
        return List.of(String.valueOf(remoteFile));
    }
}
