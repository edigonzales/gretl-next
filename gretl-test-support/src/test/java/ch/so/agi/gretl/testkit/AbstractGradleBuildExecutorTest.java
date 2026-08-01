package ch.so.agi.gretl.testkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractGradleBuildExecutorTest {
    private String previousJvmArgs;

    @AfterEach
    void restoreJvmArgs() {
        if (previousJvmArgs == null) {
            System.clearProperty(GretlTestSystemProperties.GRADLE_JVM_ARGS);
        } else {
            System.setProperty(GretlTestSystemProperties.GRADLE_JVM_ARGS, previousJvmArgs);
        }
    }

    @Test
    void addsConfiguredDaemonJvmArgumentsByDefault() {
        previousJvmArgs = System.getProperty(GretlTestSystemProperties.GRADLE_JVM_ARGS);
        System.clearProperty(GretlTestSystemProperties.GRADLE_JVM_ARGS);

        List<String> arguments = AbstractGradleBuildExecutor.normalizeArguments("help");

        assertTrue(arguments.contains(
                "-Dorg.gradle.jvmargs=" + AbstractGradleBuildExecutor.DEFAULT_TEST_KIT_JVM_ARGS));
    }

    @Test
    void preservesExplicitDaemonJvmArguments() {
        previousJvmArgs = System.getProperty(GretlTestSystemProperties.GRADLE_JVM_ARGS);
        System.setProperty(GretlTestSystemProperties.GRADLE_JVM_ARGS, "-Xmx1g -XX:MaxMetaspaceSize=768m");

        List<String> arguments = AbstractGradleBuildExecutor.normalizeArguments(
                "-Dorg.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g", "help");

        assertEquals(1, arguments.stream()
                .filter(argument -> argument.startsWith("-Dorg.gradle.jvmargs="))
                .count());
        assertTrue(arguments.contains("-Dorg.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g"));
    }
}
