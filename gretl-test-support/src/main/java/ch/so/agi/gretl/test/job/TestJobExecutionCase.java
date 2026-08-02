package ch.so.agi.gretl.test.job;

public record TestJobExecutionCase(
        TestJobDescriptor descriptor,
        TestJobBuildVariant buildVariant,
        TestJobExecutionTarget target,
        TestJobExecutionRequirement requirement) {
}
