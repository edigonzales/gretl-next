package ch.so.agi.gretl.test.job;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestJobSelectionPropertiesTest {
    @Test
    void parsesTheThreeRequirementModes() {
        assertEquals(TestJobRequirementSelection.REQUIRED_ONLY,
                TestJobSelectionProperties.parseRequirementSelection("required"));
        assertEquals(TestJobRequirementSelection.OPTIONAL_ONLY,
                TestJobSelectionProperties.parseRequirementSelection(" optional "));
        assertEquals(TestJobRequirementSelection.REQUIRED_AND_OPTIONAL,
                TestJobSelectionProperties.parseRequirementSelection("ALL"));
    }

    @Test
    void parsesFixtureModesAndCategories() {
        assertEquals(TestJobFixtureSelection.WITHOUT_FIXTURES,
                TestJobSelectionProperties.parseFixtureSelection("without-fixtures"));
        assertEquals(TestJobFixtureSelection.WITH_FIXTURES,
                TestJobSelectionProperties.parseFixtureSelection("with-fixtures"));
        assertEquals(Set.of("core", "network"),
                TestJobSelectionProperties.parseCategories("core, network, core"));
    }

    @Test
    void rejectsBlankAndMalformedValues() {
        assertThrows(IllegalArgumentException.class,
                () -> TestJobSelectionProperties.parseFixtureSelection(""));
        assertThrows(IllegalArgumentException.class,
                () -> TestJobSelectionProperties.parseCategories("core,Not-A-Category"));
    }
}
