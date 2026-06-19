package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.internal.s3.S3DeleteRequest;
import ch.so.agi.gretl.internal.s3.S3Engine;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "S3Delete", description = "Deletes one object or all objects from an S3 bucket.")
public abstract class S3Delete extends S3Task {
    private final GretlLogger log = LogEnvironment.getLogger(S3Delete.class);
    private String key;

    @Input
    @Optional
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @GretlDslMethod(description = "Configures the S3 object key. If omitted, all objects are deleted.")
    public void key(String key) {
        setKey(key);
    }

    @TaskAction
    public void delete() {
        try {
            new S3Engine().delete(new S3DeleteRequest(connectionSpec(), key));
        } catch (Exception e) {
            log.error("Exception in S3Delete task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
