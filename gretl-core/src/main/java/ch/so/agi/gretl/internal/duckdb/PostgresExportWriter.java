package ch.so.agi.gretl.internal.duckdb;

import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.util.DbConnector;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PostgresExportWriter {
    private static final int BATCH_SIZE = 1000;

    private final GretlLogger log;

    PostgresExportWriter(GretlLogger log) {
        this.log = log;
    }

    void export(Connection duckdb, PostgresExportSpec export, Map<String, PostgresTargetSpec> targets,
            List<OpenTarget> openTargets) throws SQLException {
        PostgresTargetSpec target = targets.get(export.target());
        if (target == null) {
            throw new IllegalArgumentException("PostgreSQL export '" + export.name()
                    + "' references unknown target: " + export.target());
        }

        if (export.writePath() == PostgresWritePath.DUCKDB) {
            exportWithDuckDb(duckdb, export);
            return;
        }

        OpenTarget openTarget = openTarget(target, openTargets);
        exportWithJdbc(duckdb, openTarget.connection(), export);
    }

    private OpenTarget openTarget(PostgresTargetSpec target, List<OpenTarget> openTargets) throws SQLException {
        for (OpenTarget open : openTargets) {
            if (open.alias().equals(target.alias())) {
                return open;
            }
        }
        Connection connection = DbConnector.connect(target.database().jdbcUrl(),
                target.database().username(), target.database().password());
        connection.setAutoCommit(false);
        OpenTarget open = new OpenTarget(target.alias(), connection);
        openTargets.add(open);
        return open;
    }

    private void exportWithDuckDb(Connection duckdb, PostgresExportSpec export) throws SQLException {
        if (!export.geometries().isEmpty()) {
            throw new IllegalArgumentException("PostgreSQL export '" + export.name()
                    + "' uses geometry mappings. Use writePath = 'jdbc' for controlled PostGIS geometry writes.");
        }

        DuckDbSql.QualifiedTable table = DuckDbSql.parseQualifiedTable(export.table());
        String targetTable = DuckDbSql.quoteIdentifier(export.target()) + "." + table.toSql();
        String query = DuckDbSql.stripTrailingSemicolon(export.query());
        log.info("Writing PostgreSQL export " + export.name() + " via DuckDB postgres extension.");

        if (export.mode() == PostgresWriteMode.TRUNCATE) {
            execute(duckdb, "DELETE FROM " + targetTable);
        } else if (export.mode() == PostgresWriteMode.REPLACE) {
            execute(duckdb, "DROP TABLE IF EXISTS " + targetTable);
            execute(duckdb, "CREATE TABLE " + targetTable + " AS " + query);
            return;
        }

        execute(duckdb, "INSERT INTO " + targetTable + " SELECT * FROM (" + query + ") AS "
                + DuckDbSql.quoteIdentifier("__gretl_pg_export_query"));
    }

    private void exportWithJdbc(Connection duckdb, Connection postgres, PostgresExportSpec export) throws SQLException {
        DuckDbSql.QualifiedTable table = DuckDbSql.parseQualifiedTable(export.table());
        List<QueryColumn> queryColumns = queryColumns(duckdb, export.query());
        Map<String, GeometryOverrideSpec> overrides = geometryOverrides(export.geometries());
        boolean exists = tableExists(postgres, table.schema(), table.table());

        if (!exists && !export.create()) {
            throw new IllegalArgumentException("PostgreSQL export target table does not exist: " + export.table());
        }
        if (export.mode() == PostgresWriteMode.REPLACE) {
            execute(postgres, "DROP TABLE IF EXISTS " + pgTable(postgres, table));
            exists = false;
        }

        PostgresTableMetadata metadata = exists
                ? PostgresTableMetadata.discover(postgres, table.schema(), table.table(), true)
                : null;
        Map<String, GeometryMapping> geometryMappings = geometryMappings(queryColumns, overrides, metadata, export);

        if (!exists) {
            createTable(postgres, table, queryColumns, geometryMappings);
            metadata = PostgresTableMetadata.discover(postgres, table.schema(), table.table(), true);
        }

        validateTargetColumns(metadata, queryColumns, geometryMappings);
        if (export.mode() == PostgresWriteMode.TRUNCATE) {
            execute(postgres, "TRUNCATE TABLE " + pgTable(postgres, table));
        }

        int rows = insertRows(duckdb, postgres, export, table, queryColumns, geometryMappings);
        log.lifecycle("PostgreSQL export " + export.name() + ": " + rows + " rows written to " + export.table());
    }

    private List<QueryColumn> queryColumns(Connection duckdb, String query) throws SQLException {
        String sql = "SELECT * FROM (" + DuckDbSql.stripTrailingSemicolon(query) + ") AS "
                + DuckDbSql.quoteIdentifier("__gretl_pg_export_query") + " WHERE 1=0";
        try (Statement statement = duckdb.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            List<QueryColumn> columns = new ArrayList<>();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                String label = metadata.getColumnLabel(i);
                if (label == null || label.isBlank()) {
                    label = metadata.getColumnName(i);
                }
                if (label == null || label.isBlank()) {
                    throw new IllegalArgumentException("PostgreSQL export query has a column without a name");
                }
                String normalized = PostgresTableMetadata.normalize(label);
                if (!seen.add(normalized)) {
                    throw new IllegalArgumentException("PostgreSQL export query has duplicate column label: " + label);
                }
                columns.add(new QueryColumn(label, metadata.getColumnType(i), metadata.getColumnTypeName(i)));
            }
            if (columns.isEmpty()) {
                throw new IllegalArgumentException("PostgreSQL export query must return at least one column");
            }
            return columns;
        }
    }

    private Map<String, GeometryOverrideSpec> geometryOverrides(List<GeometryOverrideSpec> geometries) {
        Map<String, GeometryOverrideSpec> result = new LinkedHashMap<>();
        for (GeometryOverrideSpec geometry : geometries) {
            if (!geometry.include()) {
                throw new IllegalArgumentException("PostgreSQL export geometry mappings do not support include = false: "
                        + geometry.column());
            }
            result.put(PostgresTableMetadata.normalize(geometry.column()), geometry);
        }
        return result;
    }

    private Map<String, GeometryMapping> geometryMappings(List<QueryColumn> queryColumns,
            Map<String, GeometryOverrideSpec> overrides, PostgresTableMetadata metadata, PostgresExportSpec export) {
        Map<String, GeometryMapping> result = new LinkedHashMap<>();
        for (QueryColumn column : queryColumns) {
            GeometryOverrideSpec override = overrides.get(PostgresTableMetadata.normalize(column.name()));
            String targetColumn = override != null && override.alias() != null && !override.alias().isBlank()
                    ? override.alias()
                    : column.name();
            PostgresTableMetadata.GeometryColumn discovered = metadata == null ? null : metadata.geometry(targetColumn);
            boolean geometry = override != null || discovered != null;

            if (!geometry && export.create() && column.duckDbGeometry()) {
                throw new IllegalArgumentException("PostgreSQL export column " + column.name()
                        + " is a DuckDB GEOMETRY. Configure geometry('" + column.name()
                        + "') with srid and type for create = true.");
            }
            if (!geometry) {
                continue;
            }
            if (discovered != null && discovered.geography()) {
                throw new IllegalArgumentException("PostgreSQL export target geography columns are not supported yet: "
                        + targetColumn);
            }

            int srid = override != null && override.srid() != null
                    ? override.srid()
                    : discovered == null ? 0 : discovered.srid();
            String type = override != null && override.type() != null && !override.type().isBlank()
                    ? override.type()
                    : discovered == null ? null : discovered.type();
            if (srid <= 0 || type == null || type.isBlank()) {
                throw new IllegalArgumentException("PostgreSQL export geometry column " + column.name()
                        + " requires srid and type. Configure geometry('" + column.name() + "').");
            }
            result.put(PostgresTableMetadata.normalize(column.name()),
                    new GeometryMapping(targetColumn, srid, sanitizeGeometryType(type)));
        }
        return result;
    }

    private void validateTargetColumns(PostgresTableMetadata metadata, List<QueryColumn> queryColumns,
            Map<String, GeometryMapping> geometryMappings) {
        for (QueryColumn column : queryColumns) {
            GeometryMapping geometry = geometryMappings.get(PostgresTableMetadata.normalize(column.name()));
            String targetColumn = geometry == null ? column.name() : geometry.targetColumn();
            metadata.requireColumn(targetColumn);
        }
    }

    private void createTable(Connection postgres, DuckDbSql.QualifiedTable table, List<QueryColumn> queryColumns,
            Map<String, GeometryMapping> geometryMappings) throws SQLException {
        List<String> columns = new ArrayList<>();
        for (QueryColumn column : queryColumns) {
            GeometryMapping geometry = geometryMappings.get(PostgresTableMetadata.normalize(column.name()));
            String targetColumn = geometry == null ? column.name() : geometry.targetColumn();
            String type = geometry == null
                    ? postgresType(column)
                    : "geometry(" + geometry.type() + "," + geometry.srid() + ")";
            columns.add(pgIdentifier(postgres, targetColumn) + " " + type);
        }
        execute(postgres, "CREATE TABLE " + pgTable(postgres, table) + " (" + String.join(", ", columns) + ")");
    }

    private int insertRows(Connection duckdb, Connection postgres, PostgresExportSpec export,
            DuckDbSql.QualifiedTable table, List<QueryColumn> queryColumns,
            Map<String, GeometryMapping> geometryMappings) throws SQLException {
        String selectSql = selectSql(export.query(), queryColumns, geometryMappings);
        try (Statement select = duckdb.createStatement();
             ResultSet rows = select.executeQuery(selectSql);
             PreparedStatement insert = postgres.prepareStatement(insertSql(postgres, table, queryColumns, geometryMappings))) {
            int count = 0;
            int pending = 0;
            while (rows.next()) {
                for (int i = 0; i < queryColumns.size(); i++) {
                    QueryColumn column = queryColumns.get(i);
                    if (geometryMappings.containsKey(PostgresTableMetadata.normalize(column.name()))) {
                        insert.setString(i + 1, rows.getString(i + 1));
                    } else {
                        insert.setObject(i + 1, rows.getObject(i + 1));
                    }
                }
                insert.addBatch();
                count++;
                pending++;
                if (pending >= BATCH_SIZE) {
                    insert.executeBatch();
                    insert.clearBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                insert.executeBatch();
            }
            return count;
        }
    }

    private String selectSql(String query, List<QueryColumn> queryColumns,
            Map<String, GeometryMapping> geometryMappings) {
        String tableAlias = DuckDbSql.quoteIdentifier("__gretl_pg_export_query");
        List<String> expressions = new ArrayList<>();
        for (QueryColumn column : queryColumns) {
            String source = tableAlias + "." + DuckDbSql.quotePhysicalIdentifier(column.name());
            if (geometryMappings.containsKey(PostgresTableMetadata.normalize(column.name()))) {
                expressions.add("ST_AsHEXWKB(" + source + ") AS " + DuckDbSql.quotePhysicalIdentifier(column.name()));
            } else {
                expressions.add(source + " AS " + DuckDbSql.quotePhysicalIdentifier(column.name()));
            }
        }
        return "SELECT " + String.join(", ", expressions)
                + " FROM (" + DuckDbSql.stripTrailingSemicolon(query) + ") AS " + tableAlias;
    }

    private String insertSql(Connection postgres, DuckDbSql.QualifiedTable table, List<QueryColumn> queryColumns,
            Map<String, GeometryMapping> geometryMappings) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (QueryColumn column : queryColumns) {
            GeometryMapping geometry = geometryMappings.get(PostgresTableMetadata.normalize(column.name()));
            String targetColumn = geometry == null ? column.name() : geometry.targetColumn();
            columnNames.add(pgIdentifier(postgres, targetColumn));
            values.add(geometry == null ? "?" : geometryValueExpression(geometry));
        }
        return "INSERT INTO " + pgTable(postgres, table)
                + " (" + String.join(", ", columnNames) + ") VALUES ("
                + String.join(", ", values) + ")";
    }

    private String geometryValueExpression(GeometryMapping geometry) {
        return "ST_SetSRID(ST_GeomFromWKB(decode(?, 'hex')), " + geometry.srid() + ")::geometry("
                + geometry.type() + "," + geometry.srid() + ")";
    }

    private String postgresType(QueryColumn column) {
        return switch (column.jdbcType()) {
            case Types.SMALLINT -> "smallint";
            case Types.INTEGER, Types.TINYINT -> "integer";
            case Types.BIGINT -> "bigint";
            case Types.FLOAT, Types.REAL -> "real";
            case Types.DOUBLE -> "double precision";
            case Types.NUMERIC, Types.DECIMAL -> "numeric";
            case Types.BOOLEAN, Types.BIT -> "boolean";
            case Types.DATE -> "date";
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> "time";
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "timestamp";
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "bytea";
            default -> "text";
        };
    }

    private boolean tableExists(Connection postgres, String schema, String table) throws SQLException {
        try (PreparedStatement statement = postgres.prepareStatement("""
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private String pgTable(Connection postgres, DuckDbSql.QualifiedTable table) throws SQLException {
        return pgIdentifier(postgres, table.schema()) + "." + pgIdentifier(postgres, table.table());
    }

    private String pgIdentifier(Connection postgres, String identifier) throws SQLException {
        DatabaseMetaData metadata = postgres.getMetaData();
        String quote = metadata.getIdentifierQuoteString();
        if (quote == null || quote.isBlank()) {
            return identifier;
        }
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    private String sanitizeGeometryType(String type) {
        String normalized = type.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("Unsupported PostGIS geometry type: " + type);
        }
        return normalized;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        log.debug(sql);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    record OpenTarget(String alias, Connection connection) {
    }

    private record QueryColumn(String name, int jdbcType, String typeName) {
        boolean duckDbGeometry() {
            return typeName != null && typeName.toUpperCase(Locale.ROOT).contains("GEOMETRY");
        }
    }

    private record GeometryMapping(String targetColumn, int srid, String type) {
    }
}
