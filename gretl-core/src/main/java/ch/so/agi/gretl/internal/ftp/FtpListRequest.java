package ch.so.agi.gretl.internal.ftp;

public record FtpListRequest(
        FtpConnectionSpec connection,
        String remoteDir
) {
}
