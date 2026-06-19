package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.internal.s3.S3ConnectionSpec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

abstract class S3Task extends AbstractCoreGretlTask {
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String endpoint = "https://s3.eu-central-1.amazonaws.com";
    private String region = "eu-central-1";

    @Internal
    public String getAccessKey() {
        return accessKey;
    }

    @Internal
    public String getSecretKey() {
        return secretKey;
    }

    @Input
    public String getBucketName() {
        return bucketName;
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

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @GretlDslMethod(required = true, description = "Configures the S3 access key.")
    public void accessKey(String accessKey) {
        setAccessKey(accessKey);
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @GretlDslMethod(required = true, description = "Configures the S3 secret key.")
    public void secretKey(String secretKey) {
        setSecretKey(secretKey);
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @GretlDslMethod(required = true, description = "Configures the S3 bucket name.")
    public void bucketName(String bucketName) {
        setBucketName(bucketName);
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setEndPoint(String endpoint) {
        setEndpoint(endpoint);
    }

    @GretlDslMethod(description = "Configures the S3 endpoint.")
    public void endpoint(String endpoint) {
        setEndpoint(endpoint);
    }

    public void endPoint(String endpoint) {
        setEndpoint(endpoint);
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @GretlDslMethod(description = "Configures the S3 region.")
    public void region(String region) {
        setRegion(region);
    }

    protected S3ConnectionSpec connectionSpec() {
        return new S3ConnectionSpec(accessKey, secretKey, bucketName, endpoint, region);
    }
}
