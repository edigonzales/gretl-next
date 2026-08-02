package ch.so.agi.gretl.test.job;

import java.util.Objects;
import java.util.Optional;

public record TestJobExecutionDeclaration(
        TestJobExecutionRequirement requirement,
        Optional<String> reason) {
    public TestJobExecutionDeclaration {
        Objects.requireNonNull(requirement, "requirement must not be null");
        reason = reason == null ? Optional.empty() : reason;
        reason.ifPresent(value -> {
            if (value.isBlank()) throw new IllegalArgumentException("reason must not be blank");
        });
        if (requirement == TestJobExecutionRequirement.NOT_APPLICABLE && reason.isEmpty()) {
            throw new IllegalArgumentException("NOT_APPLICABLE requires a reason");
        }
    }

    public static TestJobExecutionDeclaration required() {
        return new TestJobExecutionDeclaration(TestJobExecutionRequirement.REQUIRED, Optional.empty());
    }

    public static TestJobExecutionDeclaration optional(String reason) {
        return new TestJobExecutionDeclaration(TestJobExecutionRequirement.OPTIONAL,
                Optional.ofNullable(reason).filter(value -> !value.isBlank()));
    }

    public static TestJobExecutionDeclaration notApplicable(String reason) {
        return new TestJobExecutionDeclaration(TestJobExecutionRequirement.NOT_APPLICABLE,
                Optional.ofNullable(reason));
    }
}
