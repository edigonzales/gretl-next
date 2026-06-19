package ch.so.agi.gretl.internal.ftp;

public record FtpConnectionSpec(
        String server,
        String user,
        String password,
        String systemType,
        String fileSeparator,
        boolean passiveMode,
        long controlKeepAliveTimeout
) {
}
