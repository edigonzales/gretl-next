package ch.so.agi.gretl.internal.duckdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

record PostgresTableMetadata(Map<String, Column> columns, Map<String, GeometryColumn> geometryColumns) {
    PostgresTableMetadata {
        columns = Map.copyOf(columns);
        geometryColumns = Map.copyOf(geometryColumns);
    }

    static PostgresTableMetadata discover(Connection connection, String schema, String table,
            boolean autoDetectGeometry) throws SQLException {
        Map<String, Column> columns = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT column_name, data_type, udt_name
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ?
                ORDER BY ordinal_position
                """)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Column column = new Column(
                            rows.getString("column_name"),
                            rows.getString("data_type"),
                            rows.getString("udt_name"));
                    columns.put(normalize(column.name()), column);
                }
            }
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("PostgreSQL table does not exist or has no columns: "
                    + schema + "." + table);
        }

        Map<String, GeometryColumn> geometries = new LinkedHashMap<>();
        if (autoDetectGeometry) {
            discoverGeometryColumns(connection, schema, table, geometries, false);
            discoverGeometryColumns(connection, schema, table, geometries, true);
        }
        return new PostgresTableMetadata(columns, geometries);
    }

    Column requireColumn(String name) {
        Column column = columns.get(normalize(name));
        if (column == null) {
            throw new IllegalArgumentException("PostgreSQL column does not exist: " + name);
        }
        return column;
    }

    GeometryColumn geometry(String name) {
        return geometryColumns.get(normalize(name));
    }

    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static void discoverGeometryColumns(Connection connection, String schema, String table,
            Map<String, GeometryColumn> geometries, boolean geography) throws SQLException {
        String viewName = geography ? "geography_columns" : "geometry_columns";
        String columnName = geography ? "f_geography_column" : "f_geometry_column";
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT %s AS column_name, srid, type, coord_dimension
                FROM %s
                WHERE f_table_schema = ? AND f_table_name = ?
                """.formatted(columnName, viewName))) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    GeometryColumn geometry = new GeometryColumn(
                            rows.getString("column_name"),
                            rows.getInt("srid"),
                            rows.getString("type"),
                            rows.getInt("coord_dimension"),
                            geography);
                    geometries.put(normalize(geometry.name()), geometry);
                }
            }
        }
    }

    record Column(String name, String dataType, String udtName) {
    }

    record GeometryColumn(String name, int srid, String type, int dimension, boolean geography) {
    }
}
