package ch.so.agi.gretl.test.job;

import java.util.Map;
import java.util.Objects;

public record TestJobBuildVariant(
        String id,
        String file,
        Map<TestJobExecutionTarget, TestJobExecutionDeclaration> executionTargets) {
    public TestJobBuildVariant {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(file, "file must not be null");
        executionTargets = Map.copyOf(Objects.requireNonNull(executionTargets,
                "executionTargets must not be null"));
    }

    public TestJobBuildVariant(String id, String file) {
        this(id, file, Map.of());
    }

    public TestJobExecutionDeclaration declarationFor(TestJobExecutionTarget target) {
        return executionTargets.get(target);
    }

    public Map<TestJobExecutionTarget, TestJobExecutionDeclaration> executionTargets() {
        return Map.copyOf(executionTargets);
    }
}
