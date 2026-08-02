package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobDescriptor;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TestJobFixtureBindingResolver {
    public TestJobEnvironment resolve(TestJobDescriptor descriptor,
                                      TestJobExecutionTarget target,
                                      Map<String, TestFixtureLease> leases) {
        TestJobEnvironment result = TestJobEnvironment.empty();
        for (TestJobFixtureRequirement requirement : descriptor.fixtures()) {
            TestFixtureLease lease = leases.get(requirement.id());
            if (lease == null) throw new IllegalArgumentException("No lease for fixture '" + requirement.id() + "'");
            TestFixtureEndpointView view = lease.endpointView(target);
            TestJobEnvironment one = TestJobEnvironment.empty();
            for (TestJobFixtureBinding binding : requirement.bindings()) {
                TestFixtureValue value = view.require(binding.source());
                Map<String, String> property = binding.target() == TestJobBindingTarget.GRADLE_PROPERTY
                        ? Map.of(binding.name(), value.value()) : Map.of();
                Map<String, String> environment = binding.target() == TestJobBindingTarget.ENVIRONMENT_VARIABLE
                        ? Map.of(binding.name(), value.value()) : Map.of();
                Set<String> secrets = value.sensitivity() == TestFixtureValueSensitivity.SECRET
                        ? Set.of(value.value()) : Set.of();
                one = one.merge(new TestJobEnvironment(property, environment, secrets,
                        view.dockerNetwork()));
            }
            result = result.merge(one);
        }
        return result;
    }
}
