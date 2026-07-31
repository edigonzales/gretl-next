package ch.so.agi.gretl.test.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class FixturePluginRepository {
    public static final String GROUP = "fixture";
    public static final String ARTIFACT = "additional-plugin";
    public static final String PLUGIN_ID = "fixture.additional";
    public static final String VERSION = "1.0";

    public static Path create(Path repository) {
        Path versionDirectory = repository.resolve(GROUP).resolve(ARTIFACT).resolve(VERSION);
        Path markerVersionDirectory = repository.resolve(PLUGIN_ID.replace('.', '/'))
                .resolve(PLUGIN_ID + ".gradle.plugin").resolve(VERSION);
        try {
            Files.createDirectories(versionDirectory);
            Files.createDirectories(markerVersionDirectory);
            writeJar(versionDirectory.resolve(ARTIFACT + "-" + VERSION + ".jar"));
            Files.writeString(versionDirectory.resolve(ARTIFACT + "-" + VERSION + ".pom"), implementationPom());
            Files.writeString(markerVersionDirectory.resolve(PLUGIN_ID + ".gradle.plugin-" + VERSION + ".pom"), markerPom());
            return repository;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create additional fixture plugin repository", e);
        }
    }

    private static void writeJar(Path jar) throws IOException {
        String className = AdditionalFixturePlugin.class.getName().replace('.', '/') + ".class";
        try (InputStream input = AdditionalFixturePlugin.class.getClassLoader().getResourceAsStream(className);
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            if (input == null) {
                throw new IOException("Fixture plugin class is not available: " + className);
            }
            output.putNextEntry(new JarEntry(className));
            input.transferTo(output);
            output.closeEntry();
            output.putNextEntry(new JarEntry("META-INF/gradle-plugins/" + PLUGIN_ID + ".properties"));
            output.write(("implementation-class=" + AdditionalFixturePlugin.class.getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    private static String implementationPom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>fixture</groupId><artifactId>additional-plugin</artifactId><version>1.0</version>
                </project>
                """;
    }

    private static String markerPom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>fixture.additional</groupId><artifactId>fixture.additional.gradle.plugin</artifactId><version>1.0</version>
                  <packaging>pom</packaging>
                  <dependencies><dependency><groupId>fixture</groupId><artifactId>additional-plugin</artifactId><version>1.0</version></dependency></dependencies>
                </project>
                """;
    }

    private FixturePluginRepository() {
    }
}
