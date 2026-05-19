package ch.so.agi.gretl.control.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "gretl.control")
public class GretlControlProperties {
    private Path manifestPath = Path.of("gretl-server.yml");
    private Path logDirectory = Path.of("build/gretl-control/logs");
    private String defaultTimezone = "Europe/Zurich";
    private Duration workerOfflineAfter = Duration.ofMinutes(2);
    private final Manifest manifest = new Manifest();
    private final Security security = new Security();
    private final Secrets secrets = new Secrets();

    public Path getManifestPath() {
        return manifestPath;
    }

    public void setManifestPath(Path manifestPath) {
        this.manifestPath = manifestPath;
    }

    public Path getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(Path logDirectory) {
        this.logDirectory = logDirectory;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public Duration getWorkerOfflineAfter() {
        return workerOfflineAfter;
    }

    public void setWorkerOfflineAfter(Duration workerOfflineAfter) {
        this.workerOfflineAfter = workerOfflineAfter;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Security getSecurity() {
        return security;
    }

    public Secrets getSecrets() {
        return secrets;
    }

    public static class Manifest {
        private boolean watchEnabled;

        public boolean isWatchEnabled() {
            return watchEnabled;
        }

        public void setWatchEnabled(boolean watchEnabled) {
            this.watchEnabled = watchEnabled;
        }
    }

    public static class Security {
        private boolean oidcEnabled;
        private String workerToken = "dev-worker-token";

        public boolean isOidcEnabled() {
            return oidcEnabled;
        }

        public void setOidcEnabled(boolean oidcEnabled) {
            this.oidcEnabled = oidcEnabled;
        }

        public String getWorkerToken() {
            return workerToken;
        }

        public void setWorkerToken(String workerToken) {
            this.workerToken = workerToken;
        }
    }

    public static class Secrets {
        private String masterKey;

        public String getMasterKey() {
            return masterKey;
        }

        public void setMasterKey(String masterKey) {
            this.masterKey = masterKey;
        }
    }
}
