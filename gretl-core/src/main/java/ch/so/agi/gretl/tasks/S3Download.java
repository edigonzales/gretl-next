package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.s3.S3DownloadRequest;
import ch.so.agi.gretl.internal.s3.S3Engine;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "S3Download", description = "Downloads one object or a whole bucket from S3.")
public abstract class S3Download extends S3Task {
    private final GretlLogger log = LogEnvironment.getLogger(S3Download.class);
    private String key;

    @Input
    @Optional
    public String getKey() {
        return key;
    }

    @OutputDirectory
    public abstract DirectoryProperty getDownloadDir();

    public void setKey(String key) {
        this.key = key;
    }

    @GretlDslMethod(description = "Configures the S3 object key. If omitted, all objects are downloaded.")
    public void key(String key) {
        setKey(key);
    }

    private void configureDownloadDir(Object downloadDir) {
        setDirectory(getDownloadDir(), downloadDir);
    }

    @GretlDslMethod(required = true, description = "Configures the local download directory.")
    public void downloadDir(Object downloadDir) {
        configureDownloadDir(downloadDir);
    }

    @TaskAction
    public void download() {
        try {
            new S3Engine().download(new S3DownloadRequest(
                    connectionSpec(),
                    key,
                    getDownloadDir().get().getAsFile().toPath()));
        } catch (Exception e) {
            log.error("Exception in S3Download task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
