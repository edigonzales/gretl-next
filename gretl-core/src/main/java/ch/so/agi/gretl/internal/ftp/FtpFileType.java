package ch.so.agi.gretl.internal.ftp;

public enum FtpFileType {
    ASCII,
    BINARY;

    public static FtpFileType from(String value) {
        if (value == null || value.isBlank()) {
            return ASCII;
        }
        return FtpFileType.valueOf(value.trim().toUpperCase());
    }
}
