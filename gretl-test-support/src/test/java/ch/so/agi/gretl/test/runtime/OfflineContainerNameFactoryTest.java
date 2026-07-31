package ch.so.agi.gretl.test.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineContainerNameFactoryTest {
    @Test
    void createsDeterministicDockerSafeNames() {
        OfflineContainerNameFactory factory = new OfflineContainerNameFactory();
        String first = factory.create("project with spaces");
        String second = factory.create("project with spaces");

        assertEquals(first, second);
        assertTrue(first.matches("gretl-offline-[a-z0-9-]+-[0-9a-f]{8}"));
        assertTrue(first.length() <= 127);
    }
}
