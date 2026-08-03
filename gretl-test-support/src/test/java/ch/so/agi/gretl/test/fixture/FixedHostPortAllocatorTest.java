package ch.so.agi.gretl.test.fixture;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixedHostPortAllocatorTest {
    @Test
    void retriesOnlyRecognizedPortCollisionsWithANewPort() {
        List<Integer> ports = new ArrayList<>();
        String result = new FixedHostPortAllocator().executeWithRetry(3, port -> {
            ports.add(port);
            if (ports.size() == 1) throw new IllegalStateException("port is already allocated");
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(2, ports.size());
        assertEquals(false, ports.get(0).equals(ports.get(1)));
    }

    @Test
    void doesNotRetryUnrelatedFailures() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new FixedHostPortAllocator().executeWithRetry(3,
                        port -> { throw new IllegalStateException("container image missing"); }));
        assertEquals("container image missing", failure.getMessage());
    }
}
