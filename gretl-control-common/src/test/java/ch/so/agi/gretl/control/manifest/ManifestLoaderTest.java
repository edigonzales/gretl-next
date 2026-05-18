package ch.so.agi.gretl.control.manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManifestLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsAndNormalizesJobManifest() throws Exception {
        Path manifest = tempDir.resolve("gretl-server.yml");
        Files.writeString(manifest, """
                jobs:
                  - id: import-colors
                    name: Import colors
                    projectDir: jobs/colors
                    tasks: [importData]
                    enabled: true
                    cron: "0 0 2 * * ?"
                    timezone: Europe/Zurich
                    timeout: PT20M
                    workerLabels: [small]
                    jvm:
                      maxHeap: 768m
                      args: ["-Dexample=true"]
                    parameters:
                      - name: limit
                        type: INTEGER
                        defaultValue: 100
                    triggers:
                      - jobId: import-colors
                        on: SUCCESS
                """);

        GretlServerManifest loaded = new ManifestLoader().load(manifest);

        assertEquals(1, loaded.jobs().size());
        JobDefinition job = loaded.jobs().get(0);
        assertEquals("import-colors", job.id());
        assertEquals("768m", job.jvm().maxHeap());
        assertEquals(Map.of("limit", 100), new ManifestValidator().normalizeParameters(job, Map.of()));
    }

    @Test
    void rejectsUnknownTriggerTarget() throws Exception {
        Path manifest = tempDir.resolve("gretl-server.yml");
        Files.writeString(manifest, """
                jobs:
                  - id: import-colors
                    projectDir: jobs/colors
                    tasks: [importData]
                    triggers:
                      - jobId: missing-job
                        on: SUCCESS
                """);

        ManifestException exception = assertThrows(ManifestException.class, () -> new ManifestLoader().load(manifest));

        assertEquals("Job 'import-colors' references unknown trigger job 'missing-job'.", exception.getMessage());
    }

    @Test
    void rejectsInvalidCron() throws Exception {
        Path manifest = tempDir.resolve("gretl-server.yml");
        Files.writeString(manifest, """
                jobs:
                  - id: import-colors
                    projectDir: jobs/colors
                    tasks: [importData]
                    cron: "not a cron"
                """);

        ManifestException exception = assertThrows(ManifestException.class, () -> new ManifestLoader().load(manifest));

        assertEquals("Job 'import-colors' has invalid cron expression 'not a cron'.", exception.getMessage());
    }
}
