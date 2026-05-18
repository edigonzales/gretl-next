package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.api.RunClaimRequest;
import ch.so.agi.gretl.control.api.RunClaimResponse;
import ch.so.agi.gretl.control.api.RunLogAppendRequest;
import ch.so.agi.gretl.control.api.RunStatusUpdateRequest;
import ch.so.agi.gretl.control.api.WorkerHeartbeatRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationResponse;
import ch.so.agi.gretl.control.server.persistence.WorkerRecord;
import ch.so.agi.gretl.control.server.run.RunService;
import ch.so.agi.gretl.control.server.run.WorkerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class WorkerController {
    private final WorkerAuthentication authentication;
    private final WorkerService workerService;
    private final RunService runService;

    public WorkerController(WorkerAuthentication authentication, WorkerService workerService, RunService runService) {
        this.authentication = authentication;
        this.workerService = workerService;
        this.runService = runService;
    }

    @GetMapping("/api/workers")
    public List<WorkerRecord> workers() {
        return workerService.workers();
    }

    @PostMapping("/api/worker/register")
    public WorkerRegistrationResponse register(@RequestBody WorkerRegistrationRequest request, HttpServletRequest servletRequest) {
        authentication.requireWorkerToken(servletRequest);
        return workerService.register(request);
    }

    @PostMapping("/api/worker/heartbeat")
    public void heartbeat(@RequestBody WorkerHeartbeatRequest request, HttpServletRequest servletRequest) {
        authentication.requireWorkerToken(servletRequest);
        workerService.heartbeat(request);
    }

    @PostMapping("/api/worker/claim")
    public RunClaimResponse claim(@RequestBody RunClaimRequest request, HttpServletRequest servletRequest) {
        authentication.requireWorkerToken(servletRequest);
        return runService.claim(request);
    }

    @PostMapping("/api/worker/runs/{runId}/status")
    public void updateRunStatus(
            @PathVariable String runId,
            @RequestBody RunStatusUpdateRequest request,
            HttpServletRequest servletRequest) {
        authentication.requireWorkerToken(servletRequest);
        runService.updateStatus(runId, request);
    }

    @PostMapping("/api/worker/runs/{runId}/logs")
    public void appendLog(
            @PathVariable String runId,
            @RequestBody RunLogAppendRequest request,
            HttpServletRequest servletRequest) {
        authentication.requireWorkerToken(servletRequest);
        runService.appendLog(runId, request);
    }

    @GetMapping("/api/worker/runs/{runId}/cancel-requested")
    public Map<String, Boolean> cancelRequested(@PathVariable String runId, HttpServletRequest servletRequest) {
        authentication.requireWorkerToken(servletRequest);
        return Map.of("cancelRequested", runService.isCancelRequested(runId));
    }
}
