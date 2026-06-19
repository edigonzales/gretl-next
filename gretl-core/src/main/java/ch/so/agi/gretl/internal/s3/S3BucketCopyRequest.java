package ch.so.agi.gretl.internal.s3;

import java.util.Map;

public record S3BucketCopyRequest(
        String accessKey,
        String secretKey,
        String sourceBucket,
        String targetBucket,
        String endpoint,
        String region,
        String acl,
        Map<String, String> metadata
) {
}
