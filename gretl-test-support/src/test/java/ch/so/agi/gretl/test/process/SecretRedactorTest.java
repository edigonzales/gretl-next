package ch.so.agi.gretl.test.process;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SecretRedactorTest {
    private final SecretRedactor redactor = new SecretRedactor();

    @Test
    void redactsOverlappingSecretsLongestFirst() {
        assertEquals("url=***&short=***", redactor.redact(
                "url=superSecret&short=super", Set.of("super", "superSecret")));
    }

    @Test
    void redactsEncodedSecretAndIgnoresEmptyValues() {
        String value = "jdbc://user:p%40ss@example/db?password=p@ss";
        assertEquals("jdbc://user:***@example/db?password=***", redactor.redact(value, Set.of("", "p@ss")));
    }

    @Test
    void doesNotMutateInputList() {
        List<String> original = List.of("secret-value");
        List<String> redacted = redactor.redact(original, Set.of("secret-value"));
        assertEquals(List.of("secret-value"), original);
        assertFalse(redacted.contains("secret-value"));
    }
}
