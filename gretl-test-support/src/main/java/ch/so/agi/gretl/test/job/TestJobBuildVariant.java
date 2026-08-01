package ch.so.agi.gretl.test.job;

import java.util.Objects;

public record TestJobBuildVariant(String id, String file, TestJobBuildLanguage language) {
    public TestJobBuildVariant {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(language, "language must not be null");
    }
}
