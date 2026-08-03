package ch.so.agi.gretl.test.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TestJobExecutionSelector {
    public List<TestJobExecutionCase> select(
            TestJobCatalog catalog,
            TestJobExecutionTarget target,
            boolean includeOptional) {
        return select(catalog, target, includeOptional
                ? TestJobRequirementSelection.REQUIRED_AND_OPTIONAL
                : TestJobRequirementSelection.REQUIRED_ONLY,
                TestJobFixtureSelection.ALL, Set.of());
    }

    public List<TestJobExecutionCase> select(TestJobCatalog catalog, TestJobSelectionCriteria criteria) {
        if (catalog == null || criteria == null) throw new IllegalArgumentException("catalog and criteria are required");
        List<TestJobExecutionCase> selected = new ArrayList<>();
        for (TestJobDescriptor descriptor : catalog.all()) {
            if (!matchesFixtures(descriptor, criteria.fixtureSelection())
                    || !matchesCategory(descriptor, criteria.categories())) continue;
            TestJobExecutionRequirement jobRequirement = descriptor.requirementFor(criteria.target());
            for (TestJobBuildVariant variant : descriptor.builds()) {
                TestJobExecutionDeclaration declaration = variant.declarationFor(criteria.target());
                TestJobExecutionRequirement effective = declaration == null
                        ? jobRequirement : declaration.requirement();
                if (matchesRequirement(effective, criteria.requirementSelection())) {
                    selected.add(new TestJobExecutionCase(descriptor, variant, criteria.target(), effective));
                }
            }
        }
        return List.copyOf(selected);
    }

    private List<TestJobExecutionCase> select(TestJobCatalog catalog, TestJobExecutionTarget target,
                                               TestJobRequirementSelection requirementSelection,
                                               TestJobFixtureSelection fixtureSelection, Set<String> categories) {
        return select(catalog, new TestJobSelectionCriteria(target, requirementSelection, fixtureSelection, categories));
    }

    public List<TestJobExecutionCase> select(TestJobCatalog catalog, TestJobExecutionTarget target) {
        return select(catalog, TestJobSelectionProperties.fromSystemProperties(target));
    }

    private boolean matchesRequirement(TestJobExecutionRequirement requirement,
                                       TestJobRequirementSelection selection) {
        return switch (selection) {
            case REQUIRED_ONLY -> requirement == TestJobExecutionRequirement.REQUIRED;
            case OPTIONAL_ONLY -> requirement == TestJobExecutionRequirement.OPTIONAL;
            case REQUIRED_AND_OPTIONAL -> requirement == TestJobExecutionRequirement.REQUIRED
                    || requirement == TestJobExecutionRequirement.OPTIONAL;
        };
    }

    private boolean matchesFixtures(TestJobDescriptor descriptor, TestJobFixtureSelection selection) {
        return switch (selection) {
            case WITHOUT_FIXTURES -> descriptor.fixtures().isEmpty();
            case WITH_FIXTURES -> !descriptor.fixtures().isEmpty();
            case ALL -> true;
        };
    }

    private boolean matchesCategory(TestJobDescriptor descriptor, Set<String> categories) {
        return categories.isEmpty() || categories.contains(descriptor.category());
    }
}
