package ch.so.agi.gretl.control.server.run;

import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

@Service
public class LogService {
    private final GretlControlProperties properties;

    public LogService(GretlControlProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void createLogDirectory() throws IOException {
        Files.createDirectories(properties.getLogDirectory());
    }

    public Path pathFor(String runId) {
        return properties.getLogDirectory().resolve(runId + ".log").toAbsolutePath().normalize();
    }

    public synchronized void append(String runId, String stream, String line, List<String> secretValues) {
        String redacted = redact(line, secretValues);
        String record = "%s [%s] %s%n".formatted(Instant.now(), stream == null ? "stdout" : stream, redacted);
        try {
            Files.writeString(pathFor(runId), record, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Could not append run log for " + runId, e);
        }
    }

    public String read(String runId) {
        Path path = pathFor(runId);
        if (!Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read run log for " + runId, e);
        }
    }

    public SseEmitter stream(String runId) {
        SseEmitter emitter = new SseEmitter(30_000L);
        Thread thread = new Thread(() -> {
            int position = 0;
            long deadline = System.currentTimeMillis() + 30_000L;
            try {
                while (System.currentTimeMillis() < deadline) {
                    String content = read(runId);
                    if (content.length() > position) {
                        String delta = content.substring(position);
                        position = content.length();
                        emitter.send(SseEmitter.event().name("log").data(delta));
                    }
                    Thread.sleep(1000L);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, "gretl-log-stream-" + runId);
        thread.setDaemon(true);
        thread.start();
        return emitter;
    }

    private String redact(String line, List<String> secretValues) {
        if (line == null) {
            return "";
        }
        String redacted = line;
        for (String secretValue : secretValues) {
            if (secretValue != null && !secretValue.isBlank()) {
                redacted = redacted.replace(secretValue, "******");
            }
        }
        return redacted;
    }
}
