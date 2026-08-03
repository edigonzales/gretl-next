package ch.so.agi.gretl.internal.ftp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FtpEngineTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsUnsafeRemoteFilesBeforeConnecting() {
        FtpConnectionSpec unreachable = new FtpConnectionSpec(
                "127.0.0.1:1", "user", "password", null, null, true, 0);
        FtpEngine engine = new FtpEngine();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> engine.download(new FtpDownloadRequest(
                                unreachable, temporaryDirectory, "/remote", List.of("../outside.txt"), FtpFileType.BINARY))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> engine.download(new FtpDownloadRequest(
                                unreachable, temporaryDirectory, "/remote",
                                List.of(temporaryDirectory.resolve("absolute.txt").toString()), FtpFileType.BINARY)))
        );
    }
}
