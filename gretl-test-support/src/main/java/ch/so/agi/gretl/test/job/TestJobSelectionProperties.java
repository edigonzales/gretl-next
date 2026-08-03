package ch.so.agi.gretl.test.job;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class TestJobSelectionProperties {
    public static final String REQUIREMENT_PROPERTY = "gretl.job.requirementSelection";
    public static final String FIXTURE_PROPERTY = "gretl.job.fixtureSelection";
    public static final String CATEGORIES_PROPERTY = "gretl.job.categories";

    private TestJobSelectionProperties() {
    }

    public static TestJobSelectionCriteria fromSystemProperties(TestJobExecutionTarget target) {
        return new TestJobSelectionCriteria(target,
                parseRequirementSelection(System.getProperty(REQUIREMENT_PROPERTY, "required")),
                parseFixtureSelection(System.getProperty(FIXTURE_PROPERTY, "all")),
                parseCategories(System.getProperty(CATEGORIES_PROPERTY, "")));
    }

    static TestJobRequirementSelection parseRequirementSelection(String value) {
        return switch (normalize(value, REQUIREMENT_PROPERTY)) {
            case "required" -> TestJobRequirementSelection.REQUIRED_ONLY;
            case "optional" -> TestJobRequirementSelection.OPTIONAL_ONLY;
            case "all" -> TestJobRequirementSelection.REQUIRED_AND_OPTIONAL;
            default -> throw invalid(REQUIREMENT_PROPERTY, value, "required, optional or all");
        };
    }

    static TestJobFixtureSelection parseFixtureSelection(String value) {
        return switch (normalize(value, FIXTURE_PROPERTY)) {
            case "without-fixtures" -> TestJobFixtureSelection.WITHOUT_FIXTURES;
            case "with-fixtures" -> TestJobFixtureSelection.WITH_FIXTURES;
            case "all" -> TestJobFixtureSelection.ALL;
            default -> throw invalid(FIXTURE_PROPERTY, value, "without-fixtures, with-fixtures or all");
        };
    }

    static Set<String> parseCategories(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .peek(item -> {
                    if (!item.matches("[a-z][a-z0-9-]*")) {
                        throw invalid(CATEGORIES_PROPERTY, value, "comma-separated category names");
                    }
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String value, String property) {
        if (value == null || value.isBlank()) throw invalid(property, value, "a non-empty value");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalArgumentException invalid(String property, String value, String expected) {
        return new IllegalArgumentException("Invalid " + property + " value '" + value
                + "'; expected " + expected + ".");
    }
}
