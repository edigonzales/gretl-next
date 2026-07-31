package ch.so.agi.gretl.test.process;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ProcessExecutor {
    private final SecretRedactor redactor;

    public ProcessExecutor() {
        this(new SecretRedactor());
    }

    public ProcessExecutor(SecretRedactor redactor) {
        this.redactor = redactor;
    }

    public ProcessResult execute(ProcessRequest request) {
        Instant started = Instant.now();
        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(request.command())
                    .directory(request.workingDirectory().toFile());
            builder.environment().putAll(request.environment());
            process = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot start process " + redactor.redact(request.command().toString(), request.secretValues())
                            + " in " + request.workingDirectory(), e);
        }

        FutureTask<String> stdout = readAsync(process.getInputStream());
        FutureTask<String> stderr = readAsync(process.getErrorStream());
        Thread stdoutThread = new Thread(stdout, "gretl-process-stdout");
        Thread stderrThread = new Thread(stderr, "gretl-process-stderr");
        stdoutThread.start();
        stderrThread.start();

        boolean interrupted = false;
        boolean timedOut = false;
        int exitCode = -1;
        try {
            if (!process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
                timedOut = true;
                destroyProcessTree(process);
                process.waitFor(5, TimeUnit.SECONDS);
            }
            if (!timedOut) {
                exitCode = process.exitValue();
            }
        } catch (InterruptedException e) {
            interrupted = true;
            destroyProcessTree(process);
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        } finally {
            if (timedOut) {
                destroyProcessTree(process);
            }
        }

        String standardOutput = getOutput(stdout, stdoutThread, request.timeout());
        String standardError = getOutput(stderr, stderrThread, request.timeout());
        if (timedOut) {
            standardError = standardError + System.lineSeparator()
                    + "Process timed out after " + request.timeout() + ".";
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return new ProcessResult(
                exitCode,
                redactor.redact(standardOutput, request.secretValues()),
                redactor.redact(standardError, request.secretValues()),
                Duration.between(started, Instant.now()),
                redactor.redact(request.command(), request.secretValues()));
    }

    private FutureTask<String> readAsync(java.io.InputStream stream) {
        return new FutureTask<>(() -> new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    private String getOutput(FutureTask<String> output, Thread thread, Duration timeout) {
        try {
            output.get(Math.max(1, Math.min(5, timeout.toSeconds() + 1)), TimeUnit.SECONDS);
            return output.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "<output collection interrupted>";
        } catch (ExecutionException e) {
            return "<output collection failed: " + e.getCause() + ">";
        } catch (TimeoutException e) {
            thread.interrupt();
            return "<output collection timed out>";
        }
    }

    private void destroyProcessTree(Process process) {
        ProcessHandle handle = process.toHandle();
        List<ProcessHandle> descendants = handle.descendants().toList();
        java.util.Collections.reverse(descendants);
        descendants.forEach(child -> {
            child.destroy();
            if (child.isAlive()) {
                child.destroyForcibly();
            }
        });
        handle.destroy();
        if (handle.isAlive()) {
            handle.destroyForcibly();
        }
    }
}
