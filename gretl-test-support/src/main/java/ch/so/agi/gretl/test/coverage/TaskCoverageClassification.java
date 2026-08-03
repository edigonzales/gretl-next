package ch.so.agi.gretl.test.coverage;

import java.util.Locale;

public enum TaskCoverageClassification {
    DIRECT_JOB_EXECUTION,
    STRUCTURAL_CONTRACT_ONLY,
    DEPENDENCY_PRESENT_ONLY,
    NO_CANONICAL_JOB_TRACE,
    NOT_APPLICABLE;

    public static TaskCoverageClassification fromYaml(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("coverage classification must not be blank");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown coverage classification: " + value, e);
        }
    }
}
