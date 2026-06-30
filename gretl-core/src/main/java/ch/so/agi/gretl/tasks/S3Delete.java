package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.s3.S3DeleteRequest;
import ch.so.agi.gretl.internal.s3.S3Engine;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

@GretlTaskDoc(name = "S3Delete", description = "Deletes one object or all objects from an S3 bucket.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Löscht ein Objekt oder alle Objekte aus einem S3-Bucket.") })
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

    @GretlDslMethod(description = "Configures the S3 object key. If omitted, all objects are deleted.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den S3-Objekt-Schlüssel. Wenn nicht angegeben, werden alle Objekte gelöscht.") })
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
