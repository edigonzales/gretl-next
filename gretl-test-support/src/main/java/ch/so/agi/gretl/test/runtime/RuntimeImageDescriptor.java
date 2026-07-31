package ch.so.agi.gretl.test.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record RuntimeImageDescriptor(
        String imageId,
        String imageTag,
        String gretlVersion,
        String expectedGradleVersion,
        String expectedJavaMajorVersion,
        Path imageIdFile) {

    public static final String ID_FILE = "gretl.test.runtimeImage.idFile";
    public static final String TAG = "gretl.test.runtimeImage.tag";
    public static final String VERSION = "gretl.test.runtimeImage.version";
    public static final String GRADLE_VERSION = "gretl.test.runtimeImage.gradleVersion";
    public static final String JAVA_VERSION = "gretl.test.runtimeImage.javaVersion";

    public static RuntimeImageDescriptor fromSystemProperties() {
        Path idFile = Path.of(required(ID_FILE)).toAbsolutePath().normalize();
        String imageId;
        try {
            imageId = Files.readString(idFile).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read runtime image ID file '" + idFile + "'.", e);
        }
        return new RuntimeImageDescriptor(
                imageId,
                required(TAG),
                required(VERSION),
                required(GRADLE_VERSION),
                required(JAVA_VERSION),
                idFile);
    }

    public void verify() {
        if (imageId == null || !imageId.matches("sha256:[0-9a-fA-F]{64}")) {
            throw new IllegalStateException("Runtime image ID must be an immutable sha256 reference: " + imageId);
        }
        if (!Files.isRegularFile(imageIdFile)) {
            throw new IllegalStateException("Runtime image ID file does not exist: " + imageIdFile);
        }
        try {
            String fileImageId = Files.readString(imageIdFile).trim();
            if (!imageId.equals(fileImageId)) {
                throw new IllegalStateException("Runtime image ID does not match " + imageIdFile
                        + ": descriptor=" + imageId + ", file=" + fileImageId);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read runtime image ID file '" + imageIdFile + "'.", e);
        }
        if (imageTag == null || imageTag.isBlank() || gretlVersion == null || gretlVersion.isBlank()
                || expectedGradleVersion == null || expectedGradleVersion.isBlank()
                || expectedJavaMajorVersion == null || expectedJavaMajorVersion.isBlank()) {
            throw new IllegalStateException("Runtime image descriptor contains an empty required value.");
        }
    }

    private static String required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property '" + property + "'.");
        }
        return value.trim();
    }
}
