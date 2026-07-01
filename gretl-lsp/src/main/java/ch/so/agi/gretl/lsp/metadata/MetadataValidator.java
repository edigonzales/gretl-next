package ch.so.agi.gretl.lsp.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MetadataValidator {

    public List<MetadataProblem> validate(GretlMetadata metadata) {
        List<MetadataProblem> problems = new ArrayList<>();
        if (metadata.tasks().isEmpty()) {
            problems.add(new MetadataProblem("metadata", "No tasks found in metadata"));
            return problems;
        }

        Set<String> taskNames = new HashSet<>();
        for (TaskMetadata task : metadata.tasks()) {
            if (!taskNames.add(task.name())) {
                problems.add(new MetadataProblem(task.name(), "Duplicate task name"));
            }
            validateTask(task, problems);
        }
        return problems;
    }

    private void validateTask(TaskMetadata task, List<MetadataProblem> problems) {
        if (task.name() == null || task.name().isBlank()) {
            problems.add(new MetadataProblem(task.qualifiedClassName(), "Task has no name"));
        }
        if (task.description() == null || task.description().isBlank()) {
            problems.add(new MetadataProblem(task.name(), "Task has no description"));
        }
        if (task.status() == null) {
            problems.add(new MetadataProblem(task.name(), "Task has no status"));
        }

        Set<String> propNames = new HashSet<>();
        for (PropertyMetadata prop : task.properties()) {
            if (!propNames.add(prop.name())) {
                problems.add(new MetadataProblem(task.name(), "Duplicate property name: " + prop.name()));
            }
            validateProperty(task.name(), prop, problems);
        }
    }

    private void validateProperty(String taskName, PropertyMetadata prop, List<MetadataProblem> problems) {
        if (prop.name() == null || prop.name().isBlank()) {
            problems.add(new MetadataProblem(taskName, "Property has no name"));
        }
        if (prop.kind() == null) {
            problems.add(new MetadataProblem(taskName, "Property " + prop.name() + " has no kind"));
        }
        if (prop.required() && prop.acceptedForms().isEmpty()) {
            problems.add(new MetadataProblem(taskName, "Required property " + prop.name() + " has no accepted forms"));
        }
        if (prop.deprecated() && prop.migration() == null) {
            problems.add(new MetadataProblem(taskName, "Deprecated property " + prop.name() + " has no migration info"));
        }
    }

    public record MetadataProblem(String target, String message) {
    }
}
