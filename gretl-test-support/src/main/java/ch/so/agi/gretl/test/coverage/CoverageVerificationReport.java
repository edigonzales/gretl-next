package ch.so.agi.gretl.test.coverage;

import java.util.List;

public record CoverageVerificationReport(
        List<String> errors,
        List<String> directJobExecution,
        List<String> structuralContractOnly,
        List<String> dependencyPresentOnly,
        List<String> noCanonicalJobTrace,
        List<String> notApplicable,
        List<String> missingBackendExecutions) {
    public CoverageVerificationReport {
        errors = List.copyOf(errors);
        directJobExecution = List.copyOf(directJobExecution);
        structuralContractOnly = List.copyOf(structuralContractOnly);
        dependencyPresentOnly = List.copyOf(dependencyPresentOnly);
        noCanonicalJobTrace = List.copyOf(noCanonicalJobTrace);
        notApplicable = List.copyOf(notApplicable);
        missingBackendExecutions = List.copyOf(missingBackendExecutions);
    }

    public boolean successful() {
        return errors.isEmpty();
    }

    public void throwIfFailed() {
        if (!successful()) throw new AssertionError("Task coverage verification failed: " + errors);
    }
}
