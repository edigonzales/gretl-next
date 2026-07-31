package ch.so.agi.gretl.test.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class OfflineContainerNameFactory {
    public String create(String displayName) {
        String readable = sanitize(displayName);
        String value = "gretl-offline-" + readable + "-" + shortHash(displayName);
        return value.substring(0, Math.min(127, value.length()));
    }

    String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "test";
        }
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (sanitized.isBlank()) {
            sanitized = "test";
        }
        return sanitized.substring(0, Math.min(80, sanitized.length()));
    }

    String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                result.append(String.format("%02x", digest[i]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("JDK does not provide SHA-256", e);
        }
    }
}
