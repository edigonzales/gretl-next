package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobBuildVariant;
import ch.so.agi.gretl.test.job.TestJobDescriptor;
import ch.so.agi.gretl.test.job.TestJobExecutionId;
import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TestFixtureOrchestrator implements AutoCloseable {
    private final TestFixtureRegistry registry;
    private final TestFixtureNetworkManager networkManager;
    private final TestJobFixtureBindingResolver resolver;
    private boolean closed;

    public TestFixtureOrchestrator(TestFixtureRegistry registry, TestFixtureNetworkManager networkManager) {
        this.registry = registry;
        this.networkManager = networkManager;
        this.resolver = new TestJobFixtureBindingResolver();
    }

    public synchronized PreparedTestJobEnvironment prepare(TestJobDescriptor descriptor,
                                                            TestJobBuildVariant buildVariant,
                                                            TestJobExecutionTarget target,
                                                            TestJobExecutionId executionId) {
        if (closed) throw new IllegalStateException("Fixture orchestrator is closed");
        TestJobExecutionIdentity identity = new TestJobExecutionIdentity(descriptor.id(), buildVariant.id(), target,
                executionId);
        EnumSet<TestFixtureType> types = EnumSet.noneOf(TestFixtureType.class);
        for (TestJobFixtureRequirement requirement : descriptor.fixtures()) types.add(requirement.type());
        registry.startRequired(types, networkManager);
        Map<String, TestFixtureLease> leases = new LinkedHashMap<>();
        try {
            for (TestJobFixtureRequirement requirement : descriptor.fixtures()) {
                TestFixture fixture = registry.require(requirement.type());
                leases.put(requirement.id(), fixture.acquire(identity));
            }
            TestJobEnvironment environment = resolver.resolve(descriptor, target, leases);
            if ((target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                    || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE)
                    && networkManager.current().isPresent()) {
                environment = environment.merge(new TestJobEnvironment(Map.of(), Map.of(), java.util.Set.of(),
                        networkManager.currentNetworkId()));
            }
            return new PreparedTestJobEnvironment(environment, leases);
        } catch (RuntimeException failure) {
            for (TestFixtureLease lease : leases.values()) {
                try { lease.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }

    public Optional<TestFixtureNetwork> currentNetwork() { return networkManager.current(); }

    public TestFixtureRegistry registry() { return registry; }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        registry.close();
    }
}
