package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.api.RunStatus;
import ch.so.agi.gretl.control.manifest.JobDefinition;
import ch.so.agi.gretl.control.server.manifest.ManifestCatalog;
import ch.so.agi.gretl.control.server.persistence.RunRecord;
import ch.so.agi.gretl.control.server.persistence.RunRepository;
import ch.so.agi.gretl.control.server.persistence.WorkerRecord;
import ch.so.agi.gretl.control.server.run.RunService;
import ch.so.agi.gretl.control.server.run.WorkerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ControlUiService {
    private static final EnumSet<RunStatus> ACTIVE_STATUSES = EnumSet.of(
            RunStatus.QUEUED,
            RunStatus.CLAIMED,
            RunStatus.RUNNING);

    private final ManifestCatalog catalog;
    private final RunService runService;
    private final RunRepository runRepository;
    private final WorkerService workerService;

    public ControlUiService(
            ManifestCatalog catalog,
            RunService runService,
            RunRepository runRepository,
            WorkerService workerService) {
        this.catalog = catalog;
        this.runService = runService;
        this.runRepository = runRepository;
        this.workerService = workerService;
    }

    public List<JobSummary> jobSummaries() {
        return catalog.jobs().stream()
                .map(this::jobSummary)
                .toList();
    }

    public JobDefinition requireJob(String jobId) {
        return catalog.findJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job '" + jobId + "' not found."));
    }

    public List<RunSummary> runSummariesForJob(String jobId) {
        return runService.recentRunsForJob(jobId, 100).stream()
                .map(this::runSummary)
                .toList();
    }

    public RunSummary runSummary(RunRecord run) {
        return new RunSummary(run, statusClass(run.status()), isActive(run));
    }

    public boolean isActive(RunRecord run) {
        return ACTIVE_STATUSES.contains(run.status());
    }

    public List<WorkerSummary> workerSummaries() {
        Map<String, List<RunSummary>> activeRunsByWorker = runRepository.findActive().stream()
                .filter(run -> run.workerId() != null)
                .collect(Collectors.groupingBy(RunRecord::workerId,
                        Collectors.mapping(this::runSummary, Collectors.toList())));
        return workerService.workers().stream()
                .map(worker -> new WorkerSummary(worker, activeRunsByWorker.getOrDefault(worker.id(), List.of())))
                .toList();
    }

    private JobSummary jobSummary(JobDefinition job) {
        List<RunRecord> recentRuns = runService.recentRunsForJob(job.id(), 100);
        RunRecord activeRun = recentRuns.stream()
                .filter(this::isActive)
                .findFirst()
                .orElse(null);
        RunRecord displayRun = activeRun == null
                ? recentRuns.stream().findFirst().orElse(null)
                : activeRun;
        return new JobSummary(
                job,
                displayRun == null ? null : runSummary(displayRun),
                displayRun == null ? "No runs" : displayRun.status().name(),
                displayRun == null ? "unknown" : statusClass(displayRun.status()),
                job.name() == null || job.name().isBlank() ? job.id() : job.name(),
                !job.isEnabled());
    }

    private String statusClass(RunStatus status) {
        return switch (status) {
            case QUEUED, CLAIMED, RUNNING -> "active";
            case SUCCEEDED -> "succeeded";
            case FAILED, CANCELLED, TIMED_OUT -> "failed";
            case SKIPPED -> "skipped";
        };
    }

    public record JobSummary(
            JobDefinition job,
            RunSummary displayRun,
            String statusLabel,
            String statusClass,
            String title,
            boolean disabled) {
    }

    public record RunSummary(RunRecord run, String statusClass, boolean active) {
    }

    public record WorkerSummary(WorkerRecord worker, List<RunSummary> activeRuns) {
    }
}
