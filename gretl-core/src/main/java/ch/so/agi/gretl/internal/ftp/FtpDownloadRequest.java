package ch.so.agi.gretl.internal.ftp;

import java.nio.file.Path;
import java.util.List;

public record FtpDownloadRequest(
        FtpConnectionSpec connection,
        Path localDir,
        String remoteDir,
        List<String> remoteFiles,
        FtpFileType fileType
) {
}
