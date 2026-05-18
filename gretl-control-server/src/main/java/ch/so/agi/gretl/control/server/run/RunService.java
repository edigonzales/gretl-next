package ch.so.agi.gretl.control.server.run;

import ch.so.agi.gretl.control.api.ClaimedRun;
import ch.so.agi.gretl.control.api.RunClaimRequest;
import ch.so.agi.gretl.control.api.RunClaimResponse;
import ch.so.agi.gretl.control.api.RunLogAppendRequest;
import ch.so.agi.gretl.control.api.RunStatus;
import ch.so.agi.gretl.control.api.RunStatusUpdateRequest;
import ch.so.agi.gretl.control.api.RunTriggerType;
import ch.so.agi.gretl.control.manifest.JobDefinition;
import ch.so.agi.gretl.control.manifest.TriggerEvent;
import ch.so.agi.gretl.control.server.manifest.ManifestCatalog;
import ch.so.agi.gretl.control.server.persistence.RunRecord;
import ch.so.agi.gretl.control.server.persistence.RunRepository;
import ch.so.agi.gretl.control.server.secrets.SecretService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RunService {
    private final ManifestCatalog catalog;
    private final RunRepository runRepository;
    private final SecretService secretService;
    private final LogService logService;
    private final NotificationService notificationService;

    public RunService(
            ManifestCatalog catalog,
            RunRepository runRepository,
            SecretService secretService,
            LogService logService,
            NotificationService notificationService) {
        this.catalog = catalog;
        this.runRepository = runRepository;
        this.secretService = secretService;
        this.logService = logService;
        this.notificationService = notificationService;
    }

    @Transactional
    public RunRecord enqueueManual(String jobId, Map<String, Object> parameters, String triggeredBy) {
        JobDefinition job = requireJob(jobId);
        if (!job.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job '" + jobId + "' is disabled.");
        }
        return enqueue(job, catalog.normalizeParameters(job, parameters), RunTriggerType.MANUAL, triggeredBy);
    }

    @Transactional
    public RunRecord enqueueScheduled(String jobId) {
        JobDefinition job = requireJob(jobId);
        if (!job.isEnabled()) {
            return createSkipped(job, "Job is disabled.");
        }
        if (runRepository.hasActiveRun(job.id())) {
            return createSkipped(job, "Previous run still active; overlap policy is skip.");
        }
        return enqueue(job, catalog.normalizeParameters(job, Map.of()), RunTriggerType.SCHEDULE, "scheduler");
    }

    @Transactional
    public RunRecord retry(String runId, String triggeredBy) {
        RunRecord previous = requireRun(runId);
        JobDefinition job = requireJob(previous.jobId());
        if (!job.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job '" + job.id() + "' is disabled.");
        }
        return enqueue(job, previous.parameters(), RunTriggerType.RETRY, triggeredBy);
    }

    public List<RunRecord> recentRuns(int limit) {
        return runRepository.findRecent(limit);
    }

    public List<RunRecord> recentRunsForJob(String jobId, int limit) {
        requireJob(jobId);
        return runRepository.findRecentForJob(jobId, limit);
    }

    public RunRecord requireRun(String runId) {
        return runRepository.find(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run '" + runId + "' not found."));
    }

    @Transactional
    public void requestCancel(String runId) {
        requireRun(runId);
        runRepository.requestCancel(runId);
    }

    public boolean isCancelRequested(String runId) {
        return requireRun(runId).cancelRequested();
    }

    @Transactional
    public synchronized RunClaimResponse claim(RunClaimRequest request) {
        if (request.availableSlots() <= 0) {
            return new RunClaimResponse(null);
        }
        for (RunRecord queued : runRepository.findQueued()) {
            JobDefinition job = catalog.findJob(queued.jobId()).orElse(null);
            if (job == null || !job.isEnabled() || !workerMatches(job, request.labels())) {
                continue;
            }
            if (runRepository.claim(queued.id(), request.workerId(), Instant.now())) {
                Map<String, String> secrets = secretService.resolve(job.secretRefs());
                runRepository.setLogPath(queued.id(), logService.pathFor(queued.id()).toString());
                ClaimedRun claimedRun = new ClaimedRun(
                        queued.id(),
                        job.id(),
                        job.projectDir(),
                        job.tasks(),
                        queued.parameters(),
                        secrets,
                        job.jvm().maxHeap(),
                        job.jvm().args(),
                        catalog.timeoutSeconds(job));
                return new RunClaimResponse(claimedRun);
            }
        }
        return new RunClaimResponse(null);
    }

    @Transactional
    public void updateStatus(String runId, RunStatusUpdateRequest request) {
        RunRecord current = requireRun(runId);
        if (current.workerId() != null && !current.workerId().equals(request.workerId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Run is owned by worker '" + current.workerId() + "'.");
        }
        runRepository.updateStatus(runId, request.status(), request.exitCode(), request.message(), Instant.now());
        if (isTerminal(request.status())) {
            RunRecord completed = requireRun(runId);
            JobDefinition job = requireJob(completed.jobId());
            notificationService.notify(job, completed);
            enqueueStatusTriggers(completed);
        }
    }

    public void appendLog(String runId, RunLogAppendRequest request) {
        RunRecord run = requireRun(runId);
        JobDefinition job = requireJob(run.jobId());
        List<String> secretValues = secretService.resolve(job.secretRefs()).values().stream().toList();
        logService.append(runId, request.stream(), request.line(), secretValues);
    }

    public String readLog(String runId) {
        requireRun(runId);
        return logService.read(runId);
    }

    private RunRecord enqueue(JobDefinition job, Map<String, Object> parameters, RunTriggerType triggerType, String triggeredBy) {
        RunRecord run = new RunRecord(
                UUID.randomUUID().toString(),
                job.id(),
                RunStatus.QUEUED,
                triggerType,
                triggeredBy,
                null,
                Instant.now(),
                null,
                null,
                null,
                null,
                false,
                parameters,
                null,
                null);
        runRepository.insert(run);
        return run;
    }

    private RunRecord createSkipped(JobDefinition job, String message) {
        Instant now = Instant.now();
        RunRecord run = new RunRecord(
                UUID.randomUUID().toString(),
                job.id(),
                RunStatus.SKIPPED,
                RunTriggerType.SCHEDULE,
                "scheduler",
                null,
                now,
                null,
                null,
                now,
                null,
                false,
                Map.of(),
                message,
                null);
        runRepository.insert(run);
        return run;
    }

    private void enqueueStatusTriggers(RunRecord sourceRun) {
        TriggerEvent event = switch (sourceRun.status()) {
            case SUCCEEDED -> TriggerEvent.SUCCESS;
            case FAILED, TIMED_OUT, CANCELLED -> TriggerEvent.FAILURE;
            default -> null;
        };
        if (event == null) {
            return;
        }
        for (JobDefinition candidate : catalog.jobs()) {
            boolean matches = candidate.triggers().stream()
                    .anyMatch(trigger -> sourceRun.jobId().equals(trigger.jobId()) && event == trigger.on());
            if (matches && candidate.isEnabled()) {
                enqueue(candidate, catalog.normalizeParameters(candidate, Map.of()), RunTriggerType.STATUS_TRIGGER, sourceRun.id());
            }
        }
    }

    private boolean workerMatches(JobDefinition job, List<String> workerLabels) {
        return workerLabels != null && workerLabels.containsAll(job.workerLabels());
    }

    private boolean isTerminal(RunStatus status) {
        return status == RunStatus.SUCCEEDED
                || status == RunStatus.FAILED
                || status == RunStatus.CANCELLED
                || status == RunStatus.TIMED_OUT
                || status == RunStatus.SKIPPED;
    }

    private JobDefinition requireJob(String jobId) {
        return catalog.findJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job '" + jobId + "' not found."));
    }
}
