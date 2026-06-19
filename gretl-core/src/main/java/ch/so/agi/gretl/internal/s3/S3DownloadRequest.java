package ch.so.agi.gretl.internal.s3;

import java.nio.file.Path;

public record S3DownloadRequest(
        S3ConnectionSpec connection,
        String key,
        Path downloadDir
) {
}
