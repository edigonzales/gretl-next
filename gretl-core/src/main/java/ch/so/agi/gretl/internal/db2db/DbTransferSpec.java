package ch.so.agi.gretl.internal.db2db;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record DbTransferSpec(
        Path sqlFile,
        TargetIdentifier targetTable,
        boolean deleteAllRows,
        Map<String, GeometryTransform> geometryColumns
) {

    public DbTransferSpec(Path sqlFile, String targetTable, boolean deleteAllRows, List<String> geometryColumnDefinitions) {
        this(sqlFile, TargetIdentifier.parse(targetTable), deleteAllRows, parseGeometryColumns(geometryColumnDefinitions));
    }

    public DbTransferSpec {
        if (sqlFile == null) {
            throw new IllegalArgumentException("sqlFile must not be null");
        }
        if (targetTable == null) {
            throw new IllegalArgumentException("targetTable must not be null");
        }
        geometryColumns = Map.copyOf(new LinkedHashMap<>(geometryColumns == null ? Map.of() : geometryColumns));
    }

    public boolean isGeometryColumn(String columnName) {
        return geometryColumns.containsKey(normalizeColumnName(columnName));
    }

    public String valueExpression(String columnName) {
        GeometryTransform transform = geometryColumns.get(normalizeColumnName(columnName));
        return transform == null ? "?" : transform.wrap("?");
    }

    public String inputSignature() {
        return sqlFile + "|" + targetTable.displayName() + "|" + deleteAllRows + "|" + geometryColumns.keySet();
    }

    private static Map<String, GeometryTransform> parseGeometryColumns(List<String> definitions) {
        Map<String, GeometryTransform> columns = new LinkedHashMap<>();
        if (definitions == null) {
            return columns;
        }
        for (String definition : definitions) {
            GeometryTransform transform = GeometryTransform.create(definition);
            columns.put(transform.columnNameUpperCase(), transform);
        }
        return columns;
    }

    private static String normalizeColumnName(String columnName) {
        if (columnName == null) {
            return "";
        }
        return columnName.toUpperCase(Locale.ROOT);
    }
}
