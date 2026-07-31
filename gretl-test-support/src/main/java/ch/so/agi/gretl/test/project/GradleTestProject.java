package ch.so.agi.gretl.test.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public GradleTestProject settingsKotlin(String content) {
        return file("settings.gradle.kts", content);
    }

    public GradleTestProject buildGroovy(String content) {
        return file("build.gradle", content);
    }

    public GradleTestProject buildKotlin(String content) {
        return file("build.gradle.kts", content);
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

    public Path path(String relativePath) {
        return directory.resolve(relativePath).normalize();
    }

    public Path directory() {
        return directory;
    }
}
