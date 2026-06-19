package ch.so.agi.gretl.internal.s3;

public record S3ConnectionSpec(
        String accessKey,
        String secretKey,
        String bucketName,
        String endpoint,
        String region
) {
}
