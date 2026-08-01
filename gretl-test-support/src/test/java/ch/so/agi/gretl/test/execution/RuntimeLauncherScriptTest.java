package ch.so.agi.gretl.test.execution;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeLauncherScriptTest {
    @Test
    void launcherAddsOfflineExactlyOnce() throws Exception {
        String script = launcher();
        assertEquals(1, script.lines().filter(line -> line.contains("--offline")).count());
    }

    @Test
    void launcherUsesBundledInitScript() throws Exception {
        assertTrue(launcher().contains("--init-script /opt/gretl/init/gretl.init.gradle"));
    }

    @Test
    void launcherForwardsAllUserArguments() throws Exception {
        String script = launcher();
        assertTrue(script.contains("\"$@\""));
    }

    @Test
    void launcherDoesNotForceNoDaemon() throws Exception {
        String script = launcher();
        assertFalse(script.contains("--no-daemon"));
    }

    @Test
    void launcherDoesNotForceDaemon() throws Exception {
        assertFalse(launcher().contains("--daemon"));
    }

    @Test
    void launcherDoesNotDisableDockerNetworking() throws Exception {
        assertFalse(launcher().contains("--network"));
    }

    private String launcher() throws Exception {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (directory != null && !Files.isRegularFile(directory.resolve("docker/gretl"))) {
            directory = directory.getParent();
        }
        if (directory == null) {
            throw new IllegalStateException("Cannot locate repository root");
        }
        return Files.readString(directory.resolve("docker/gretl"));
    }
}
