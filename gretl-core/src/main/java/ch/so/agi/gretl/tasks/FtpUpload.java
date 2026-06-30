package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ftp.FtpEngine;
import ch.so.agi.gretl.internal.ftp.FtpFileType;
import ch.so.agi.gretl.internal.ftp.FtpUploadRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "FtpUpload", description = "Uploads one local file to an FTP server.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Lädt eine lokale Datei auf einen FTP-Server hoch.") })
public abstract class FtpUpload extends FtpTask {
    private final GretlLogger log = LogEnvironment.getLogger(FtpUpload.class);
    private String remoteDir;
    private String fileType = "ASCII";

    @InputFile
    public abstract RegularFileProperty getLocalFile();

    @Input
    public String getRemoteDir() {
        return remoteDir;
    }

    @Input
    @Optional
    public String getFileType() {
        return fileType;
    }

    private void configureLocalFile(Object localFile) {
        setRegularFile(getLocalFile(), localFile);
    }

    @GretlDslMethod(required = true, description = "Configures the local file to upload.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die hochzuladende lokale Datei fest.") })
    public void localFile(Object localFile) {
        configureLocalFile(localFile);
    }

    public void setRemoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
    }

    @GretlDslMethod(required = true, description = "Configures the target FTP directory.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt das FTP-Zielverzeichnis fest.") })
    public void remoteDir(String remoteDir) {
        setRemoteDir(remoteDir);
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    @GretlDslMethod(description = "Configures ASCII or BINARY file transfer mode.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den Dateitransfer-Modus (ASCII oder BINARY).") })
    public void fileType(String fileType) {
        setFileType(fileType);
    }

    @TaskAction
    public void upload() {
        try {
            new FtpEngine().upload(new FtpUploadRequest(
                    connectionSpec(),
                    getLocalFile().get().getAsFile().toPath(),
                    remoteDir,
                    FtpFileType.from(fileType)));
        } catch (Exception e) {
            log.error("Exception in FtpUpload task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
