package ch.so.agi.gretl.test.fixture;

import java.util.Optional;

public record TestFixtureStartContext(Optional<TestFixtureNetwork> network) {
    public TestFixtureStartContext {
        network = network == null ? Optional.empty() : network;
    }

    public TestFixtureNetwork requireNetwork(TestFixtureType type) {
        return network.orElseThrow(() -> new IllegalStateException(
                "Fixture " + type + " requires a Docker network"));
    }
}
