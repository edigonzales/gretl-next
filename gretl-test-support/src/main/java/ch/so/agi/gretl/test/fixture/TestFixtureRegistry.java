package ch.so.agi.gretl.test.fixture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TestFixtureRegistry implements AutoCloseable {
    private final EnumMap<TestFixtureType, TestFixture> fixtures;
    private boolean closed;

    public TestFixtureRegistry(Collection<? extends TestFixture> values) {
        fixtures = new EnumMap<>(TestFixtureType.class);
        Objects.requireNonNull(values, "values must not be null").forEach(fixture -> {
            Objects.requireNonNull(fixture, "fixture must not be null");
            if (fixtures.putIfAbsent(fixture.type(), fixture) != null) {
                throw new IllegalArgumentException("Duplicate fixture type: " + fixture.type());
            }
        });
    }

    public TestFixtureRegistry(Map<TestFixtureType, ? extends TestFixture> values) {
        this(values.values());
    }

    public synchronized TestFixture require(TestFixtureType type) {
        if (closed) throw new IllegalStateException("Fixture registry is closed");
        TestFixture fixture = fixtures.get(type);
        if (fixture == null) throw new IllegalArgumentException("No fixture registered for " + type);
        return fixture;
    }

    public synchronized void startRequired(Collection<TestFixtureType> types,
                                            TestFixtureNetworkManager networkManager) {
        if (closed) throw new IllegalStateException("Fixture registry is closed");
        Objects.requireNonNull(types, "types must not be null");
        Objects.requireNonNull(networkManager, "networkManager must not be null");
        List<TestFixture> started = new ArrayList<>();
        try {
            List<TestFixture> required = types.stream().distinct().map(this::require).toList();
            boolean needsNetwork = required.stream().anyMatch(TestFixture::requiresDockerNetwork);
            TestFixtureStartContext context = new TestFixtureStartContext(
                    needsNetwork ? java.util.Optional.of(networkManager.require()) : java.util.Optional.empty());
            for (TestFixture fixture : required) {
                if (!fixture.isRunning()) {
                    fixture.start(context);
                    started.add(fixture);
                }
            }
        } catch (RuntimeException failure) {
            for (int i = started.size() - 1; i >= 0; i--) {
                try { started.get(i).close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        List<TestFixture> values = new ArrayList<>(fixtures.values());
        for (int i = values.size() - 1; i >= 0; i--) {
            try { values.get(i).close(); }
            catch (RuntimeException e) {
                if (failure == null) failure = new IllegalStateException("Fixture cleanup failed");
                failure.addSuppressed(e);
            }
        }
        if (failure != null) throw failure;
    }
}
