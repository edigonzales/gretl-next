package ch.so.agi.gretl.test.fixture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PreparedTestJobEnvironment implements AutoCloseable {
    private final TestJobEnvironment environment;
    private final Map<String, TestFixtureLease> leases;
    private boolean closed;

    public PreparedTestJobEnvironment(TestJobEnvironment environment,
                                      Map<String, ? extends TestFixtureLease> leases) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
        this.leases = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(leases, "leases must not be null")));
    }

    public TestJobEnvironment environment() { return environment; }

    public Map<String, TestFixtureLease> leases() { return leases; }

    public synchronized <T extends TestFixtureLease> T requireLease(String id, Class<T> type) {
        if (closed) throw new IllegalStateException("Prepared fixture environment is closed");
        TestFixtureLease lease = leases.get(id);
        if (lease == null) throw new IllegalArgumentException("No fixture lease for '" + id + "'");
        if (!type.isInstance(lease)) throw new IllegalArgumentException("Fixture lease '" + id
                + "' is not a " + type.getName());
        return type.cast(lease);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        var values = new ArrayList<>(leases.values());
        for (int i = values.size() - 1; i >= 0; i--) {
            try { values.get(i).close(); }
            catch (RuntimeException e) {
                if (failure == null) failure = new IllegalStateException("Fixture lease cleanup failed");
                failure.addSuppressed(e);
            }
        }
        if (failure != null) throw failure;
    }
}
