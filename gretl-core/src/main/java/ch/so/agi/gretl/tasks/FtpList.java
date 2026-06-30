package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ftp.FtpEngine;
import ch.so.agi.gretl.internal.ftp.FtpListRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.List;

@GretlTaskDoc(name = "FtpList", description = "Lists files in an FTP directory.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Listet Dateien in einem FTP-Verzeichnis auf.") })
public abstract class FtpList extends FtpTask {
    private final GretlLogger log = LogEnvironment.getLogger(FtpList.class);
    private String remoteDir;
    private List<String> files = new ArrayList<>();

    @Input
    public String getRemoteDir() {
        return remoteDir;
    }

    @Internal
    public List<String> getFiles() {
        return files;
    }

    public void setRemoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
    }

    @GretlDslMethod(required = true, description = "Configures the remote FTP directory.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt das entfernte FTP-Verzeichnis fest.") })
    public void remoteDir(String remoteDir) {
        setRemoteDir(remoteDir);
    }

    @TaskAction
    public void list() {
        try {
            files = new FtpEngine().list(new FtpListRequest(connectionSpec(), remoteDir));
        } catch (Exception e) {
            log.error("Exception in FtpList task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
