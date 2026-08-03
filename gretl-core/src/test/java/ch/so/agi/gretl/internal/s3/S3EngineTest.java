package ch.so.agi.gretl.internal.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class S3EngineTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsUnsafeExplicitKeysBeforeConnecting() {
        S3ConnectionSpec unreachable = new S3ConnectionSpec(
                "access", "secret", "bucket", "http://127.0.0.1:1", "eu-central-1");
        S3Engine engine = new S3Engine();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> engine.download(new S3DownloadRequest(
                                unreachable, "../outside.txt", temporaryDirectory))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> engine.download(new S3DownloadRequest(
                                unreachable, temporaryDirectory.resolve("absolute.txt").toString(), temporaryDirectory)))
        );
    }
}
