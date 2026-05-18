package ch.so.agi.gretl.control.manifest;

public class ManifestException extends RuntimeException {
    public ManifestException(String message) {
        super(message);
    }

    public ManifestException(String message, Throwable cause) {
        super(message, cause);
    }
}
