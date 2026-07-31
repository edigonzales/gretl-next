package ch.so.agi.gretl.test.execution;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GretlExecutionModeTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(GretlExecutionMode.SYSTEM_PROPERTY);
    }

    @Test
    void defaultsToTestkitClasspath() {
        assertEquals(GretlExecutionMode.TESTKIT_CLASSPATH, GretlExecutionMode.current());
    }

    @Test
    void parsesAllSupportedValues() {
        assertEquals(GretlExecutionMode.RUNTIME_IMAGE, GretlExecutionMode.parse("runtime-image"));
        assertEquals(GretlExecutionMode.PUBLISHED_ARTIFACT, GretlExecutionMode.parse("PUBLISHED_ARTIFACT"));
    }

    @Test
    void rejectsUnknownValueClearly() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GretlExecutionMode.parse("not-a-mode"));
        assertTrue(error.getMessage().contains("unknown value"));
        assertTrue(error.getMessage().contains(GretlExecutionMode.SYSTEM_PROPERTY));
        assertTrue(error.getMessage().contains("RUNTIME_IMAGE"));
    }
}
