package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DuckDbExecutionRequest(
        String taskName,
        Path databaseFile,
        boolean inMemory,
        boolean installExtensions,
        List<DuckDbSourceSpec> sources,
        List<Path> sqlFiles,
        List<Map<String, String>> parameterSets,
        List<DuckDbExportSpec> exports
) {
    public DuckDbExecutionRequest {
        if (taskName == null || taskName.isBlank()) {
            taskName = "DuckDbSqlExecutor";
        }
        if (databaseFile == null && !inMemory) {
            throw new IllegalArgumentException("database file or inMemoryDatabase() must be configured");
        }
        if (databaseFile != null && inMemory) {
            throw new IllegalArgumentException("Use either database file(...) or inMemoryDatabase(), not both");
        }
        if (sqlFiles == null || sqlFiles.isEmpty()) {
            throw new IllegalArgumentException("sqlFiles must not be empty");
        }
        sources = List.copyOf(sources == null ? List.of() : sources);
        sqlFiles = List.copyOf(sqlFiles);
        parameterSets = normalizeParameterSets(parameterSets);
        exports = List.copyOf(exports == null ? List.of() : exports);
    }

    public String jdbcUrl() {
        if (inMemory) {
            return "jdbc:duckdb:";
        }
        return "jdbc:duckdb:" + databaseFile.toAbsolutePath();
    }

    private static List<Map<String, String>> normalizeParameterSets(List<Map<String, String>> parameterSets) {
        if (parameterSets == null || parameterSets.isEmpty()) {
            return List.of(Map.of());
        }
        return parameterSets.stream()
                .map(parameterSet -> Map.copyOf(new LinkedHashMap<>(parameterSet)))
                .toList();
    }
}
