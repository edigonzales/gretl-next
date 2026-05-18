package ch.so.agi.gretl.control.manifest;

import org.quartz.CronExpression;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ManifestValidator {
    public void validate(GretlServerManifest manifest) {
        if (manifest == null) {
            throw new ManifestException("Manifest must not be null.");
        }
        Set<String> jobIds = new HashSet<>();
        for (JobDefinition job : manifest.jobs()) {
            validateJob(job, jobIds);
        }
        for (JobDefinition job : manifest.jobs()) {
            for (TriggerDefinition trigger : job.triggers()) {
                if (isBlank(trigger.jobId())) {
                    throw new ManifestException("Job '" + job.id() + "' has a trigger without jobId.");
                }
                if (!jobIds.contains(trigger.jobId())) {
                    throw new ManifestException("Job '" + job.id() + "' references unknown trigger job '" + trigger.jobId() + "'.");
                }
                if (trigger.on() == null) {
                    throw new ManifestException("Job '" + job.id() + "' has a trigger without event.");
                }
            }
        }
    }

    public Map<String, Object> normalizeParameters(JobDefinition job, Map<String, Object> provided) {
        Map<String, Object> input = provided == null ? Map.of() : provided;
        java.util.LinkedHashMap<String, Object> normalized = new java.util.LinkedHashMap<>();
        Set<String> known = new HashSet<>();
        for (ParameterDefinition parameter : job.parameters()) {
            known.add(parameter.name());
            Object value = input.containsKey(parameter.name()) ? input.get(parameter.name()) : parameter.defaultValue();
            if (value == null && parameter.isRequired()) {
                throw new ManifestException("Missing required parameter '" + parameter.name() + "' for job '" + job.id() + "'.");
            }
            if (value != null) {
                normalized.put(parameter.name(), coerceValue(job, parameter, value));
            }
        }
        for (String name : input.keySet()) {
            if (!known.contains(name)) {
                throw new ManifestException("Unknown parameter '" + name + "' for job '" + job.id() + "'.");
            }
        }
        return normalized;
    }

    public long timeoutSeconds(JobDefinition job) {
        if (isBlank(job.timeout())) {
            return Duration.ofHours(1).toSeconds();
        }
        try {
            return Duration.parse(job.timeout()).toSeconds();
        } catch (DateTimeParseException e) {
            throw new ManifestException("Job '" + job.id() + "' has invalid ISO-8601 timeout '" + job.timeout() + "'.", e);
        }
    }

    private void validateJob(JobDefinition job, Set<String> jobIds) {
        if (job == null) {
            throw new ManifestException("Manifest contains an empty job entry.");
        }
        require(job.id(), "Job id is required.");
        if (!jobIds.add(job.id())) {
            throw new ManifestException("Duplicate job id '" + job.id() + "'.");
        }
        require(job.projectDir(), "Job '" + job.id() + "' requires projectDir.");
        if (job.tasks().isEmpty()) {
            throw new ManifestException("Job '" + job.id() + "' requires at least one task.");
        }
        if (!isBlank(job.cron()) && !CronExpression.isValidExpression(job.cron())) {
            throw new ManifestException("Job '" + job.id() + "' has invalid cron expression '" + job.cron() + "'.");
        }
        if (!isBlank(job.timezone())) {
            try {
                ZoneId.of(job.timezone());
            } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
                throw new ManifestException("Job '" + job.id() + "' has invalid timezone '" + job.timezone() + "'.", e);
            }
        }
        timeoutSeconds(job);
        Set<String> parameterNames = new HashSet<>();
        for (ParameterDefinition parameter : job.parameters()) {
            validateParameter(job, parameter, parameterNames);
        }
    }

    private void validateParameter(JobDefinition job, ParameterDefinition parameter, Set<String> parameterNames) {
        if (parameter == null || isBlank(parameter.name())) {
            throw new ManifestException("Job '" + job.id() + "' contains a parameter without name.");
        }
        if (!parameterNames.add(parameter.name())) {
            throw new ManifestException("Job '" + job.id() + "' contains duplicate parameter '" + parameter.name() + "'.");
        }
        if (parameter.type() == null) {
            throw new ManifestException("Job '" + job.id() + "' parameter '" + parameter.name() + "' requires type.");
        }
        if (parameter.defaultValue() != null) {
            coerceValue(job, parameter, parameter.defaultValue());
        }
    }

    private Object coerceValue(JobDefinition job, ParameterDefinition parameter, Object value) {
        return switch (parameter.type()) {
            case STRING -> value.toString();
            case INTEGER -> coerceInteger(job, parameter, value);
            case BOOLEAN -> coerceBoolean(job, parameter, value);
        };
    }

    private Integer coerceInteger(JobDefinition job, ParameterDefinition parameter, Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new ManifestException("Job '" + job.id() + "' parameter '" + parameter.name() + "' requires integer value.", e);
        }
    }

    private Boolean coerceBoolean(JobDefinition job, ParameterDefinition parameter, Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = value.toString().toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "false".equals(text)) {
            return Boolean.parseBoolean(text);
        }
        throw new ManifestException("Job '" + job.id() + "' parameter '" + parameter.name() + "' requires boolean value.");
    }

    private void require(String value, String message) {
        if (isBlank(value)) {
            throw new ManifestException(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
