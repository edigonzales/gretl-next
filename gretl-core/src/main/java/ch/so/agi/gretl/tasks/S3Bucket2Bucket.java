package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.s3.S3BucketCopyRequest;
import ch.so.agi.gretl.internal.s3.S3Engine;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.LinkedHashMap;
import java.util.Map;

@GretlTaskDoc(name = "S3Bucket2Bucket", description = "Copies all objects from one S3 bucket to another.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Kopiert alle Objekte von einem S3-Bucket in einen anderen.") })
public abstract class S3Bucket2Bucket extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(S3Bucket2Bucket.class);

    private String accessKey;
    private String secretKey;
    private String sourceBucket;
    private String targetBucket;
    private String endpoint = "https://s3.eu-central-1.amazonaws.com";
    private String region = "eu-central-1";
    private String acl;
    private Map<String, String> metadata = new LinkedHashMap<>();

    @Internal
    public String getAccessKey() {
        return accessKey;
    }

    @Internal
    public String getSecretKey() {
        return secretKey;
    }

    @Input
    public String getSourceBucket() {
        return sourceBucket;
    }

    @Input
    public String getTargetBucket() {
        return targetBucket;
    }

    @Input
    @Optional
    public String getEndpoint() {
        return endpoint;
    }

    @Input
    @Optional
    public String getRegion() {
        return region;
    }

    @Input
    public String getAcl() {
        return acl;
    }

    @Input
    @Optional
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @GretlDslMethod(required = true, description = "Configures the S3 access key.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den S3-Zugriffsschlüssel.") })
    public void accessKey(String accessKey) {
        setAccessKey(accessKey);
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @GretlDslMethod(required = true, description = "Configures the S3 secret key.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den geheimen S3-Schlüssel.") })
    public void secretKey(String secretKey) {
        setSecretKey(secretKey);
    }

    public void setSourceBucket(String sourceBucket) {
        this.sourceBucket = sourceBucket;
    }

    @GretlDslMethod(required = true, description = "Configures the source S3 bucket.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den Quell-S3-Bucket.") })
    public void sourceBucket(String sourceBucket) {
        setSourceBucket(sourceBucket);
    }

    public void setTargetBucket(String targetBucket) {
        this.targetBucket = targetBucket;
    }

    @GretlDslMethod(required = true, description = "Configures the target S3 bucket.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den Ziel-S3-Bucket.") })
    public void targetBucket(String targetBucket) {
        setTargetBucket(targetBucket);
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setEndPoint(String endpoint) {
        setEndpoint(endpoint);
    }

    @GretlDslMethod(description = "Configures the S3 endpoint.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert den S3-Endpunkt.") })
    public void endpoint(String endpoint) {
        setEndpoint(endpoint);
    }

    public void endPoint(String endpoint) {
        setEndpoint(endpoint);
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @GretlDslMethod(description = "Configures the S3 region.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die S3-Region.") })
    public void region(String region) {
        setRegion(region);
    }

    public void setAcl(String acl) {
        this.acl = acl;
    }

    @GretlDslMethod(required = true, description = "Configures the S3 canned ACL.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die S3-Canned-ACL.") })
    public void acl(String acl) {
        setAcl(acl);
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public void setMetaData(Map<String, String> metadata) {
        setMetadata(metadata);
    }

    @GretlDslMethod(description = "Configures replacement metadata for copied objects.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert Ersatz-Metadaten für kopierte Objekte.") })
    public void metadata(Map<String, String> metadata) {
        setMetadata(metadata);
    }

    public void metaData(Map<String, String> metadata) {
        setMetadata(metadata);
    }

    @TaskAction
    public void copy() {
        try {
            new S3Engine().copyBucket(new S3BucketCopyRequest(
                    accessKey,
                    secretKey,
                    sourceBucket,
                    targetBucket,
                    endpoint,
                    region,
                    acl,
                    metadata));
        } catch (Exception e) {
            log.error("Exception in S3Bucket2Bucket task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
