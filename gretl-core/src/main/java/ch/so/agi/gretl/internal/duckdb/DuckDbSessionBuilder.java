package ch.so.agi.gretl.internal.duckdb;

import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.util.DbConnector;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DuckDbSessionBuilder {
    private final GretlLogger log;

    public DuckDbSessionBuilder(GretlLogger log) {
        this.log = log;
    }

    public DuckDbSessionArtifacts bootstrap(Connection duckdb, DuckDbExecutionRequest request,
            boolean supportsGeometryCrs) throws Exception {
        List<String> logicalSchemas = new ArrayList<>();
        List<String> rawAttachments = new ArrayList<>();

        for (DuckDbSourceSpec source : request.sources()) {
            logicalSchemas.add(source.alias());
            if (source instanceof PostgresSourceSpec postgres) {
                rawAttachments.add(bootstrapPostgres(duckdb, postgres, supportsGeometryCrs));
            } else if (source instanceof GpkgSourceSpec gpkg) {
                bootstrapGpkg(duckdb, gpkg);
            } else {
                throw new IllegalArgumentException("Unsupported DuckDB source: " + source);
            }
        }
        return new DuckDbSessionArtifacts(logicalSchemas, rawAttachments);
    }

    public void cleanup(Connection duckdb, DuckDbSessionArtifacts artifacts) throws SQLException {
        for (String logicalSchema : artifacts.logicalSchemas()) {
            execute(duckdb, "DROP SCHEMA IF EXISTS " + DuckDbSql.quoteIdentifier(logicalSchema) + " CASCADE");
        }
        for (String rawAttachment : artifacts.rawAttachments()) {
            execute(duckdb, "DETACH " + DuckDbSql.quoteIdentifier(rawAttachment));
        }
    }

    private String bootstrapPostgres(Connection duckdb, PostgresSourceSpec source,
            boolean supportsGeometryCrs) throws Exception {
        PostgresJdbcUrl jdbcUrl = PostgresJdbcUrl.parse(source.database().jdbcUrl());
        String rawAlias = "__gretl_" + source.alias() + "_raw";
        String secretName = "__gretl_" + source.alias() + "_secret";

        log.info("Creating DuckDB postgres secret for source " + source.alias() + " (<redacted>)");
        execute(duckdb, createPostgresSecretSql(secretName, jdbcUrl,
                source.database().username(), source.database().password()));

        String attachOptions = jdbcUrl.libpqOptionsWithoutCredentials();
        String attach = "ATTACH " + DuckDbSql.quoteLiteral(attachOptions)
                + " AS " + DuckDbSql.quoteIdentifier(rawAlias)
                + " (TYPE postgres, SECRET " + DuckDbSql.quoteIdentifier(secretName) + ", READ_ONLY)";
        execute(duckdb, attach);
        execute(duckdb, "CREATE SCHEMA IF NOT EXISTS " + DuckDbSql.quoteIdentifier(source.alias()));

        try (Connection postgres = DbConnector.connect(source.database().jdbcUrl(),
                source.database().username(), source.database().password())) {
            postgres.setReadOnly(true);
            for (PostgresTableSpec table : source.tables()) {
                PostgresTableMetadata metadata = PostgresTableMetadata.discover(
                        postgres, table.physicalSchema(), table.physicalTable(), source.autoDetectGeometry());
                createPostgresLogicalObject(duckdb, source, table, rawAlias, metadata, supportsGeometryCrs);
            }
        }
        return rawAlias;
    }

    private void bootstrapGpkg(Connection duckdb, GpkgSourceSpec source) throws Exception {
        if (!Files.isReadable(source.file())) {
            throw new IllegalArgumentException("GeoPackage source file is not readable: " + source.file());
        }
        execute(duckdb, "CREATE SCHEMA IF NOT EXISTS " + DuckDbSql.quoteIdentifier(source.alias()));
        for (GpkgLayerSpec layer : source.layers()) {
            DuckDbMode mode = resolveMode(layer.mode(), source.mode());
            String objectName = DuckDbSql.quoteIdentifier(source.alias()) + "." + DuckDbSql.quoteIdentifier(layer.alias());
            dropLogicalObject(duckdb, objectName);

            String selectList = layer.columns().isEmpty()
                    ? "*"
                    : layer.columns().stream().map(DuckDbSql::quotePhysicalIdentifier).reduce((a, b) -> a + ", " + b).orElse("*");
            String sourceSql = "SELECT " + selectList
                    + " FROM ST_Read(" + DuckDbSql.quoteLiteral(source.file().toAbsolutePath().toString())
                    + ", layer = " + DuckDbSql.quoteLiteral(layer.layer()) + ")";
            createLogicalObject(duckdb, objectName, sourceSql, mode);
        }
    }

    private void createPostgresLogicalObject(Connection duckdb, PostgresSourceSpec source, PostgresTableSpec table,
            String rawAlias, PostgresTableMetadata metadata, boolean supportsGeometryCrs) throws SQLException {
        DuckDbMode mode = resolveMode(table.mode(), source.mode());
        String objectName = DuckDbSql.quoteIdentifier(source.alias()) + "." + DuckDbSql.quoteIdentifier(table.alias());
        dropLogicalObject(duckdb, objectName);

        List<ColumnProjection> projections = buildPostgresProjections(table, metadata, supportsGeometryCrs);
        boolean hasGeometry = projections.stream().anyMatch(ColumnProjection::geometry);
        String sourceSql;
        if (hasGeometry) {
            String innerSql = projections.stream()
                    .map(ColumnProjection::postgresExpression)
                    .reduce((a, b) -> a + ", " + b)
                    .orElseThrow();
            String outerSql = projections.stream()
                    .map(ColumnProjection::duckDbExpression)
                    .reduce((a, b) -> a + ", " + b)
                    .orElseThrow();
            String postgresQuery = "SELECT " + innerSql + " FROM "
                    + DuckDbSql.quotePhysicalIdentifier(table.physicalSchema())
                    + "." + DuckDbSql.quotePhysicalIdentifier(table.physicalTable());
            sourceSql = "SELECT " + outerSql + " FROM postgres_query("
                    + DuckDbSql.quoteLiteral(rawAlias) + ", " + DuckDbSql.quoteLiteral(postgresQuery) + ")";
        } else {
            String selectList = projections.stream()
                    .map(ColumnProjection::directExpression)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("*");
            sourceSql = "SELECT " + selectList + " FROM "
                    + DuckDbSql.quoteIdentifier(rawAlias)
                    + "." + DuckDbSql.quotePhysicalIdentifier(table.physicalSchema())
                    + "." + DuckDbSql.quotePhysicalIdentifier(table.physicalTable());
        }
        createLogicalObject(duckdb, objectName, sourceSql, mode);
    }

    private List<ColumnProjection> buildPostgresProjections(PostgresTableSpec table,
            PostgresTableMetadata metadata, boolean supportsGeometryCrs) {
        Map<String, GeometryOverrideSpec> overrides = new LinkedHashMap<>();
        for (GeometryOverrideSpec override : table.geometries()) {
            overrides.put(PostgresTableMetadata.normalize(override.column()), override);
            metadata.requireColumn(override.column());
        }

        List<PostgresTableMetadata.Column> sourceColumns = new ArrayList<>();
        if (table.columns().isEmpty()) {
            sourceColumns.addAll(metadata.columns().values());
        } else {
            for (String column : table.columns()) {
                sourceColumns.add(metadata.requireColumn(column));
            }
        }

        List<ColumnProjection> projections = new ArrayList<>();
        for (PostgresTableMetadata.Column column : sourceColumns) {
            String normalized = PostgresTableMetadata.normalize(column.name());
            GeometryOverrideSpec override = overrides.get(normalized);
            PostgresTableMetadata.GeometryColumn discovered = metadata.geometry(column.name());
            boolean isGeometry = discovered != null || override != null;
            if (isGeometry) {
                if (override != null && !override.include()) {
                    continue;
                }
                projections.add(ColumnProjection.geometry(column, discovered, override, supportsGeometryCrs));
            } else {
                projections.add(ColumnProjection.scalar(column));
            }
        }
        return projections;
    }

    private void createLogicalObject(Connection connection, String objectName, String sourceSql, DuckDbMode mode)
            throws SQLException {
        if (mode == DuckDbMode.MATERIALIZE) {
            execute(connection, "CREATE TABLE " + objectName + " AS " + sourceSql);
        } else {
            execute(connection, "CREATE OR REPLACE VIEW " + objectName + " AS " + sourceSql);
        }
    }

    private void dropLogicalObject(Connection connection, String objectName) throws SQLException {
        execute(connection, "DROP VIEW IF EXISTS " + objectName);
        execute(connection, "DROP TABLE IF EXISTS " + objectName);
    }

    private DuckDbMode resolveMode(String childMode, String parentMode) {
        if (childMode != null && !childMode.isBlank()) {
            return DuckDbMode.parse(childMode);
        }
        return DuckDbMode.parse(parentMode);
    }

    private String createPostgresSecretSql(String secretName, PostgresJdbcUrl url, String username, String password) {
        List<String> options = new ArrayList<>();
        options.add("TYPE postgres");
        options.add("HOST " + DuckDbSql.quoteLiteral(url.host()));
        options.add("PORT " + url.port());
        options.add("DATABASE " + DuckDbSql.quoteLiteral(url.database()));
        if (username != null && !username.isBlank()) {
            options.add("USER " + DuckDbSql.quoteLiteral(username));
        }
        if (password != null) {
            options.add("PASSWORD " + DuckDbSql.quoteLiteral(password));
        }
        return "CREATE OR REPLACE SECRET " + DuckDbSql.quoteIdentifier(secretName)
                + " (" + String.join(", ", options) + ")";
    }

    private void execute(Connection connection, String sql) throws SQLException {
        log.debug(maskSecrets(sql));
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String maskSecrets(String sql) {
        return sql.replaceAll("(?i)PASSWORD\\s+'[^']*'", "PASSWORD '<redacted>'");
    }

    private record ColumnProjection(
            String postgresExpression,
            String duckDbExpression,
            String directExpression,
            boolean geometry
    ) {
        static ColumnProjection scalar(PostgresTableMetadata.Column column) {
            String quoted = DuckDbSql.quotePhysicalIdentifier(column.name());
            return new ColumnProjection(
                    quoted,
                    quoted,
                    quoted,
                    false);
        }

        static ColumnProjection geometry(PostgresTableMetadata.Column column,
                PostgresTableMetadata.GeometryColumn discovered, GeometryOverrideSpec override,
                boolean supportsGeometryCrs) {
            String outputName = override != null && override.alias() != null && !override.alias().isBlank()
                    ? override.alias()
                    : column.name();
            int srid = override != null && override.srid() != null
                    ? override.srid()
                    : discovered == null ? 0 : discovered.srid();
            boolean force2d = override != null && override.force2d();
            boolean geography = discovered != null && discovered.geography();
            String sourceGeom = DuckDbSql.quotePhysicalIdentifier(column.name()) + (geography ? "::geometry" : "");
            if (force2d) {
                sourceGeom = "ST_Force2D(" + sourceGeom + ")";
            }
            String postgresExpression = "encode(ST_AsEWKB(" + sourceGeom + "), 'hex') AS "
                    + DuckDbSql.quotePhysicalIdentifier(outputName);
            String duckDbExpression = "ST_GeomFromHEXEWKB(" + DuckDbSql.quotePhysicalIdentifier(outputName) + ")";
            if (supportsGeometryCrs && srid > 0) {
                duckDbExpression = "(" + duckDbExpression + ")::GEOMETRY('EPSG:" + srid + "')";
            }
            duckDbExpression += " AS " + DuckDbSql.quotePhysicalIdentifier(outputName);
            return new ColumnProjection(
                    postgresExpression,
                    duckDbExpression,
                    DuckDbSql.quotePhysicalIdentifier(column.name()),
                    true);
        }
    }
}
