package ch.so.agi.gretl.test.job;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

public final class DefaultTestJobMaterializer implements TestJobMaterializer {
    private final TestJobSettingsRenderer settingsRenderer;

    public DefaultTestJobMaterializer() {
        this(new DefaultTestJobSettingsRenderer());
    }

    public DefaultTestJobMaterializer(TestJobSettingsRenderer settingsRenderer) {
        this.settingsRenderer = settingsRenderer;
    }

    @Override
    public MaterializedTestJob materialize(TestJobDescriptor descriptor, TestJobBuildVariant build,
                                           TestJobExecutionTarget target, Path destinationRoot) {
        Path sourceRoot = descriptor.sourceDirectory().toAbsolutePath().normalize();
        Path destination = destinationRoot.toAbsolutePath().normalize()
                .resolve(descriptor.id()).resolve(build.id()).resolve(target.name().toLowerCase());
        if (destination.startsWith(sourceRoot)) throw new IllegalArgumentException("Materialized job destination must be outside catalog: " + destination);
        try {
            deleteRecursively(destination);
            Files.createDirectories(destination);
            copySelectedFiles(sourceRoot, destination, build.file());
            Path settings = destination.resolve("settings.gradle");
            java.util.Optional<java.net.URI> publishedRepository = java.util.Optional.empty();
            java.util.Optional<String> pluginVersion = java.util.Optional.empty();
            if (target == TestJobExecutionTarget.PUBLISHED_ARTIFACT) {
                var configuration = ch.so.agi.gretl.testkit.PublishedArtifactTestConfiguration.fromSystemProperties();
                publishedRepository = java.util.Optional.of(configuration.repositoryUri());
                pluginVersion = java.util.Optional.of(configuration.pluginVersion());
            }
            Files.writeString(settings, settingsRenderer.renderGroovy(new TestJobSettingsRequest(
                    descriptor.id(), target, publishedRepository, pluginVersion)), StandardCharsets.UTF_8);
            Path traceDirectory = destination.resolve(".gretl-test");
            Files.createDirectories(traceDirectory);
            Path trace = traceDirectory.resolve("task-trace.jsonl");
            Path bootstrap = traceDirectory.resolve("task-trace.init.gradle");
            Files.writeString(trace, "", StandardCharsets.UTF_8);
            Files.writeString(bootstrap, traceScript(), StandardCharsets.UTF_8);
            verifyBuildFileUnchanged(sourceRoot.resolve(build.file()), destination.resolve(build.file()));
            return new MaterializedTestJob(descriptor, build, target, destination,
                    destination.resolve(build.file()), settings, trace);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot materialize test job " + descriptor.id() + " at " + destination, e);
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new MaterializationException(e);
                }
            });
        } catch (MaterializationException e) {
            throw e.ioException;
        }
    }

    private void copySelectedFiles(Path sourceRoot, Path destination, String selectedBuild) throws IOException {
        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(path -> !path.equals(sourceRoot))
                    .filter(path -> !containsGeneratedDirectory(sourceRoot.relativize(path)))
                    .filter(path -> !path.getFileName().toString().equals("job.yaml"))
                    .filter(path -> !isBuildVariant(path.getFileName().toString()) || path.getFileName().toString().equals(selectedBuild))
                    .forEach(source -> {
                        Path relative = sourceRoot.relativize(source);
                        Path target = destination.resolve(relative);
                        try {
                            if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                                Files.createDirectories(target);
                            } else if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                                Files.createDirectories(target.getParent());
                                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                            }
                        } catch (IOException e) {
                            throw new MaterializationException(e);
                        }
                    });
        } catch (MaterializationException e) {
            throw e.ioException;
        }
    }

    private boolean isBuildVariant(String file) {
        return file.equals("build.gradle") || file.equals("build.gradle.kts");
    }

    private boolean containsGeneratedDirectory(Path relative) {
        for (Path part : relative) {
            if (part.toString().equals("build") || part.toString().equals(".gradle") || part.toString().equals(".git")) return true;
        }
        return false;
    }

    private void verifyBuildFileUnchanged(Path source, Path copy) throws IOException {
        if (!MessageDigest.isEqual(digest(source), digest(copy))) throw new IllegalStateException("Materialized build file differs from canonical source.");
    }

    private byte[] digest(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            input.transferTo(new java.io.OutputStream() {
                @Override public void write(int b) { digest.update((byte) b); }
                @Override public void write(byte[] b, int off, int len) { digest.update(b, off, len); }
            });
            return digest.digest();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String traceScript() {
        return """
                def traceFile = new File(System.getProperty('gretl.test.traceFile', '.gretl-test/task-trace.jsonl'))
                if (System.getProperty('gretl.test.traceEnabled', 'false').toBoolean()) {
                    traceFile.parentFile.mkdirs()
                    gradle.addListener(new TaskExecutionListener() {
                        void beforeExecute(Task task) { }
                        void afterExecute(Task task, TaskState state) {
                            def outcome = state.failure != null ? 'FAILED' : state.noSource ? 'NO_SOURCE' : state.upToDate ? 'UP_TO_DATE' : state.skipped ? 'SKIPPED' : 'SUCCESS'
                            def className = task.class.name.replaceAll(/_Decorated$/, '')
                            def json = groovy.json.JsonOutput.toJson([job: System.getProperty('gretl.test.jobId'), build: System.getProperty('gretl.test.buildVariant'), backend: System.getProperty('gretl.test.executionTarget'), path: task.path, className: className, outcome: outcome])
                            synchronized (traceFile) { traceFile.append(json + System.lineSeparator(), 'UTF-8') }
                        }
                    } as TaskExecutionListener)
                }
                """;
    }

    private static final class MaterializationException extends RuntimeException {
        private final IOException ioException;
        private MaterializationException(IOException ioException) { this.ioException = ioException; }
    }
}
