package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.fixture.TestFixtureRegistry;

import java.nio.file.Path;
import java.util.Objects;

public record TestJobExecutionSessionConfiguration(
        Path materializedJobsRoot,
        TestJobExecutionTarget target,
        TestJobBackendContext backendContext,
        TestJobMaterializer materializer,
        TestJobAssertionRegistry assertionRegistry,
        TestFixtureRegistry fixtureRegistry,
        MaterializedJobRetentionPolicy retentionPolicy) {
    public TestJobExecutionSessionConfiguration {
        materializedJobsRoot = Objects.requireNonNull(materializedJobsRoot, "materializedJobsRoot must not be null")
                .toAbsolutePath().normalize();
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(backendContext, "backendContext must not be null");
        materializer = materializer == null ? new DefaultTestJobMaterializer() : materializer;
        assertionRegistry = Objects.requireNonNull(assertionRegistry, "assertionRegistry must not be null");
        fixtureRegistry = Objects.requireNonNull(fixtureRegistry, "fixtureRegistry must not be null");
        retentionPolicy = retentionPolicy == null ? MaterializedJobRetentionPolicy.DELETE_ON_SUCCESS : retentionPolicy;
    }

    public TestJobExecutionSessionConfiguration(Path materializedJobsRoot,
                                                TestJobExecutionTarget target,
                                                TestJobBackendContext backendContext,
                                                TestJobAssertionRegistry assertionRegistry,
                                                TestFixtureRegistry fixtureRegistry) {
        this(materializedJobsRoot, target, backendContext, new DefaultTestJobMaterializer(),
                assertionRegistry, fixtureRegistry, MaterializedJobRetentionPolicy.DELETE_ON_SUCCESS);
    }
}
