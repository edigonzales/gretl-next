package ch.so.agi.gretl.test.fixture;

import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.Network;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestFixtureNetworkManagerTest {
    @Test
    void doesNotCreateNetworkOnConstructionOrCurrent() {
        AtomicInteger calls = new AtomicInteger();
        TestFixtureNetworkManager manager = new TestFixtureNetworkManager(() -> {
            calls.incrementAndGet();
            throw new AssertionError("network must remain lazy");
        });
        assertFalse(manager.isCreated());
        assertTrue(manager.current().isEmpty());
        assertTrue(manager.currentNetworkId().isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void createsNetworkOnlyOnRequireAndAtMostOnce() {
        AtomicInteger calls = new AtomicInteger();
        TestFixtureNetwork network = new TestFixtureNetwork(new Network() {
            @Override public String getId() { return "test-network"; }
            @Override public void close() { }
            @Override public Statement apply(Statement base, Description description) { return base; }
        });
        TestFixtureNetworkManager manager = new TestFixtureNetworkManager(() -> {
            calls.incrementAndGet();
            return network;
        });
        assertEquals(network, manager.require());
        assertEquals(network, manager.require());
        assertEquals(1, calls.get());
        assertEquals("test-network", manager.currentNetworkId().orElseThrow());
    }

    @Test
    void closeWithoutCreationIsIdempotentAndRequireAfterCloseFails() {
        AtomicInteger calls = new AtomicInteger();
        TestFixtureNetworkManager manager = new TestFixtureNetworkManager(() -> {
            calls.incrementAndGet();
            throw new AssertionError("network must not be created");
        });
        manager.close();
        manager.close();
        assertThrows(IllegalStateException.class, manager::require);
        assertEquals(0, calls.get());
    }

    @Test
    void factoryFailureDoesNotLeaveCreatedState() {
        AtomicInteger calls = new AtomicInteger();
        TestFixtureNetworkManager manager = new TestFixtureNetworkManager(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("factory failed");
        });
        assertThrows(IllegalStateException.class, manager::require);
        assertThrows(IllegalStateException.class, manager::require);
        assertFalse(manager.isCreated());
        assertTrue(manager.current().isEmpty());
        assertEquals(1, calls.get());
    }
}
