package ch.so.agi.gretl.control.server.manifest;

import ch.so.agi.gretl.control.manifest.GretlServerManifest;
import ch.so.agi.gretl.control.manifest.JobDefinition;
import ch.so.agi.gretl.control.manifest.ManifestLoader;
import ch.so.agi.gretl.control.manifest.ManifestValidator;
import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ManifestCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestCatalog.class);

    private final GretlControlProperties properties;
    private final ManifestValidator validator = new ManifestValidator();
    private final ManifestLoader loader = new ManifestLoader(validator);
    private final AtomicReference<GretlServerManifest> manifest = new AtomicReference<>(new GretlServerManifest(List.of()));
    private volatile Map<String, JobDefinition> jobsById = Map.of();
    private volatile Path loadedPath;
    private volatile Instant loadedAt;
    private volatile Instant lastReloadAttemptAt;
    private volatile String lastReloadError;

    public ManifestCatalog(GretlControlProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void load() {
        Path manifestPath = resolveManifestPath();
        loadedPath = manifestPath;
        if (!Files.exists(manifestPath)) {
            LOGGER.warn("GRETL control manifest not found at {}. Starting with an empty job catalog.",
                    manifestPath.toAbsolutePath());
            loadedAt = Instant.now();
            return;
        }
        GretlServerManifest loaded = loader.load(manifestPath);
        apply(loaded, manifestPath, Instant.now());
        LOGGER.info("Loaded {} GRETL control job definitions from {}.", loaded.jobs().size(), manifestPath.toAbsolutePath());
    }

    public synchronized ManifestReload reload() {
        Path manifestPath = resolveManifestPath();
        Instant attempt = Instant.now();
        lastReloadAttemptAt = attempt;
        try {
            GretlServerManifest previous = manifest.get();
            GretlServerManifest loaded = loader.load(manifestPath);
            apply(loaded, manifestPath, attempt);
            LOGGER.info("Reloaded {} GRETL control job definitions from {}.",
                    loaded.jobs().size(), manifestPath.toAbsolutePath());
            return ManifestReload.success(manifestPath, attempt, diff(previous, loaded));
        } catch (RuntimeException e) {
            lastReloadError = e.getMessage();
            LOGGER.warn("Could not reload GRETL control manifest from {}. Keeping last valid catalog.",
                    manifestPath.toAbsolutePath(), e);
            return ManifestReload.failure(manifestPath, loadedAt, attempt, e.getMessage());
        }
    }

    public List<JobDefinition> jobs() {
        return manifest.get().jobs();
    }

    public Optional<JobDefinition> findJob(String jobId) {
        return Optional.ofNullable(jobsById.get(jobId));
    }

    public Map<String, Object> normalizeParameters(JobDefinition job, Map<String, Object> parameters) {
        return validator.normalizeParameters(job, parameters);
    }

    public long timeoutSeconds(JobDefinition job) {
        return validator.timeoutSeconds(job);
    }

    public ManifestStatus status() {
        return new ManifestStatus(
                loadedPath == null ? resolveManifestPath() : loadedPath,
                loadedAt,
                lastReloadAttemptAt,
                lastReloadError,
                jobsById.keySet().stream().sorted().toList());
    }

    public Path resolvedManifestPath() {
        return resolveManifestPath();
    }

    private void apply(GretlServerManifest loaded, Path manifestPath, Instant now) {
        manifest.set(loaded);
        jobsById = loaded.jobs().stream().collect(Collectors.toUnmodifiableMap(JobDefinition::id, Function.identity()));
        loadedPath = manifestPath;
        loadedAt = now;
        lastReloadAttemptAt = now;
        lastReloadError = null;
    }

    private ManifestChangeSet diff(GretlServerManifest previous, GretlServerManifest loaded) {
        Map<String, JobDefinition> previousJobs = byId(previous);
        Map<String, JobDefinition> loadedJobs = byId(loaded);
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> unchanged = new ArrayList<>();

        for (String jobId : loadedJobs.keySet()) {
            JobDefinition previousJob = previousJobs.get(jobId);
            if (previousJob == null) {
                added.add(jobId);
            } else if (previousJob.equals(loadedJobs.get(jobId))) {
                unchanged.add(jobId);
            } else {
                updated.add(jobId);
            }
        }
        for (String jobId : previousJobs.keySet()) {
            if (!loadedJobs.containsKey(jobId)) {
                removed.add(jobId);
            }
        }
        sort(added);
        sort(removed);
        sort(updated);
        sort(unchanged);
        return new ManifestChangeSet(added, removed, updated, unchanged);
    }

    private Map<String, JobDefinition> byId(GretlServerManifest input) {
        return input.jobs().stream().collect(Collectors.toUnmodifiableMap(JobDefinition::id, Function.identity()));
    }

    private void sort(List<String> values) {
        values.sort(Comparator.naturalOrder());
    }

    private Path resolveManifestPath() {
        Path configured = properties.getManifestPath();
        if (configured.isAbsolute() || Files.exists(configured)) {
            return configured;
        }
        Path parentRelative = Path.of("..").resolve(configured).normalize();
        if (Files.exists(parentRelative)) {
            return parentRelative;
        }
        return configured;
    }

    public record ManifestStatus(
            Path path,
            Instant loadedAt,
            Instant lastReloadAttemptAt,
            String lastReloadError,
            List<String> jobIds) {
    }

    public record ManifestReload(
            boolean success,
            Path path,
            Instant loadedAt,
            Instant attemptedAt,
            ManifestChangeSet changeSet,
            String error) {
        static ManifestReload success(Path path, Instant loadedAt, ManifestChangeSet changeSet) {
            return new ManifestReload(true, path, loadedAt, loadedAt, changeSet, null);
        }

        static ManifestReload failure(Path path, Instant loadedAt, Instant attemptedAt, String error) {
            return new ManifestReload(false, path, loadedAt, attemptedAt,
                    new ManifestChangeSet(List.of(), List.of(), List.of(), List.of()), error);
        }
    }

    public record ManifestChangeSet(
            List<String> addedJobs,
            List<String> removedJobs,
            List<String> updatedJobs,
            List<String> unchangedJobs) {
    }
}
