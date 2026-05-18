package ch.so.agi.gretl.control.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "gretl.control.worker")
@Component
public class WorkerProperties {
    private String serverUrl = "http://localhost:8080";
    private String token = "dev-worker-token";
    private String workerId = defaultWorkerId();
    private String displayName = workerId;
    private List<String> labels = List.of();
    private int capacity = 1;
    private Path workspaceRoot = Path.of(".").toAbsolutePath().normalize();
    private String gretlExecutable = "gretl";
    private Duration pollInterval = Duration.ofSeconds(5);
    private Duration heartbeatInterval = Duration.ofSeconds(15);
    private Duration cancelPollInterval = Duration.ofSeconds(2);

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public String getGretlExecutable() {
        return gretlExecutable;
    }

    public void setGretlExecutable(String gretlExecutable) {
        this.gretlExecutable = gretlExecutable;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getCancelPollInterval() {
        return cancelPollInterval;
    }

    public void setCancelPollInterval(Duration cancelPollInterval) {
        this.cancelPollInterval = cancelPollInterval;
    }

    private static String defaultWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "gretl-worker";
        }
    }
}
