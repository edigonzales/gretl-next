package ch.so.agi.gretl.test.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class GradleTestProject {
    private final Path directory;

    private GradleTestProject(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public static GradleTestProject create(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create Gradle test project: " + directory, e);
        }
        return new GradleTestProject(directory);
    }

    public GradleTestProject settingsGroovy(String content) {
        return file("settings.gradle", content);
    }

    public GradleTestProject buildGroovy(String content) {
        return file("build.gradle", content);
    }

    public GradleTestProject file(String path, String content) {
        Path target = directory.resolve(path).normalize();
        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("Project file escapes project directory: " + path);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write Gradle fixture " + target, e);
        }
        return this;
    }

    public GradleTestProject textFile(String relativePath, String content) {
        return file(relativePath, content);
    }

    public GradleTestProject binaryFile(String relativePath, byte[] content) {
        Path target = directory.resolve(relativePath).normalize();
        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("Project file escapes project directory: " + relativePath);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write Gradle fixture " + target, e);
        }
        return this;
    }

    public void assertContainsOnlyExpectedTopLevelFiles(Set<String> expected) {
        try (var paths = Files.list(directory)) {
            Set<String> actual = paths.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
            if (!actual.equals(expected)) {
                throw new AssertionError("Unexpected top-level consumer files. Expected " + expected + ", got " + actual);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot inspect Gradle fixture " + directory, e);
        }
    }

    public void assertContainsNoSourceClasspathReferences() {
        try (var paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String content = Files.readString(path, StandardCharsets.UTF_8);
                    for (String forbidden : new String[] {"includeBuild", "mavenLocal", "flatDir", "withPluginClasspath",
                            "gretl-core/build", "gretl-geotools/build", "build/classes", "build/resources"}) {
                        if (content.contains(forbidden)) {
                            throw new AssertionError("Consumer fixture contains forbidden reference " + forbidden
                                    + " in " + path);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot inspect Gradle fixture " + path, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Cannot inspect Gradle fixture " + directory, e);
        }
    }

    public Path path(String relativePath) {
        return directory.resolve(relativePath).normalize();
    }

    public Path directory() {
        return directory;
    }
}
