package ch.so.agi.gretl.control.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "gretl.control.manifest-path=src/test/resources/gretl-server.yml",
        "gretl.control.security.worker-token=test-token",
        "gretl.control.secrets.master-key=test-secret-key"
})
@AutoConfigureMockMvc
class GretlControlServerApiTest {
    @Autowired
    MockMvc mvc;

    @Test
    void enqueuesAndClaimsRunWithResolvedSecrets() throws Exception {
        mvc.perform(put("/api/secrets/db-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"secret-value\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/jobs/import-colors/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parameters\":{\"limit\":25}}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId", is("import-colors")))
                .andExpect(jsonPath("$.status", is("QUEUED")));

        mvc.perform(post("/api/worker/register")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"displayName\":\"Worker 1\",\"labels\":[\"small\"],\"capacity\":1}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/worker/claim")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":\"worker-1\",\"labels\":[\"small\"],\"availableSlots\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.jobId", is("import-colors")))
                .andExpect(jsonPath("$.run.parameters.limit", is(25)))
                .andExpect(jsonPath("$.run.secrets.db-password", is("secret-value")))
                .andExpect(jsonPath("$.run.jvmMaxHeap", is("512m")));
    }

    @Test
    void rejectsWorkerRequestWithoutToken() throws Exception {
        mvc.perform(get("/api/worker/runs/missing/cancel-requested"))
                .andExpect(status().isUnauthorized());
    }
}
