package ch.so.agi.gretl.test.job;

import java.util.Objects;

public record ExpectedTaskExecution(String path, String className) {
    public ExpectedTaskExecution {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(className, "className must not be null");
    }
}
