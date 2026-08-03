package ch.so.agi.gretl.test.coverage;

import ch.so.agi.gretl.test.execution.GretlTaskOutcome;
import ch.so.agi.gretl.test.job.ExpectedTaskExecution;
import ch.so.agi.gretl.test.job.TestJobCatalog;
import ch.so.agi.gretl.test.job.TestJobDescriptor;
import ch.so.agi.gretl.test.job.TestJobExecutionRequirement;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;
import ch.so.agi.gretl.test.trace.TaskExecutionTraceEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public final class TaskCoverageVerifier {
    private static final Set<GretlTaskOutcome> POSITIVE_OUTCOMES = Set.of(
            GretlTaskOutcome.SUCCESS, GretlTaskOutcome.UP_TO_DATE, GretlTaskOutcome.FROM_CACHE);

    public CoverageVerificationReport verify(
            TaskCoverageManifest manifest,
            TestJobCatalog catalog,
            Collection<TaskExecutionTrace> traces,
            Set<String> publicTaskClasses) {
        List<String> errors = new ArrayList<>();
        List<String> missingBackends = new ArrayList<>();
        List<String> direct = new ArrayList<>();
        List<String> structural = new ArrayList<>();
        List<String> dependency = new ArrayList<>();
        List<String> notYet = new ArrayList<>();
        List<String> notApplicable = new ArrayList<>();
        Set<String> classes = Set.copyOf(publicTaskClasses);
        Set<String> seenClasses = new HashSet<>();

        for (String className : classes) {
            if (manifest.findByClassName(className).isEmpty()) {
                errors.add("Public task has no coverage entry: " + className);
            }
        }
        for (TaskCoverageEntry entry : manifest.entries()) {
            if (!seenClasses.add(entry.className())) {
                errors.add("Coverage class has multiple matrix entries: " + entry.className());
            }
            if (!classes.contains(entry.className())) {
                errors.add("Coverage entry references a non-public task class: " + entry.className());
            }
            switch (entry.classification()) {
                case DIRECT_JOB_EXECUTION -> direct.add(entry.className());
                case STRUCTURAL_CONTRACT_ONLY -> structural.add(entry.className());
                case DEPENDENCY_PRESENT_ONLY -> dependency.add(entry.className());
                case NOT_YET_COVERED -> notYet.add(entry.className());
                case NOT_APPLICABLE -> notApplicable.add(entry.className());
            }
            if (entry.classification() == TaskCoverageClassification.DIRECT_JOB_EXECUTION
                    && entry.scenarios().isEmpty()) {
                errors.add("DIRECT_JOB_EXECUTION has no scenario: " + entry.className());
            }
            if (entry.classification() != TaskCoverageClassification.DIRECT_JOB_EXECUTION
                    && entry.scenarios().size() > 0) {
                errors.add("Non-direct coverage entry has an execution scenario: " + entry.className());
            }
            if ((entry.classification() == TaskCoverageClassification.NOT_APPLICABLE
                    || entry.classification() == TaskCoverageClassification.STRUCTURAL_CONTRACT_ONLY
                    || entry.classification() == TaskCoverageClassification.DEPENDENCY_PRESENT_ONLY)
                    && entry.reason().isBlank()) {
                errors.add("Coverage classification requires a reason: " + entry.className());
            }
            for (TaskCoverageScenario scenario : entry.scenarios()) {
                verifyScenario(entry, scenario, catalog, traces, errors, missingBackends);
            }
        }
        return new CoverageVerificationReport(errors, direct, structural, dependency, notYet,
                notApplicable, missingBackends);
    }

    private void verifyScenario(TaskCoverageEntry entry, TaskCoverageScenario scenario,
                                TestJobCatalog catalog, Collection<TaskExecutionTrace> traces,
                                List<String> errors, List<String> missingBackends) {
        TestJobDescriptor job = catalog.find(scenario.jobId()).orElse(null);
        if (job == null) {
            errors.add("Coverage scenario references an unknown job: " + scenario.jobId());
            return;
        }
        ExpectedTaskExecution expected = job.expectedTasks().stream()
                .filter(task -> task.path().equals(scenario.taskPath())).findFirst().orElse(null);
        if (expected == null) {
            errors.add("Coverage scenario task path is not expected by job " + scenario.jobId()
                    + ": " + scenario.taskPath());
        } else if (!expected.className().equals(entry.className())) {
            errors.add("Coverage class mismatch for " + scenario.jobId() + " " + scenario.taskPath()
                    + ": matrix=" + entry.className() + ", job=" + expected.className());
        }
        for (TestJobExecutionTarget target : TestJobExecutionTarget.values()) {
            if (!scenario.targets().containsKey(target)) {
                errors.add("Coverage scenario has no target requirement for " + target.yamlName()
                        + ": " + entry.className() + " / " + scenario.jobId());
            } else if (scenario.requirementFor(target) == TestJobExecutionRequirement.REQUIRED
                    && job.requirementFor(target) == TestJobExecutionRequirement.NOT_APPLICABLE) {
                errors.add("Coverage scenario requires a target marked not-applicable by job "
                        + scenario.jobId() + ": " + target.yamlName());
            }
        }
        for (TestJobExecutionTarget target : TestJobExecutionTarget.values()) {
            TestJobExecutionRequirement requirement = scenario.requirementFor(target);
            List<TaskExecutionTraceEntry> present = traces.stream().flatMap(trace -> trace.entries().stream())
                    .filter(trace -> identifies(trace, scenario, target))
                    .toList();
            boolean found = present.stream().anyMatch(trace -> matches(trace, entry.className()));
            if (requirement == TestJobExecutionRequirement.REQUIRED && !found) {
                String missing = entry.className() + " / " + scenario.jobId() + " / "
                        + scenario.taskPath() + " / " + target.yamlName();
                missingBackends.add(missing);
                errors.add("Missing positive trace for " + missing);
            } else if (requirement == TestJobExecutionRequirement.OPTIONAL
                    && !present.isEmpty() && !found) {
                errors.add("Present optional trace is not a positive "
                        + entry.className() + " trace for " + scenario.jobId() + " / "
                        + scenario.taskPath() + " / " + target.yamlName());
            }
        }
    }

    private boolean identifies(TaskExecutionTraceEntry trace, TaskCoverageScenario scenario,
                               TestJobExecutionTarget target) {
        return trace.backend() == target
                && trace.jobId().equals(scenario.jobId())
                && trace.taskPath().equals(scenario.taskPath());
    }

    private boolean matches(TaskExecutionTraceEntry trace, String className) {
        return trace.taskClassName().equals(className)
                && POSITIVE_OUTCOMES.contains(trace.outcome());
    }
}
