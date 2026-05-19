package ch.so.agi.gretl.control.server;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "gretl.control.manifest-path=build/test-manifests/reload-test.yml",
        "gretl.control.security.worker-token=test-token",
        "gretl.control.secrets.master-key=test-secret-key"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GretlControlManifestReloadTest {
    private static final Path MANIFEST = Path.of("build/test-manifests/reload-test.yml");
    private static final String QUARTZ_GROUP = "gretl-control-jobs";

    @Autowired
    MockMvc mvc;

    @Autowired
    Scheduler scheduler;

    @Test
    void reloadsManifestReconcilesQuartzAndSkipsQueuedRunsForRemovedOrDisabledJobs() throws Exception {
        assertScheduled("import-colors");
        assertScheduled("nightly-report");
        assertCron("nightly-report", "0 0 1 * * ?");

        String activeRunId = enqueue("active-job");
        claim(activeRunId);
        String removedRunId = enqueue("import-colors");
        String disabledRunId = enqueue("disable-me");

        writeManifest(UPDATED_MANIFEST);
        mvc.perform(post("/api/admin/manifest/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.addedJobs", hasItems("new-job")))
                .andExpect(jsonPath("$.removedJobs", containsInAnyOrder("active-job", "import-colors")))
                .andExpect(jsonPath("$.updatedJobs", containsInAnyOrder("disable-me", "nightly-report")))
                .andExpect(jsonPath("$.skippedQueuedRuns", containsInAnyOrder(removedRunId, disabledRunId)));

        mvc.perform(post("/api/worker/runs/" + activeRunId + "/logs")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"stream\":\"stdout\",\"line\":\"still running\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/worker/runs/" + activeRunId + "/status")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"status\":\"SUCCEEDED\",\"exitCode\":0,\"message\":\"done\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/runs/" + activeRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCEEDED")));

        mvc.perform(get("/api/runs/" + removedRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SKIPPED")))
                .andExpect(jsonPath("$.message", is("Job was removed or disabled by manifest reload.")));
        mvc.perform(get("/api/runs/" + disabledRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SKIPPED")));

        assertNotScheduled("import-colors");
        assertNotScheduled("disable-me");
        assertScheduled("nightly-report");
        assertScheduled("new-job");
        assertCron("nightly-report", "0 30 3 * * ?");

        writeManifest(INVALID_MANIFEST);
        mvc.perform(post("/api/admin/manifest/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", containsString("requires at least one task")));

        mvc.perform(get("/api/jobs/new-job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("new-job")));
        mvc.perform(get("/api/jobs/broken-job"))
                .andExpect(status().isNotFound());
    }

    private String enqueue(String jobId) throws Exception {
        String response = mvc.perform(post("/api/jobs/" + jobId + "/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parameters\":{}}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("QUEUED")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void claim(String runId) throws Exception {
        mvc.perform(post("/api/worker/claim")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"labels\":[\"active\"],\"availableSlots\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.runId", is(runId)));
    }

    private void assertScheduled(String jobId) throws Exception {
        assertTrue(scheduler.checkExists(JobKey.jobKey(jobId, QUARTZ_GROUP)), "Expected scheduled job " + jobId);
    }

    private void assertNotScheduled(String jobId) throws Exception {
        assertFalse(scheduler.checkExists(JobKey.jobKey(jobId, QUARTZ_GROUP)), "Expected no scheduled job " + jobId);
    }

    private void assertCron(String jobId, String expression) throws Exception {
        CronTrigger trigger = (CronTrigger) scheduler.getTriggersOfJob(JobKey.jobKey(jobId, QUARTZ_GROUP)).get(0);
        org.junit.jupiter.api.Assertions.assertEquals(expression, trigger.getCronExpression());
    }

    private static void writeManifest(String content) {
        try {
            Files.createDirectories(MANIFEST.getParent());
            Files.writeString(MANIFEST, content);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final String INITIAL_MANIFEST = """
            jobs:
              - id: import-colors
                name: Import colors
                projectDir: jobs/colors
                tasks: [importData]
                enabled: true
                cron: "0 0 2 * * ?"
                timezone: Europe/Zurich
                workerLabels: []
              - id: active-job
                name: Active job
                projectDir: jobs/active
                tasks: [run]
                enabled: true
                workerLabels: [active]
              - id: disable-me
                name: Disable me
                projectDir: jobs/disable-me
                tasks: [run]
                enabled: true
                workerLabels: []
              - id: nightly-report
                name: Nightly report
                projectDir: jobs/report
                tasks: [report]
                enabled: true
                cron: "0 0 1 * * ?"
                timezone: Europe/Zurich
                workerLabels: []
            """;

    private static final String UPDATED_MANIFEST = """
            jobs:
              - id: disable-me
                name: Disable me
                projectDir: jobs/disable-me
                tasks: [run]
                enabled: false
                workerLabels: []
              - id: nightly-report
                name: Nightly report
                projectDir: jobs/report
                tasks: [report]
                enabled: true
                cron: "0 30 3 * * ?"
                timezone: Europe/Zurich
                workerLabels: []
              - id: new-job
                name: New job
                projectDir: jobs/new
                tasks: [run]
                enabled: true
                cron: "0 0 4 * * ?"
                timezone: Europe/Zurich
                workerLabels: []
            """;

    private static final String INVALID_MANIFEST = """
            jobs:
              - id: broken-job
                name: Broken job
                projectDir: jobs/broken
                enabled: true
            """;

    static {
        writeManifest(INITIAL_MANIFEST);
    }
}
