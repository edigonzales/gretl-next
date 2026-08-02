package ch.so.agi.gretl.test.job;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public record TestJobExecutionId(String value) {
    public TestJobExecutionId {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.matches("[a-z0-9][a-z0-9-]{7,127}")) {
            throw new IllegalArgumentException("Invalid test job execution id: " + value);
        }
    }

    public static TestJobExecutionId create(TestJobDescriptor descriptor,
                                             TestJobBuildVariant variant,
                                             TestJobExecutionTarget target) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(variant, "variant must not be null");
        Objects.requireNonNull(target, "target must not be null");
        String input = descriptor.id() + "\n" + variant.id() + "\n" + target.name()
                + "\n" + System.nanoTime() + "\n" + Thread.currentThread().getId();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return new TestJobExecutionId(descriptor.id() + "-" + target.name().toLowerCase().replace('_', '-') + "-"
                    + HexFormat.of().formatHex(digest, 0, 10));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public String shortToken() {
        return value.substring(Math.max(0, value.length() - 12));
    }
}
