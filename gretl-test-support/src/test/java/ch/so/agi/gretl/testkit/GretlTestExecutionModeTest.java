package ch.so.agi.gretl.testkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GretlTestExecutionModeTest {
    private final Map<String, String> originalProperties = new HashMap<>();

    @BeforeEach
    void captureSystemProperties() {
        originalProperties.put(
                GretlTestSystemProperties.EXECUTION_MODE,
                System.getProperty(GretlTestSystemProperties.EXECUTION_MODE));
    }

    @AfterEach
    void restoreSystemProperty() {
        String originalMode = originalProperties.get(GretlTestSystemProperties.EXECUTION_MODE);
        if (originalMode == null) {
            System.clearProperty(GretlTestSystemProperties.EXECUTION_MODE);
        } else {
            System.setProperty(GretlTestSystemProperties.EXECUTION_MODE, originalMode);
        }
    }

    @Test
    void missingPropertyUsesPluginClasspath() {
        System.clearProperty(GretlTestSystemProperties.EXECUTION_MODE);

        assertEquals(GretlTestExecutionMode.PLUGIN_CLASSPATH, GretlTestExecutionMode.current());
    }

    @Test
    void acceptsPublishedArtifactWithHyphensAndDifferentCase() {
        System.setProperty(GretlTestSystemProperties.EXECUTION_MODE, "published-artifact");
        assertEquals(GretlTestExecutionMode.PUBLISHED_ARTIFACT, GretlTestExecutionMode.current());

        System.setProperty(GretlTestSystemProperties.EXECUTION_MODE, "PuBlIsHeD_ArTiFaCt");
        assertEquals(GretlTestExecutionMode.PUBLISHED_ARTIFACT, GretlTestExecutionMode.current());
    }

    @Test
    void rejectsUnknownValueWithPropertyAndAllowedValues() {
        System.setProperty(GretlTestSystemProperties.EXECUTION_MODE, "runtime-image");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, GretlTestExecutionMode::current);

        assertTrue(exception.getMessage().contains(GretlTestSystemProperties.EXECUTION_MODE));
        assertTrue(exception.getMessage().contains("PLUGIN_CLASSPATH"));
        assertTrue(exception.getMessage().contains("PUBLISHED_ARTIFACT"));
    }
}
