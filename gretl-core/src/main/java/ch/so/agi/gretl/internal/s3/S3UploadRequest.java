package ch.so.agi.gretl.internal.s3;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record S3UploadRequest(
        S3ConnectionSpec connection,
        List<Path> sourceFiles,
        String acl,
        String contentType,
        Map<String, String> metadata
) {
}
