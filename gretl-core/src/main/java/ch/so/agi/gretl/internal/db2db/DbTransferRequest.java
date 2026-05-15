package ch.so.agi.gretl.internal.db2db;

import ch.so.agi.gretl.internal.sql.DatabaseSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DbTransferRequest(
        String taskName,
        DatabaseSpec sourceDatabase,
        DatabaseSpec targetDatabase,
        List<DbTransferSpec> transfers,
        List<Map<String, String>> parameterSets,
        int batchSize,
        int fetchSize
) {

    public DbTransferRequest {
        if (taskName == null || taskName.isBlank()) {
            taskName = "Db2Db";
        }
        if (sourceDatabase == null) {
            throw new IllegalArgumentException("sourceDatabase must not be null");
        }
        if (targetDatabase == null) {
            throw new IllegalArgumentException("targetDatabase must not be null");
        }
        if (transfers == null || transfers.isEmpty()) {
            throw new IllegalArgumentException("transfers must not be empty");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        if (fetchSize < 0) {
            throw new IllegalArgumentException("fetchSize must be greater than or equal to zero");
        }
        transfers = List.copyOf(transfers);
        parameterSets = normalizeParameterSets(parameterSets);
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
