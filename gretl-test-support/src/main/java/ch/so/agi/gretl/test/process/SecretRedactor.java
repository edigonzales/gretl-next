package ch.so.agi.gretl.test.process;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SecretRedactor {
    public static final String MASK = "***";

    public String redact(String value, Set<String> secrets) {
        if (value == null || value.isEmpty() || secrets == null || secrets.isEmpty()) {
            return value;
        }
        String redacted = value;
        List<String> candidates = secrets.stream()
                .filter(Objects::nonNull)
                .filter(secret -> !secret.isEmpty())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        for (String secret : candidates) {
            redacted = redacted.replace(secret, MASK);
            String encoded = percentEncode(secret);
            if (!encoded.equals(secret)) {
                redacted = redacted.replace(encoded, MASK);
            }
        }
        return redacted;
    }

    public List<String> redact(List<String> values, Set<String> secrets) {
        Objects.requireNonNull(values, "values must not be null");
        List<String> redacted = new ArrayList<>(values.size());
        for (String value : values) {
            redacted.add(redact(value, secrets));
        }
        return List.copyOf(redacted);
    }

    private String percentEncode(String value) {
        StringBuilder result = new StringBuilder();
        for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
            int unsigned = valueByte & 0xff;
            if ((unsigned >= 'a' && unsigned <= 'z')
                    || (unsigned >= 'A' && unsigned <= 'Z')
                    || (unsigned >= '0' && unsigned <= '9')
                    || unsigned == '-' || unsigned == '.' || unsigned == '_' || unsigned == '~') {
                result.append((char) unsigned);
            } else {
                result.append('%');
                result.append(String.format("%02X", unsigned));
            }
        }
        return result.toString();
    }
}
