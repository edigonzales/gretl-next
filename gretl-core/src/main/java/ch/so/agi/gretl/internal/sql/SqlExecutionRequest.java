package ch.so.agi.gretl.internal.sql;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SqlExecutionRequest(
        String taskName,
        DatabaseSpec database,
        List<Path> sqlFiles,
        List<Map<String, String>> parameterSets
) {

    public SqlExecutionRequest {
        if (taskName == null || taskName.isBlank()) {
            taskName = "SqlExecutor";
        }
        if (database == null) {
            throw new IllegalArgumentException("database must not be null");
        }
        if (sqlFiles == null || sqlFiles.isEmpty()) {
            throw new IllegalArgumentException("sqlFiles must not be empty");
        }
        sqlFiles = List.copyOf(sqlFiles);
        parameterSets = normalizeParameterSets(parameterSets);
    }

    private static List<Map<String, String>> normalizeParameterSets(List<Map<String, String>> parameterSets) {
        if (parameterSets == null || parameterSets.isEmpty()) {
            return List.of(Map.of());
        }

        return parameterSets.stream()
                .map(SqlExecutionRequest::copyParameterSet)
                .toList();
    }

    private static Map<String, String> copyParameterSet(Map<String, String> parameterSet) {
        if (parameterSet == null) {
            throw new IllegalArgumentException("parameterSets must not contain null entries");
        }
        return Map.copyOf(new LinkedHashMap<>(parameterSet));
    }
}
