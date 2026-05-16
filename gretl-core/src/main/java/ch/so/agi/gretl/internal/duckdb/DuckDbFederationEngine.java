package ch.so.agi.gretl.internal.duckdb;

import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.EmptyFileException;
import ch.so.agi.gretl.util.FileStylingDefinition;
import ch.so.agi.gretl.util.GretlException;
import ch.so.agi.gretl.util.SqlReader;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DuckDbFederationEngine {
    private final GretlLogger log;

    public DuckDbFederationEngine() {
        this(LogEnvironment.getLogger(DuckDbFederationEngine.class));
    }

    DuckDbFederationEngine(GretlLogger log) {
        this.log = log;
    }

    public void execute(DuckDbExecutionRequest request) throws Exception {
        validateSqlFiles(request.sqlFiles());
        validateExportTargets(request.exports());
        if (request.databaseFile() != null && request.databaseFile().getParent() != null) {
            Files.createDirectories(request.databaseFile().getParent());
        }

        log.lifecycle(request.taskName() + ": Start DuckDbSqlExecutor");
        log.lifecycle(request.taskName() + ": DuckDB database: "
                + (request.inMemory() ? ":memory:" : request.databaseFile().toAbsolutePath())
                + ", Sources: " + request.sources().size()
                + ", Exports: " + request.exports().size());

        List<PendingExport> pendingExports = new ArrayList<>();
        try (Connection connection = openDuckDb(request.jdbcUrl())) {
            connection.setAutoCommit(false);
            try {
                ensureSupportedVersion(connection);
                loadExtensions(connection, request);
                boolean supportsGeometryCrs = supportsGeometryCrs(connection);
                DuckDbSessionBuilder builder = new DuckDbSessionBuilder(log);
                DuckDbSessionArtifacts artifacts = builder.bootstrap(connection, request, supportsGeometryCrs);

                for (Map<String, String> parameterSet : request.parameterSets()) {
                    executeSqlFiles(request.taskName(), connection, request.sqlFiles(), parameterSet);
                }

                pendingExports = exportResults(connection, request.exports());
                builder.cleanup(connection, artifacts);
                connection.commit();
            } catch (Exception e) {
                rollback(connection);
                cleanupTempExports(pendingExports);
                throw e;
            }
        }
        moveExports(pendingExports);
        log.lifecycle(request.taskName() + ": End DuckDbSqlExecutor (successful)");
    }

    private Connection openDuckDb(String jdbcUrl) throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        Connection connection = DriverManager.getConnection(jdbcUrl);
        connection.setAutoCommit(false);
        return connection;
    }

    private void validateSqlFiles(List<Path> sqlFiles) throws Exception {
        if (sqlFiles == null || sqlFiles.isEmpty()) {
            throw new GretlException(GretlException.TYPE_NO_FILE, "Inputfile list is null or empty");
        }
        for (Path sqlFile : sqlFiles) {
            if (sqlFile == null || !Files.isReadable(sqlFile)) {
                throw new GretlException(GretlException.TYPE_FILE_NOT_READABLE,
                        "Can not read sql file at path: " + sqlFile);
            }
            if (!"sql".equalsIgnoreCase(FilenameUtils.getExtension(sqlFile.toString()))) {
                throw new GretlException(GretlException.TYPE_WRONG_EXTENSION,
                        "File extension must be .sql. Error at File: " + sqlFile);
            }
            if (Files.size(sqlFile) == 0) {
                throw new EmptyFileException("File must not be empty: " + sqlFile.toAbsolutePath());
            }
            FileStylingDefinition.checkForUtf8(sqlFile.toFile());
            FileStylingDefinition.checkForBOMInFile(sqlFile.toFile());
            log.info(sqlFile.toAbsolutePath().toString());
        }
    }

    private void validateExportTargets(List<DuckDbExportSpec> exports) throws IOException {
        for (DuckDbExportSpec export : exports) {
            if (Files.exists(export.file()) && !export.overwrite()) {
                throw new IllegalArgumentException("Export target already exists and overwrite is false: " + export.file());
            }
            if (export.file().getParent() != null) {
                Files.createDirectories(export.file().getParent());
            }
        }
    }

    private void ensureSupportedVersion(Connection connection) throws SQLException {
        String version;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version()")) {
            resultSet.next();
            version = resultSet.getString(1);
        }
        if (version == null || !version.startsWith("v1.5.2")) {
            throw new IllegalStateException("DuckDbSqlExecutor requires DuckDB 1.5.2.x, but got: " + version);
        }
    }

    private void loadExtensions(Connection connection, DuckDbExecutionRequest request) throws SQLException {
        LinkedHashSet<String> extensions = new LinkedHashSet<>();
        for (DuckDbSourceSpec source : request.sources()) {
            extensions.addAll(source.requiredExtensions());
        }
        for (DuckDbExportSpec export : request.exports()) {
            extensions.addAll(export.requiredExtensions());
        }
        for (String extension : extensions) {
            if (request.installExtensions()) {
                execute(connection, "INSTALL " + extension);
            }
            try {
                execute(connection, "LOAD " + extension);
            } catch (SQLException e) {
                throw new SQLException("DuckDB extension '" + extension + "' could not be loaded. "
                        + "Install it in the GRETL Docker image or set installExtensions = true for development.",
                        e);
            }
        }
    }

    private boolean supportsGeometryCrs(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT ST_GeomFromText('POINT (0 0)')::GEOMETRY('EPSG:4326')");
            return true;
        } catch (SQLException e) {
            log.info("DuckDB GEOMETRY CRS typing is not available; geometries will be untyped.");
            return false;
        }
    }

    private void executeSqlFiles(String taskName, Connection connection, List<Path> sqlFiles,
            Map<String, String> params) throws Exception {
        for (Path sqlFile : sqlFiles) {
            executeSqlFile(taskName, connection, sqlFile, params);
        }
    }

    private void executeSqlFile(String taskName, Connection connection, Path sqlFile,
            Map<String, String> params) throws Exception {
        SqlReader reader = new SqlReader();
        try {
            String statement = reader.readSqlStmt(sqlFile.toFile(), params);
            if (statement == null) {
                throw new GretlException(GretlException.TYPE_NO_STATEMENT,
                        "At least one statement must be in the sql-File");
            }
            while (statement != null) {
                executeSqlStatement(taskName, connection, statement.trim());
                statement = reader.nextSqlStmt();
            }
        } finally {
            reader.close();
        }
    }

    private void executeSqlStatement(String taskName, Connection connection, String statement) throws SQLException {
        if (statement.isEmpty()) {
            return;
        }
        log.debug(statement);
        try (Statement dbStatement = connection.createStatement()) {
            boolean hasResultSet = dbStatement.execute(statement);
            if (hasResultSet) {
                logResultSet(taskName, dbStatement);
            } else {
                logUpdateCount(taskName, dbStatement.getUpdateCount());
            }
        } catch (SQLException ex) {
            throw new SQLException("Error while executing the sqlstatement. " + ex.getMessage(), ex);
        }
    }

    private void logResultSet(String taskName, Statement dbStatement) throws SQLException {
        try (ResultSet resultSet = dbStatement.getResultSet()) {
            StringBuilder result = new StringBuilder();
            int columnCount = resultSet.getMetaData().getColumnCount();
            String separator = "";
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = resultSet.getString(i);
                    if (value != null) {
                        result.append(separator).append(value);
                        separator = " ";
                    }
                }
                if (result.length() > 0) {
                    log.lifecycle(taskName + ": " + result);
                }
                result.setLength(0);
                separator = "";
            }
        }
    }

    private void logUpdateCount(String taskName, int modifiedLines) {
        if (modifiedLines == 1) {
            log.lifecycle(taskName + ": " + modifiedLines + " Line has been modified.");
        } else if (modifiedLines > 1) {
            log.lifecycle(taskName + ": " + modifiedLines + " Lines have been modified.");
        } else {
            log.lifecycle(taskName + ": No Line has been modified.");
        }
    }

    private List<PendingExport> exportResults(Connection connection, List<DuckDbExportSpec> exports) throws SQLException, IOException {
        List<PendingExport> pending = new ArrayList<>();
        execute(connection, "CREATE SCHEMA IF NOT EXISTS \"__gretl_export\"");
        for (DuckDbExportSpec export : exports) {
            Path temp = tempExportPath(export.file());
            pending.add(new PendingExport(temp, export.file(), export.overwrite()));
            if (export instanceof GpkgExportSpec gpkg) {
                exportGpkg(connection, gpkg, temp);
            } else if (export instanceof ParquetExportSpec parquet) {
                exportParquet(connection, parquet, temp);
            } else {
                throw new IllegalArgumentException("Unsupported DuckDB export: " + export);
            }
        }
        execute(connection, "DROP SCHEMA IF EXISTS \"__gretl_export\" CASCADE");
        return pending;
    }

    private void exportGpkg(Connection connection, GpkgExportSpec export, Path tempFile) throws SQLException {
        String layerTable = DuckDbSql.quoteIdentifier("__gretl_export") + "." + DuckDbSql.quoteIdentifier(export.layer());
        execute(connection, "DROP TABLE IF EXISTS " + layerTable);
        execute(connection, "CREATE TABLE " + layerTable + " AS "
                + DuckDbSql.stripTrailingSemicolon(export.query()));

        List<String> options = new ArrayList<>();
        options.add("FORMAT GDAL");
        options.add("DRIVER " + DuckDbSql.quoteLiteral("GPKG"));
        if (export.srs() != null && !export.srs().isBlank()) {
            options.add("SRS " + DuckDbSql.quoteLiteral(export.srs()));
        }
        execute(connection, "COPY " + layerTable + " TO "
                + DuckDbSql.quoteLiteral(tempFile.toAbsolutePath().toString())
                + " WITH (" + String.join(", ", options) + ")");
        execute(connection, "DROP TABLE IF EXISTS " + layerTable);
        normalizeGpkgLayerName(tempFile, export.layer());
    }

    private void exportParquet(Connection connection, ParquetExportSpec export, Path tempFile) throws SQLException {
        execute(connection, "COPY (" + DuckDbSql.stripTrailingSemicolon(export.query()) + ") TO "
                + DuckDbSql.quoteLiteral(tempFile.toAbsolutePath().toString())
                + " WITH (FORMAT PARQUET)");
    }

    private Path tempExportPath(Path finalFile) throws IOException {
        Path parent = finalFile.toAbsolutePath().getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath();
        }
        Files.createDirectories(parent);
        String fileName = finalFile.getFileName().toString();
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            extension = fileName.substring(dot);
        }
        Path temp = parent.resolve("." + fileName + "." + UUID.randomUUID() + ".tmp" + extension);
        Files.deleteIfExists(temp);
        return temp;
    }

    private void moveExports(List<PendingExport> pendingExports) throws IOException {
        for (PendingExport export : pendingExports) {
            if (export.overwrite()) {
                Files.move(export.tempFile(), export.finalFile(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } else {
                Files.move(export.tempFile(), export.finalFile(), StandardCopyOption.ATOMIC_MOVE);
            }
        }
    }

    private void cleanupTempExports(List<PendingExport> pendingExports) {
        for (PendingExport export : pendingExports) {
            try {
                Files.deleteIfExists(export.tempFile());
            } catch (IOException e) {
                log.error("failed to delete temp export " + export.tempFile(), e);
            }
        }
    }

    private void normalizeGpkgLayerName(Path gpkgFile, String requestedLayer) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver is required to normalize GeoPackage layer names", e);
        }
        try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + gpkgFile.toAbsolutePath())) {
            String currentLayer = currentGpkgLayer(sqlite);
            if (currentLayer == null || requestedLayer.equals(currentLayer)) {
                return;
            }
            try (Statement statement = sqlite.createStatement()) {
                statement.execute("ALTER TABLE " + quoteSqliteIdentifier(currentLayer)
                        + " RENAME TO " + quoteSqliteIdentifier(requestedLayer));
            }
            updateGpkgMetadata(sqlite, "gpkg_contents", currentLayer, requestedLayer);
            updateGpkgMetadata(sqlite, "gpkg_geometry_columns", currentLayer, requestedLayer);
            updateGpkgMetadata(sqlite, "gpkg_extensions", currentLayer, requestedLayer);
        }
    }

    private String currentGpkgLayer(Connection sqlite) throws SQLException {
        try (Statement statement = sqlite.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT table_name FROM gpkg_contents LIMIT 1")) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return null;
        }
    }

    private void updateGpkgMetadata(Connection sqlite, String table, String currentLayer, String requestedLayer)
            throws SQLException {
        if (!sqliteTableExists(sqlite, table)) {
            return;
        }
        try (PreparedStatement statement = sqlite.prepareStatement(
                "UPDATE " + quoteSqliteIdentifier(table) + " SET table_name = ? WHERE table_name = ?")) {
            statement.setString(1, requestedLayer);
            statement.setString(2, currentLayer);
            statement.executeUpdate();
        }
        if ("gpkg_contents".equals(table)) {
            try (PreparedStatement statement = sqlite.prepareStatement(
                    "UPDATE gpkg_contents SET identifier = ? WHERE table_name = ?")) {
                statement.setString(1, requestedLayer);
                statement.setString(2, requestedLayer);
                statement.executeUpdate();
            }
        }
    }

    private boolean sqliteTableExists(Connection sqlite, String table) throws SQLException {
        try (PreparedStatement statement = sqlite.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String quoteSqliteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void execute(Connection connection, String sql) throws SQLException {
        log.debug(sql);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.error("failed to rollback", e);
        }
    }

    private record PendingExport(Path tempFile, Path finalFile, boolean overwrite) {
    }
}
