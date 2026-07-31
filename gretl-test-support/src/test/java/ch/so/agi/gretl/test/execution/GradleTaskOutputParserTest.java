package ch.so.agi.gretl.test.execution;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleTaskOutputParserTest {
    private final GradleTaskOutputParser parser = new GradleTaskOutputParser();

    @Test
    void parsesSuccessAndFailedOutcome() {
        Map<String, GretlTaskOutcome> outcomes = parser.parse("""
                > Task :writeMarker SUCCESS
                > Task :failTask FAILED
                BUILD FAILED
                """);

        assertEquals(GretlTaskOutcome.SUCCESS, outcomes.get(":writeMarker"));
        assertEquals(GretlTaskOutcome.FAILED, outcomes.get(":failTask"));
    }

    @Test
    void parsesUpToDateAndNoSource() {
        Map<String, GretlTaskOutcome> outcomes = parser.parse("""
                > Task :cached UP-TO-DATE
                > Task :empty NO-SOURCE
                """);

        assertEquals(GretlTaskOutcome.UP_TO_DATE, outcomes.get(":cached"));
        assertEquals(GretlTaskOutcome.NO_SOURCE, outcomes.get(":empty"));
    }

    @Test
    void returnsEmptyMapForUnparseableOutput() {
        assertTrue(parser.parse("not Gradle output").isEmpty());
    }
}
