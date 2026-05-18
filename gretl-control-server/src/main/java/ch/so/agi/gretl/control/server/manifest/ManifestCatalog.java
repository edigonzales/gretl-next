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

    public ManifestCatalog(GretlControlProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void load() {
        Path manifestPath = resolveManifestPath();
        if (!Files.exists(manifestPath)) {
            LOGGER.warn("GRETL control manifest not found at {}. Starting with an empty job catalog.",
                    manifestPath.toAbsolutePath());
            return;
        }
        GretlServerManifest loaded = loader.load(manifestPath);
        manifest.set(loaded);
        jobsById = loaded.jobs().stream().collect(Collectors.toUnmodifiableMap(JobDefinition::id, Function.identity()));
        LOGGER.info("Loaded {} GRETL control job definitions from {}.", loaded.jobs().size(), manifestPath.toAbsolutePath());
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
}
