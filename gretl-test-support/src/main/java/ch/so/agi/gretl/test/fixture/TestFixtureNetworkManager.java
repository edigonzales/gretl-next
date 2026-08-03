package ch.so.agi.gretl.test.fixture;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class TestFixtureNetworkManager implements AutoCloseable {
    private final Supplier<TestFixtureNetwork> factory;
    private TestFixtureNetwork network;
    private Throwable factoryFailure;
    private boolean creationAttempted;
    private boolean closed;

    public TestFixtureNetworkManager(Supplier<TestFixtureNetwork> factory) {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    public synchronized TestFixtureNetwork require() {
        if (closed) throw new IllegalStateException("Fixture network manager is closed");
        if (factoryFailure != null) throwFactoryFailure(factoryFailure);
        if (network == null && !creationAttempted) {
            creationAttempted = true;
            try {
                network = Objects.requireNonNull(factory.get(), "Fixture network factory returned null");
            } catch (RuntimeException | Error failure) {
                factoryFailure = failure;
                throw failure;
            }
        }
        if (factoryFailure != null) throwFactoryFailure(factoryFailure);
        return network;
    }

    public synchronized Optional<TestFixtureNetwork> current() {
        return Optional.ofNullable(network);
    }

    public synchronized Optional<String> currentNetworkId() {
        return current().map(TestFixtureNetwork::dockerNetworkId);
    }

    public synchronized boolean isCreated() {
        return network != null;
    }

    private static void throwFactoryFailure(Throwable failure) {
        if (failure instanceof Error error) throw error;
        throw (RuntimeException) failure;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (network != null) network.close();
    }
}
