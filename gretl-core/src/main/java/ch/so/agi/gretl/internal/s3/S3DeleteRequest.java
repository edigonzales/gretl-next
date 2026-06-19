package ch.so.agi.gretl.internal.s3;

public record S3DeleteRequest(
        S3ConnectionSpec connection,
        String key
) {
}
