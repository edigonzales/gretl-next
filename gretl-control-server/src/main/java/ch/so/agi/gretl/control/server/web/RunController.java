package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.server.persistence.RunRecord;
import ch.so.agi.gretl.control.server.run.LogService;
import ch.so.agi.gretl.control.server.run.RunService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {
    private final RunService runService;
    private final LogService logService;

    public RunController(RunService runService, LogService logService) {
        this.runService = runService;
        this.logService = logService;
    }

    @GetMapping
    public List<RunRecord> runs() {
        return runService.recentRuns(200);
    }

    @GetMapping("/{runId}")
    public RunRecord run(@PathVariable String runId) {
        return runService.requireRun(runId);
    }

    @PostMapping("/{runId}/cancel")
    public void cancel(@PathVariable String runId) {
        runService.requestCancel(runId);
    }

    @PostMapping("/{runId}/retry")
    public RunRecord retry(@PathVariable String runId, Principal principal) {
        String actor = principal == null ? "anonymous" : principal.getName();
        return runService.retry(runId, actor);
    }

    @GetMapping(value = "/{runId}/logs", produces = MediaType.TEXT_PLAIN_VALUE)
    public String logs(@PathVariable String runId) {
        return runService.readLog(runId);
    }

    @GetMapping("/{runId}/logs/stream")
    public SseEmitter streamLogs(@PathVariable String runId) {
        runService.requireRun(runId);
        return logService.stream(runId);
    }
}
