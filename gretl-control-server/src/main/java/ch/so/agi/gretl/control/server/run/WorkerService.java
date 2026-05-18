package ch.so.agi.gretl.control.server.run;

import ch.so.agi.gretl.control.api.WorkerHeartbeatRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationResponse;
import ch.so.agi.gretl.control.api.WorkerStatus;
import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import ch.so.agi.gretl.control.server.persistence.WorkerRecord;
import ch.so.agi.gretl.control.server.persistence.WorkerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class WorkerService {
    private final WorkerRepository repository;
    private final GretlControlProperties properties;

    public WorkerService(WorkerRepository repository, GretlControlProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public WorkerRegistrationResponse register(WorkerRegistrationRequest request) {
        String id = request.workerId() == null || request.workerId().isBlank()
                ? java.util.UUID.randomUUID().toString()
                : request.workerId();
        repository.upsert(new WorkerRecord(
                id,
                request.displayName() == null || request.displayName().isBlank() ? id : request.displayName(),
                request.labels() == null ? List.of() : request.labels(),
                Math.max(1, request.capacity()),
                0,
                Instant.now(),
                WorkerStatus.ONLINE));
        return new WorkerRegistrationResponse(id);
    }

    public void heartbeat(WorkerHeartbeatRequest request) {
        repository.upsert(new WorkerRecord(
                request.workerId(),
                request.workerId(),
                request.labels() == null ? List.of() : request.labels(),
                Math.max(1, request.capacity()),
                Math.max(0, request.activeRuns()),
                Instant.now(),
                WorkerStatus.ONLINE));
    }

    public List<WorkerRecord> workers() {
        return repository.findAll();
    }

    @Scheduled(fixedDelay = 30_000)
    public void markOfflineWorkers() {
        repository.markOfflineBefore(Instant.now().minus(properties.getWorkerOfflineAfter()));
    }
}
