package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.api.RunStartRequest;
import ch.so.agi.gretl.control.manifest.JobDefinition;
import ch.so.agi.gretl.control.server.manifest.ManifestCatalog;
import ch.so.agi.gretl.control.server.persistence.RunRecord;
import ch.so.agi.gretl.control.server.run.RunService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final ManifestCatalog catalog;
    private final RunService runService;

    public JobController(ManifestCatalog catalog, RunService runService) {
        this.catalog = catalog;
        this.runService = runService;
    }

    @GetMapping
    public List<JobDefinition> jobs() {
        return catalog.jobs();
    }

    @GetMapping("/{jobId}")
    public JobDefinition job(@PathVariable String jobId) {
        return catalog.findJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job '" + jobId + "' not found."));
    }

    @GetMapping("/{jobId}/runs")
    public List<RunRecord> runs(@PathVariable String jobId) {
        return runService.recentRunsForJob(jobId, 100);
    }

    @PostMapping("/{jobId}/runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunRecord start(@PathVariable String jobId, @RequestBody(required = false) RunStartRequest request, Principal principal) {
        String actor = principal == null ? "anonymous" : principal.getName();
        return runService.enqueueManual(jobId, request == null ? null : request.parameters(), actor);
    }
}
