package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.fixture.TestJobFixtureRequirement;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TestJobDescriptor(
        int schemaVersion,
        String id,
        String description,
        String category,
        List<TestJobBuildVariant> builds,
        List<String> entryTasks,
        List<ExpectedTaskExecution> expectedTasks,
        Map<TestJobExecutionTarget, TestJobExecutionRequirement> executionTargets,
        Set<String> capabilities,
        List<TestJobFixtureRequirement> fixtures,
        String assertions,
        Duration timeout,
        Path sourceDirectory) {

    public TestJobDescriptor {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(category, "category must not be null");
        builds = List.copyOf(Objects.requireNonNull(builds, "builds must not be null"));
        entryTasks = List.copyOf(Objects.requireNonNull(entryTasks, "entryTasks must not be null"));
        expectedTasks = List.copyOf(Objects.requireNonNull(expectedTasks, "expectedTasks must not be null"));
        executionTargets = Map.copyOf(Objects.requireNonNull(executionTargets, "executionTargets must not be null"));
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities must not be null"));
        fixtures = List.copyOf(Objects.requireNonNull(fixtures, "fixtures must not be null"));
        Objects.requireNonNull(assertions, "assertions must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        sourceDirectory = Objects.requireNonNull(sourceDirectory, "sourceDirectory must not be null")
                .toAbsolutePath().normalize();
    }

    public TestJobDescriptor(int schemaVersion, String id, String description, String category,
                             List<TestJobBuildVariant> builds, List<String> entryTasks,
                             List<ExpectedTaskExecution> expectedTasks,
                             Map<TestJobExecutionTarget, TestJobExecutionRequirement> executionTargets,
                             Set<String> capabilities, String assertions, Duration timeout,
                             Path sourceDirectory) {
        this(schemaVersion, id, description, category, builds, entryTasks, expectedTasks,
                executionTargets, capabilities, List.of(), assertions, timeout, sourceDirectory);
    }

    public TestJobBuildVariant requireBuild(String variantId) {
        return builds.stream().filter(build -> build.id().equals(variantId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown build variant '" + variantId
                        + "' for test job " + id));
    }

    public TestJobExecutionRequirement requirementFor(TestJobExecutionTarget target) {
        return Optional.ofNullable(executionTargets.get(target)).orElseThrow(
                () -> new IllegalArgumentException("Test job " + id + " has no requirement for " + target));
    }

    public boolean supports(TestJobExecutionTarget target) {
        return requirementFor(target) != TestJobExecutionRequirement.NOT_APPLICABLE;
    }
}
