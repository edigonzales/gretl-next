package ch.so.agi.gretl.combined;

import ch.so.agi.gretl.testkit.GretlTestSystemProperties;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedTestKitDaemonJvmFunctionalTest extends CombinedPluginTestSupport {
    @Test
    void testKitDaemonReceivesConfiguredJvmArguments() throws Exception {
        String configuredJvmArgs = System.getProperty(
                GretlTestSystemProperties.GRADLE_JVM_ARGS,
                GretlTestSystemProperties.DEFAULT_GRADLE_JVM_ARGS).trim();
        String daemonAssertions = Arrays.stream(configuredJvmArgs.split("\\s+"))
                .map(argument -> "assert inputArguments.contains('" + escapeGroovy(argument) + "')")
                .collect(Collectors.joining("\n                        "));

        writeSettings();
        writeGroovyBuild("""
                import java.lang.management.ManagementFactory

                tasks.register('inspectTestKitDaemonJvm') {
                    doLast {
                        def inputArguments = ManagementFactory.runtimeMXBean.inputArguments
                        println 'TESTKIT_DAEMON_JVM_ARGS=' + inputArguments
                        %s
                    }
                }
                """.formatted(daemonAssertions));

        BuildResult result = run("inspectTestKitDaemonJvm");

        assertTrue(result.getOutput().contains("TESTKIT_DAEMON_JVM_ARGS="), result.getOutput());
        for (String argument : configuredJvmArgs.split("\\s+")) {
            assertTrue(result.getOutput().contains(argument), result.getOutput());
        }
    }

    private static String escapeGroovy(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
