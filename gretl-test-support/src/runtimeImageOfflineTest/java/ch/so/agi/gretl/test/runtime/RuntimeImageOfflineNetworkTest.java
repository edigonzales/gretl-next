package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageOfflineExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageOfflineNetworkTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void onlyLoopbackAndNoExternalTcpAreAvailable() {
        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("network"));
        project.settingsGroovy("rootProject.name = 'network'\n")
                .buildGroovy("""
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('networkCanary') {
                            doLast {
                                def interfaces = java.net.NetworkInterface.networkInterfaces.toList()
                                assert interfaces.every { it.isLoopback() }
                                try {
                                    java.net.InetAddress.getByName('example.invalid')
                                    throw new AssertionError('external DNS resolution unexpectedly succeeded')
                                } catch (java.net.UnknownHostException expected) {
                                    // NetworkMode=none is the guarantee; this is a diagnostic canary.
                                }
                                def socket = new java.net.Socket()
                                try {
                                    socket.connect(new java.net.InetSocketAddress('198.51.100.1', 80), 250)
                                    throw new AssertionError('external TCP connection unexpectedly succeeded')
                                } catch (java.io.IOException expected) {
                                    // NetworkMode=none is the guarantee; this is a diagnostic canary.
                                } finally {
                                    socket.close()
                                }
                            }
                        }
                        """);

        GretlBuildResult result = new RuntimeImageOfflineExecutor(RuntimeImageDescriptor.fromSystemProperties()).execute(
                GretlBuildRequest.builder(project.directory())
                        .arguments(List.of("--rerun-tasks", "networkCanary"))
                        .timeout(Duration.ofMinutes(2))
                        .runtimeImageOptions(RuntimeImageRunOptions.offline())
                        .build());
        assertTrue(result.successful(), result.output());
    }
}
