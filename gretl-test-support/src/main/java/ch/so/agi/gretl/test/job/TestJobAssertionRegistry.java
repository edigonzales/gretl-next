package ch.so.agi.gretl.test.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class TestJobAssertionRegistry {
    private final Map<String, TestJobAssertions> assertions = new HashMap<>();

    public TestJobAssertionRegistry(Collection<TestJobAssertions> values) {
        values.forEach(value -> {
            if (assertions.putIfAbsent(value.id(), value) != null) throw new IllegalArgumentException("Duplicate test job assertion id: " + value.id());
        });
    }

    public TestJobAssertions require(String id) {
        TestJobAssertions value = assertions.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown test job assertion id: " + id);
        return value;
    }

    public boolean contains(String id) { return assertions.containsKey(id); }
}
