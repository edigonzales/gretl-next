package ch.so.agi.gretl.control.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "gretl.control.manifest-path=src/test/resources/gretl-server.yml",
        "gretl.control.security.worker-token=test-token",
        "gretl.control.security.oidc-enabled=true",
        "gretl.control.secrets.master-key=test-secret-key",
        "spring.security.oauth2.client.registration.test.client-id=test-client",
        "spring.security.oauth2.client.registration.test.client-secret=test-secret",
        "spring.security.oauth2.client.registration.test.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.test.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.registration.test.scope=openid",
        "spring.security.oauth2.client.provider.test.authorization-uri=https://issuer.example.test/oauth2/authorize",
        "spring.security.oauth2.client.provider.test.token-uri=https://issuer.example.test/oauth2/token",
        "spring.security.oauth2.client.provider.test.user-info-uri=https://issuer.example.test/userinfo",
        "spring.security.oauth2.client.provider.test.user-name-attribute=sub",
        "spring.security.oauth2.client.provider.test.jwk-set-uri=https://issuer.example.test/jwks"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(statements = {
        "delete from runs",
        "delete from workers",
        "delete from secrets"
})
class GretlControlServerUiSecurityTest {
    @Autowired
    MockMvc mvc;

    @Test
    void viewerCanReadButCannotStartJobs() throws Exception {
        mvc.perform(get("/jobs").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Import colors")));

        mvc.perform(post("/jobs/import-colors/runs")
                        .with(user("viewer").roles("VIEWER"))
                        .with(csrf())
                        .param("limit", "25"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCanStartJobsButCannotReloadCatalog() throws Exception {
        mvc.perform(post("/jobs/import-colors/runs")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Queued run")));

        mvc.perform(post("/admin/manifest/reload")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReloadCatalog() throws Exception {
        mvc.perform(post("/admin/manifest/reload")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reload succeeded")));
    }
}
