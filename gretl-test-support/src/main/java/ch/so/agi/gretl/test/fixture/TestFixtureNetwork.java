package ch.so.agi.gretl.test.fixture;

import org.testcontainers.containers.Network;

import java.util.Objects;

public final class TestFixtureNetwork implements AutoCloseable {
    private final Network network;
    private boolean closed;

    TestFixtureNetwork(Network network) {
        this.network = Objects.requireNonNull(network, "network must not be null");
    }

    public static TestFixtureNetwork create() {
        return new TestFixtureNetwork(Network.newNetwork());
    }

    public synchronized Network testcontainersNetwork() {
        requireOpen();
        return network;
    }

    public synchronized String dockerNetworkId() {
        requireOpen();
        return network.getId();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        network.close();
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Fixture network is closed");
    }
}
