package ch.so.agi.gretl.testkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validated configuration for a TestKit build that combines the two GRETL
 * plugin artifacts explicitly.  The validation is intentionally strict: a
 * combined child build must never accidentally inherit the host test runtime.
 */
public record ExplicitPluginClasspathTestConfiguration(Path classpathFile, Path testKitDirectory) {

    public ExplicitPluginClasspathTestConfiguration {
        classpathFile = requireAbsolute(classpathFile, "classpathFile");
        testKitDirectory = requireAbsolute(testKitDirectory, "testKitDirectory");
    }

    public static ExplicitPluginClasspathTestConfiguration fromSystemProperties() {
        String classpath = required(GretlTestSystemProperties.EXPLICIT_PLUGIN_CLASSPATH);
        String testKit = required(GretlTestSystemProperties.TEST_KIT_DIRECTORY);
        return new ExplicitPluginClasspathTestConfiguration(Path.of(classpath), Path.of(testKit));
    }

    public List<File> readClasspath() {
        validate();
        try {
            return Files.readAllLines(classpathFile, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(Path::of)
                    .map(Path::toFile)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read combined plugin classpath file '"
                    + classpathFile + "'.", e);
        }
    }

    public void validate() {
        if (!Files.isRegularFile(classpathFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Combined plugin classpath file does not exist: " + classpathFile);
        }
        if (!testKitDirectory.isAbsolute()) {
            throw new IllegalStateException("Combined TestKit directory must be absolute: " + testKitDirectory);
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(classpathFile, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read combined plugin classpath file '"
                    + classpathFile + "'.", e);
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("Combined plugin classpath is empty: " + classpathFile);
        }

        Set<Path> canonicalEntries = new LinkedHashSet<>();
        boolean core = false;
        boolean geotools = false;
        for (String line : lines) {
            Path entry = Path.of(line);
            if (!entry.isAbsolute()) {
                throw new IllegalStateException("Combined plugin classpath entries must be absolute: " + line);
            }
            if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("Combined plugin classpath entry does not exist: " + entry);
            }
            Path canonical;
            try {
                canonical = entry.toRealPath();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot resolve combined plugin classpath entry: " + entry, e);
            }
            if (!canonicalEntries.add(canonical)) {
                throw new IllegalStateException("Combined plugin classpath contains a duplicate entry: " + canonical);
            }

            String name = entry.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
            String normalized = entry.toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
            if (name.contains("gretl-test-support") || name.contains("gretl-combined-tests")) {
                throw new IllegalStateException("Combined plugin classpath must not contain test artifacts: " + entry);
            }
            if (name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")) {
                throw new IllegalStateException("Combined plugin classpath must not contain source or Javadoc JARs: " + entry);
            }
            if (name.startsWith("gt-") || (name.contains("geotools") && !name.startsWith("gretl-geotools-"))) {
                throw new IllegalStateException("Raw GeoTools libraries must remain in the worker runtime, not the child plugin classpath: " + entry);
            }
            if (name.matches("gretl-core-.+\\.jar")) {
                core = true;
            }
            if (name.matches("gretl-geotools-.+\\.jar")) {
                geotools = true;
            }
            if (normalized.contains("/build/classes/") || normalized.contains("/build/resources/")) {
                throw new IllegalStateException("Combined plugin classpath must contain plugin artifacts, not build output directories: " + entry);
            }
        }
        if (!core) {
            throw new IllegalStateException("Combined plugin classpath does not contain a gretl-core plugin JAR: " + classpathFile);
        }
        if (!geotools) {
            throw new IllegalStateException("Combined plugin classpath does not contain a gretl-geotools plugin JAR: " + classpathFile);
        }
    }

    private static Path requireAbsolute(Path value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (!value.isAbsolute()) {
            throw new IllegalArgumentException(name + " must be absolute: " + value);
        }
        Path absolute = value.toAbsolutePath().normalize();
        return absolute;
    }

    private static String required(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property '" + propertyName
                    + "' for combined plugin TestKit execution.");
        }
        return value.trim();
    }
}
