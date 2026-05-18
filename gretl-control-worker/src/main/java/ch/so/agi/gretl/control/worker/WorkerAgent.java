package ch.so.agi.gretl.control.worker;

import ch.so.agi.gretl.control.api.ClaimedRun;
import ch.so.agi.gretl.control.api.RunClaimRequest;
import ch.so.agi.gretl.control.api.RunClaimResponse;
import ch.so.agi.gretl.control.api.WorkerHeartbeatRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationRequest;
import ch.so.agi.gretl.control.api.WorkerRegistrationResponse;
import ch.so.agi.gretl.control.worker.client.ControlPlaneClient;
import ch.so.agi.gretl.control.worker.execution.RunExecutor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class WorkerAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerAgent.class);

    private final WorkerProperties properties;
    private final ControlPlaneClient client;
    private final RunExecutor executor;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, Boolean> activeRuns = new ConcurrentHashMap<>();

    public WorkerAgent(WorkerProperties properties, ControlPlaneClient client, RunExecutor executor) {
        this.properties = properties;
        this.client = client;
        this.executor = executor;
    }

    @PostConstruct
    public void register() {
        WorkerRegistrationResponse response = client.register(new WorkerRegistrationRequest(
                properties.getWorkerId(),
                displayName(),
                labels(),
                properties.getCapacity()));
        properties.setWorkerId(response.workerId());
        LOGGER.info("Registered GRETL worker {} at {}.", response.workerId(), properties.getServerUrl());
    }

    @Scheduled(fixedDelayString = "#{@workerProperties.heartbeatInterval.toMillis()}")
    public void heartbeat() {
        client.heartbeat(new WorkerHeartbeatRequest(properties.getWorkerId(), labels(), properties.getCapacity(), activeRuns.size()));
    }

    @Scheduled(fixedDelayString = "#{@workerProperties.pollInterval.toMillis()}")
    public void poll() {
        int availableSlots = Math.max(0, properties.getCapacity() - activeRuns.size());
        while (availableSlots > 0) {
            RunClaimResponse response = client.claim(new RunClaimRequest(properties.getWorkerId(), labels(), availableSlots));
            if (response == null || !response.hasRun()) {
                return;
            }
            ClaimedRun run = response.run();
            activeRuns.put(run.runId(), Boolean.TRUE);
            executorService.submit(() -> {
                try {
                    executor.execute(run);
                } finally {
                    activeRuns.remove(run.runId());
                }
            });
            availableSlots--;
        }
    }

    @PreDestroy
    public void stop() {
        executorService.shutdownNow();
    }

    private List<String> labels() {
        return properties.getLabels().stream()
                .filter(label -> label != null && !label.isBlank())
                .map(String::trim)
                .toList();
    }

    private String displayName() {
        return properties.getDisplayName() == null || properties.getDisplayName().isBlank()
                ? properties.getWorkerId()
                : properties.getDisplayName();
    }
}
