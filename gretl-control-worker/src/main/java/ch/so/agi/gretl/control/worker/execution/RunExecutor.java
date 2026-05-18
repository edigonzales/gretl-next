package ch.so.agi.gretl.control.worker.execution;

import ch.so.agi.gretl.control.api.ClaimedRun;
import ch.so.agi.gretl.control.api.RunLogAppendRequest;
import ch.so.agi.gretl.control.api.RunStatus;
import ch.so.agi.gretl.control.api.RunStatusUpdateRequest;
import ch.so.agi.gretl.control.worker.WorkerProperties;
import ch.so.agi.gretl.control.worker.client.ControlPlaneClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RunExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunExecutor.class);

    private final WorkerProperties properties;
    private final GretlCommandFactory commandFactory;
    private final ControlPlaneClient client;

    public RunExecutor(WorkerProperties properties, GretlCommandFactory commandFactory, ControlPlaneClient client) {
        this.properties = properties;
        this.commandFactory = commandFactory;
        this.client = client;
    }

    public void execute(ClaimedRun run) {
        Process process = null;
        try {
            GretlCommand command = commandFactory.create(run);
            if (!Files.isDirectory(command.workingDirectory())) {
                throw new IllegalStateException("Working directory does not exist: " + command.workingDirectory());
            }
            client.updateStatus(run.runId(), new RunStatusUpdateRequest(properties.getWorkerId(), RunStatus.RUNNING, null,
                    "Starting GRETL process."));
            ProcessBuilder processBuilder = new ProcessBuilder(command.command());
            processBuilder.directory(command.workingDirectory().toFile());
            processBuilder.environment().putAll(command.environment());
            process = processBuilder.start();
            stream(run.runId(), "stdout", process.getInputStream());
            stream(run.runId(), "stderr", process.getErrorStream());

            RunStatus finalStatus = waitForCompletion(run, process);
            int exitCode = process.isAlive() ? -1 : process.exitValue();
            if (finalStatus == RunStatus.SUCCEEDED || finalStatus == RunStatus.FAILED) {
                finalStatus = exitCode == 0 ? RunStatus.SUCCEEDED : RunStatus.FAILED;
            }
            client.updateStatus(run.runId(), new RunStatusUpdateRequest(properties.getWorkerId(), finalStatus, exitCode,
                    "GRETL process finished with exit code " + exitCode + "."));
        } catch (Exception e) {
            LOGGER.warn("Run {} failed before process completion: {}", run.runId(), e.getMessage());
            client.updateStatus(run.runId(), new RunStatusUpdateRequest(properties.getWorkerId(), RunStatus.FAILED, null, e.getMessage()));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private RunStatus waitForCompletion(ClaimedRun run, Process process) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(run.timeoutSeconds());
        while (true) {
            if (process.waitFor(properties.getCancelPollInterval().toMillis(), TimeUnit.MILLISECONDS)) {
                return process.exitValue() == 0 ? RunStatus.SUCCEEDED : RunStatus.FAILED;
            }
            if (client.cancelRequested(run.runId())) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                return RunStatus.CANCELLED;
            }
            if (Instant.now().isAfter(deadline)) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                return RunStatus.TIMED_OUT;
            }
        }
    }

    private void stream(String runId, String stream, InputStream inputStream) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    client.appendLog(runId, new RunLogAppendRequest(properties.getWorkerId(), stream, line));
                }
            } catch (IOException e) {
                LOGGER.warn("Could not stream {} for run {}: {}", stream, runId, e.getMessage());
            } catch (RuntimeException e) {
                LOGGER.warn("Could not send {} log for run {}: {}", stream, runId, e.getMessage());
            }
        }, "gretl-run-log-" + runId + "-" + stream);
        thread.setDaemon(true);
        thread.start();
    }
}
