package ch.so.agi.gretl.geotools.internal;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class EmbeddedWorkerClasspath {

    private static final String RESOURCE_ROOT = "gretl-geotools-worker-classpath";

    private EmbeddedWorkerClasspath() {
    }

    public static Set<File> resolve(Project project) {
        URL root = EmbeddedWorkerClasspath.class.getClassLoader().getResource(RESOURCE_ROOT);
        if (root == null) {
            throw new IllegalStateException("Embedded GeoTools worker classpath not found.");
        }

        try {
            if ("file".equals(root.getProtocol())) {
                return collectJarFiles(Path.of(root.toURI()));
            }
            if ("jar".equals(root.getProtocol())) {
                return extractFromJar(project, root);
            }
            throw new IllegalStateException("Unsupported worker classpath resource protocol: " + root);
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve embedded GeoTools worker classpath.", e);
        }
    }

    private static Set<File> collectJarFiles(Path directory) throws IOException {
        Set<File> files = new LinkedHashSet<>();
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .map(Path::toFile)
                    .forEach(files::add);
        }
        return files;
    }

    private static Set<File> extractFromJar(Project project, URL root) throws IOException {
        JarURLConnection connection = (JarURLConnection) root.openConnection();
        Path outputDir = project.getLayout().getBuildDirectory()
                .dir("gretl-geotools-worker-classpath")
                .get()
                .getAsFile()
                .toPath();
        Files.createDirectories(outputDir);

        Set<File> files = new LinkedHashSet<>();
        try (JarFile jar = connection.getJarFile()) {
            for (JarEntry entry : java.util.Collections.list(jar.entries())) {
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(RESOURCE_ROOT + "/") || !name.endsWith(".jar")) {
                    continue;
                }

                Path target = outputDir.resolve(name.substring(RESOURCE_ROOT.length() + 1));
                Files.createDirectories(target.getParent());
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                files.add(target.toFile());
            }
        }
        return files;
    }
}
