package ch.so.agi.gretl.test.job;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestJobExecutionSelectorTest {
    private static final FileSystemTestJobCatalog CATALOG =
            FileSystemTestJobCatalog.load(Path.of("..", "test-jobs"));
    private static final TestJobExecutionTarget TARGET = TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE;

    @Test
    void selectsRequiredJobsOnly() {
        var cases = select(TestJobRequirementSelection.REQUIRED_ONLY, TestJobFixtureSelection.ALL, Set.of());
        assertTrue(cases.stream().allMatch(item -> item.requirement() == TestJobExecutionRequirement.REQUIRED));
        assertTrue(cases.stream().anyMatch(item -> item.descriptor().id().equals("core-gzip")));
    }

    @Test
    void selectsOptionalJobsOnly() {
        var cases = select(TestJobRequirementSelection.OPTIONAL_ONLY, TestJobFixtureSelection.ALL, Set.of());
        assertFalse(cases.isEmpty());
        assertTrue(cases.stream().allMatch(item -> item.requirement() == TestJobExecutionRequirement.OPTIONAL));
        assertTrue(cases.stream().anyMatch(item -> item.descriptor().id().equals("geotools-read-shapefile")));
        assertFalse(cases.stream().anyMatch(item -> item.descriptor().id().equals("core-gzip")));
    }

    @Test
    void selectsRequiredAndOptionalJobs() {
        var required = select(TestJobRequirementSelection.REQUIRED_ONLY, TestJobFixtureSelection.ALL, Set.of());
        var all = select(TestJobRequirementSelection.REQUIRED_AND_OPTIONAL, TestJobFixtureSelection.ALL, Set.of());
        assertTrue(all.size() > required.size());
    }

    @Test
    void selectsFixtureFreeJobs() {
        var cases = select(TestJobRequirementSelection.REQUIRED_ONLY,
                TestJobFixtureSelection.WITHOUT_FIXTURES, Set.of());
        assertTrue(cases.stream().allMatch(item -> item.descriptor().fixtures().isEmpty()));
        assertFalse(cases.stream().anyMatch(item -> item.descriptor().id().equals("network-http-curl")));
    }

    @Test
    void selectsFixtureBackedJobs() {
        var cases = select(TestJobRequirementSelection.REQUIRED_ONLY,
                TestJobFixtureSelection.WITH_FIXTURES, Set.of());
        assertTrue(cases.stream().allMatch(item -> !item.descriptor().fixtures().isEmpty()));
        assertTrue(cases.stream().anyMatch(item -> item.descriptor().id().equals("network-http-curl")));
    }

    @Test
    void combinesCategoryAndFixtureSelection() {
        var cases = select(TestJobRequirementSelection.REQUIRED_ONLY,
                TestJobFixtureSelection.WITHOUT_FIXTURES, Set.of("core"));
        assertTrue(cases.stream().allMatch(item -> item.descriptor().category().equals("core")));
        assertEquals(Set.of("core-gzip"),
                cases.stream().map(item -> item.descriptor().id()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void buildVariantOverrideDeterminesEffectiveRequirement() {
        var descriptor = new TestJobDescriptor(2, "override", "override", "core",
                java.util.List.of(new TestJobBuildVariant("groovy", "build.gradle",
                        Map.of(TARGET, TestJobExecutionDeclaration.optional("slow")))),
                java.util.List.of(":run"), java.util.List.of(new ExpectedTaskExecution(":run", "Task")),
                Map.of(TARGET, TestJobExecutionRequirement.REQUIRED), Set.of(), java.util.List.of(),
                "override", java.time.Duration.ofSeconds(1), Path.of("."));
        TestJobCatalog catalog = new InMemoryCatalog(descriptor);
        var selected = new TestJobExecutionSelector().select(catalog,
                new TestJobSelectionCriteria(TARGET, TestJobRequirementSelection.OPTIONAL_ONLY,
                        TestJobFixtureSelection.ALL, Set.of()));
        assertEquals(1, selected.size());
        assertEquals(TestJobExecutionRequirement.OPTIONAL, selected.get(0).requirement());
    }

    @Test
    void notApplicableIsNeverSelected() {
        var descriptor = new TestJobDescriptor(2, "not-applicable", "not-applicable", "core",
                java.util.List.of(new TestJobBuildVariant("groovy", "build.gradle")),
                java.util.List.of(":run"), java.util.List.of(new ExpectedTaskExecution(":run", "Task")),
                Map.of(TARGET, TestJobExecutionRequirement.NOT_APPLICABLE), Set.of(), java.util.List.of(),
                "not-applicable", java.time.Duration.ofSeconds(1), Path.of("."));
        assertTrue(new TestJobExecutionSelector().select(new InMemoryCatalog(descriptor),
                TestJobSelectionCriteria.required(TARGET)).isEmpty());
    }

    @Test
    void invalidSelectionPropertyFailsClearly() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> TestJobSelectionProperties.parseRequirementSelection("sometimes"));
        assertTrue(failure.getMessage().contains(TestJobSelectionProperties.REQUIREMENT_PROPERTY));
    }

    private static java.util.List<TestJobExecutionCase> select(TestJobRequirementSelection requirement,
                                                                TestJobFixtureSelection fixtures,
                                                                Set<String> categories) {
        return new TestJobExecutionSelector().select(CATALOG,
                new TestJobSelectionCriteria(TARGET, requirement, fixtures, categories));
    }

    private record InMemoryCatalog(TestJobDescriptor descriptor) implements TestJobCatalog {
        @Override public java.util.List<TestJobDescriptor> all() { return java.util.List.of(descriptor); }
        @Override public java.util.Optional<TestJobDescriptor> find(String id) {
            return descriptor.id().equals(id) ? java.util.Optional.of(descriptor) : java.util.Optional.empty();
        }
        @Override public TestJobDescriptor require(String id) { return find(id).orElseThrow(); }
        @Override public java.util.stream.Stream<TestJobDescriptor> supporting(TestJobExecutionTarget target) {
            return descriptor.supports(target) ? java.util.stream.Stream.of(descriptor) : java.util.stream.Stream.empty();
        }
        @Override public Path rootDirectory() { return Path.of("."); }
    }
}
