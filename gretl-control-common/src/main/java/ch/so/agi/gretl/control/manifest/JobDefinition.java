package ch.so.agi.gretl.control.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobDefinition(
        String id,
        String name,
        String projectDir,
        List<String> tasks,
        Boolean enabled,
        String cron,
        String timezone,
        OverlapPolicy overlapPolicy,
        String timeout,
        List<String> workerLabels,
        JvmDefinition jvm,
        List<ParameterDefinition> parameters,
        List<String> secretRefs,
        List<TriggerDefinition> triggers,
        List<NotificationDefinition> notifications) {

    public JobDefinition {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        workerLabels = workerLabels == null ? List.of() : List.copyOf(workerLabels);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        secretRefs = secretRefs == null ? List.of() : List.copyOf(secretRefs);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
        jvm = jvm == null ? new JvmDefinition(null, List.of()) : jvm;
        overlapPolicy = overlapPolicy == null ? OverlapPolicy.SKIP : overlapPolicy;
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }
}
