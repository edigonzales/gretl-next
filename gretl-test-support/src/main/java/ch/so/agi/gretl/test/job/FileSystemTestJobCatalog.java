package ch.so.agi.gretl.test.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class FileSystemTestJobCatalog implements TestJobCatalog {
    private final Path root;
    private final List<TestJobDescriptor> jobs;

    public static FileSystemTestJobCatalog load(Path rootDirectory) {
        return new FileSystemTestJobCatalog(rootDirectory, new TestJobYamlReader(), new TestJobDescriptorValidator());
    }

    FileSystemTestJobCatalog(Path rootDirectory, TestJobYamlReader reader, TestJobDescriptorValidator validator) {
        root = rootDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Test job catalog root is not a directory: " + root);
        List<Path> descriptors = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().equals("job.yaml"))
                    .filter(path -> !containsIgnoredDirectory(root.relativize(path)))
                    .forEach(descriptors::add);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot discover test jobs below " + root, e);
        }
        if (descriptors.isEmpty()) throw new IllegalArgumentException("Test job catalog is empty: " + root);
        Map<String, Path> ids = new HashMap<>();
        List<TestJobDescriptor> loaded = new ArrayList<>();
        for (Path descriptor : descriptors) {
            TestJobDescriptor job = reader.read(descriptor);
            if (ids.putIfAbsent(job.id(), descriptor) != null) throw new IllegalArgumentException("Duplicate test job id '" + job.id() + "' in " + descriptor);
            validator.validate(job);
            loaded.add(job);
        }
        loaded.sort(Comparator.comparing(TestJobDescriptor::id));
        jobs = List.copyOf(loaded);
    }

    @Override public List<TestJobDescriptor> all() { return jobs; }
    @Override public Optional<TestJobDescriptor> find(String id) { return jobs.stream().filter(job -> job.id().equals(id)).findFirst(); }
    @Override public TestJobDescriptor require(String id) { return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown test job: " + id)); }
    @Override public Stream<TestJobDescriptor> supporting(TestJobExecutionTarget target) { return jobs.stream().filter(job -> job.supports(target)); }
    @Override public Path rootDirectory() { return root; }

    private static boolean containsIgnoredDirectory(Path relative) {
        for (Path part : relative) {
            if (part.toString().equals("build") || part.toString().equals(".gradle")
                    || part.toString().equals(".git")) {
                return true;
            }
        }
        return false;
    }
}
