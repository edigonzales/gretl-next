package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.ftp.FtpDeleteRequest;
import ch.so.agi.gretl.internal.ftp.FtpEngine;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

@GretlTaskDoc(name = "FtpDelete", description = "Deletes files from an FTP server.")
public abstract class FtpDelete extends FtpTask {
    private final GretlLogger log = LogEnvironment.getLogger(FtpDelete.class);
    private String remoteDir;
    private Object remoteFile;

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
        return FtpDownload.remoteFiles(remoteFile);
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

    @TaskAction
    public void delete() {
        try {
            new FtpEngine().delete(new FtpDeleteRequest(
                    connectionSpec(),
                    remoteDir,
                    FtpDownload.remoteFiles(remoteFile)));
        } catch (Exception e) {
            log.error("Exception in FtpDelete task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
