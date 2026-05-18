package ch.so.agi.gretl.control.worker.client;

import ch.so.agi.gretl.control.api.RunClaimRequest;
import ch.so.agi.gretl.control.api.RunClaimResponse;
import ch.so.agi.gretl.control.api.RunLogAppendRequest;
import ch.so.agi.gretl.control.api.RunStatusUpdateRequest;
import ch.so.agi.gretl.control.api.WorkerHeartbeatRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationResponse;
import ch.so.agi.gretl.control.worker.WorkerProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ControlPlaneClient {
    private final RestClient restClient;

    public ControlPlaneClient(RestClient.Builder builder, WorkerProperties properties) {
        this.restClient = builder
                .baseUrl(properties.getServerUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .build();
    }

    public WorkerRegistrationResponse register(WorkerRegistrationRequest request) {
        return restClient.post()
                .uri("/api/worker/register")
                .body(request)
                .retrieve()
                .body(WorkerRegistrationResponse.class);
    }

    public void heartbeat(WorkerHeartbeatRequest request) {
        restClient.post()
                .uri("/api/worker/heartbeat")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public RunClaimResponse claim(RunClaimRequest request) {
        return restClient.post()
                .uri("/api/worker/claim")
                .body(request)
                .retrieve()
                .body(RunClaimResponse.class);
    }

    public void updateStatus(String runId, RunStatusUpdateRequest request) {
        restClient.post()
                .uri("/api/worker/runs/{runId}/status", runId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void appendLog(String runId, RunLogAppendRequest request) {
        restClient.post()
                .uri("/api/worker/runs/{runId}/logs", runId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    public boolean cancelRequested(String runId) {
        Map<String, Object> body = restClient.get()
                .uri("/api/worker/runs/{runId}/cancel-requested", runId)
                .retrieve()
                .body(Map.class);
        Object value = body == null ? null : body.get("cancelRequested");
        return value instanceof Boolean bool && bool;
    }
}
