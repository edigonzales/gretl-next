package ch.so.agi.gretl.internal.ftp;

import java.nio.file.Path;

public record FtpUploadRequest(
        FtpConnectionSpec connection,
        Path localFile,
        String remoteDir,
        FtpFileType fileType
) {
}
