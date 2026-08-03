package ch.so.agi.gretl.test.job;

import java.util.Objects;
import java.util.Set;

public record TestJobSelectionCriteria(
        TestJobExecutionTarget target,
        TestJobRequirementSelection requirementSelection,
        TestJobFixtureSelection fixtureSelection,
        Set<String> categories) {

    public TestJobSelectionCriteria {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(requirementSelection, "requirementSelection must not be null");
        Objects.requireNonNull(fixtureSelection, "fixtureSelection must not be null");
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories must not be null"));
    }

    public static TestJobSelectionCriteria required(TestJobExecutionTarget target) {
        return new TestJobSelectionCriteria(target, TestJobRequirementSelection.REQUIRED_ONLY,
                TestJobFixtureSelection.ALL, Set.of());
    }
}
