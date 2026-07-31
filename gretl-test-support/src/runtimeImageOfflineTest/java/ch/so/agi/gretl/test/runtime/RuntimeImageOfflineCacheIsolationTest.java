package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageOfflineExecutor;
import ch.so.agi.gretl.test.project.GradleTestProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageOfflineCacheIsolationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void poisonedHostCacheCannotInfluenceFreshWritableGradleHome() throws Exception {
        Path poisonedHostCache = temporaryDirectory.resolve("poisoned-host-cache");
        Files.createDirectories(poisonedHostCache.resolve("caches/modules-2"));
        Files.writeString(poisonedHostCache.resolve("caches/modules-2/marker.txt"), "HOST_POISON");

        GradleTestProject project = GradleTestProject.create(temporaryDirectory.resolve("consumer"));
        project.settingsGroovy("rootProject.name = 'cache-isolation'\n")
                .buildGroovy("""
                        plugins { id 'ch.so.agi.gretl' }
                        tasks.register('cacheCanary') {
                            doLast {
                                assert System.getenv('GRADLE_USER_HOME') == '/work/gradle-home'
                                assert !file('/work/gradle-home/caches/modules-2/marker.txt').exists()
                            }
                        }
                        """);

        GretlBuildRequest request = GretlBuildRequest.builder(project.directory())
                .arguments(List.of("--rerun-tasks", "cacheCanary"))
                .timeout(Duration.ofMinutes(2))
                .runtimeImageOptions(RuntimeImageRunOptions.offline())
                .build();
        RuntimeImageOfflineExecutor executor = new RuntimeImageOfflineExecutor(RuntimeImageDescriptor.fromSystemProperties());
        GretlBuildResult result = executor.execute(request);

        assertTrue(result.successful(), result.output());
        assertFalse(result.output().contains("HOST_POISON"), result.output());
        assertTrue(executor.containerMounts(request).stream()
                .noneMatch(mount -> mount.hostPath().equals(poisonedHostCache)), "host cache was mounted");
    }
}
