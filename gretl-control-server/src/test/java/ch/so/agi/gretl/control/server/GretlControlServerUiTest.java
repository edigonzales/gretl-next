package ch.so.agi.gretl.control.server;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "gretl.control.manifest-path=src/test/resources/gretl-server.yml",
        "gretl.control.security.worker-token=test-token",
        "gretl.control.secrets.master-key=test-secret-key"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(statements = {
        "delete from runs",
        "delete from workers",
        "delete from secrets"
})
class GretlControlServerUiTest {
    @Autowired
    MockMvc mvc;

    @Test
    void rendersJobOverviewAndDetail() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        mvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Import colors")))
                .andExpect(content().string(containsString("No runs")))
                .andExpect(content().string(containsString("/jobs/import-colors")));

        mvc.perform(get("/jobs/import-colors"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Start job")))
                .andExpect(content().string(containsString("name=\"limit\"")))
                .andExpect(content().string(containsString("Default: 100")));
    }

    @Test
    void validatesStartParametersAndGivesActiveRunsPrecedence() throws Exception {
        mvc.perform(post("/jobs/import-colors/runs")
                        .with(csrf())
                        .param("limit", "not-a-number"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("requires integer value")));

        mvc.perform(post("/jobs/import-colors/runs")
                        .with(csrf())
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Queued run")));

        mvc.perform(get("/ui/fragments/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("QUEUED")));

        String firstRunId = claimNextRun();
        mvc.perform(post("/api/worker/runs/" + firstRunId + "/status")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"status\":\"SUCCEEDED\",\"exitCode\":0,\"message\":\"done\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/ui/fragments/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SUCCEEDED")));

        mvc.perform(post("/jobs/import-colors/runs")
                        .with(csrf())
                        .param("limit", "30"))
                .andExpect(status().isOk());

        mvc.perform(get("/ui/fragments/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("QUEUED")));
    }

    @Test
    void rendersInlineLogsAndWorkerDetails() throws Exception {
        mvc.perform(post("/jobs/import-colors/runs")
                        .with(csrf())
                        .param("limit", "25"))
                .andExpect(status().isOk());

        String runId = claimNextRun();
        mvc.perform(post("/api/worker/runs/" + runId + "/logs")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"stream\":\"stdout\",\"line\":\"hello from gretl\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/runs/" + runId + "/logs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hello from gretl")))
                .andExpect(content().string(containsString("EventSource")));

        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Worker 1")))
                .andExpect(content().string(containsString("import-colors")));
    }

    @Test
    void rendersManifestReloadResult() throws Exception {
        mvc.perform(post("/admin/manifest/reload").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reload succeeded")));
    }

    private String claimNextRun() throws Exception {
        mvc.perform(put("/api/secrets/db-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"secret-value\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/worker/register")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"displayName\":\"Worker 1\",\"labels\":[\"small\"],\"capacity\":1}"))
                .andExpect(status().isOk());
        MvcResult claimResult = mvc.perform(post("/api/worker/claim")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"labels\":[\"small\"],\"availableSlots\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(claimResult.getResponse().getContentAsString(), "$.run.runId");
    }
}
