package ch.so.agi.gretl.internal.ftp;

import java.util.List;

public record FtpDeleteRequest(
        FtpConnectionSpec connection,
        String remoteDir,
        List<String> remoteFiles
) {
}
