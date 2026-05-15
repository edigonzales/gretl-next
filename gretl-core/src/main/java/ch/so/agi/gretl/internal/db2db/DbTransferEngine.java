package ch.so.agi.gretl.internal.db2db;

import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.DbConnector;
import ch.so.agi.gretl.util.EmptyFileException;
import ch.so.agi.gretl.util.FileStylingDefinition;
import ch.so.agi.gretl.util.GretlException;
import ch.so.agi.gretl.util.SqlReader;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DbTransferEngine {

    private final GretlLogger log;

    public DbTransferEngine() {
        this(LogEnvironment.getLogger(DbTransferEngine.class));
    }

    DbTransferEngine(GretlLogger log) {
        this.log = log;
    }

    public void execute(DbTransferRequest request) throws Exception {
        validateTransfers(request.transfers());

        log.lifecycle(request.taskName() + ": Start Db2Db");
        log.lifecycle(request.taskName() + ": Source DB-URL: " + request.sourceDatabase().jdbcUrl()
                + ", Target DB-URL: " + request.targetDatabase().jdbcUrl()
                + ", Transfers: " + request.transfers().size());

        try (Connection source = openConnection(request.sourceDatabase().jdbcUrl(),
                request.sourceDatabase().username(), request.sourceDatabase().password());
             Connection target = openConnection(request.targetDatabase().jdbcUrl(),
                     request.targetDatabase().username(), request.targetDatabase().password())) {
            List<Integer> rowCounts = new ArrayList<>();
            try {
                for (Map<String, String> parameterSet : request.parameterSets()) {
                    for (DbTransferSpec transfer : request.transfers()) {
                        rowCounts.add(processTransfer(source, target, transfer, parameterSet, request));
                    }
                }
                target.commit();
                String counts = rowCounts.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
                log.lifecycle(String.format(
                        "%s: Finished Db2Db. Number of transfer executions: %s, transferred rows: [%s]",
                        request.taskName(), rowCounts.size(), counts));
            } catch (Exception e) {
                rollback(target);
                throw e;
            } finally {
                rollback(source);
            }
        }
    }

    private Connection openConnection(String jdbcUrl, String username, String password) throws SQLException {
        Connection connection = DbConnector.connect(jdbcUrl, username, password);
        connection.setAutoCommit(false);
        return connection;
    }

    private void validateTransfers(List<DbTransferSpec> transfers) throws Exception {
        for (DbTransferSpec transfer : transfers) {
            validateSqlFile(transfer.sqlFile());
        }
    }

    private void validateSqlFile(Path sqlFile) throws Exception {
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

    private int processTransfer(Connection source, Connection target, DbTransferSpec transfer,
            Map<String, String> parameters, DbTransferRequest request) throws Exception {
        if (transfer.deleteAllRows()) {
            deleteTargetRows(target, transfer);
        }

        List<String> statements = extractStatements(transfer.sqlFile(), parameters);
        if (statements.size() == 2) {
            try (Statement statement = source.createStatement()) {
                statement.execute(statements.get(0));
            }
        }

        String selectStatement = statements.get(statements.size() - 1);
        log.debug("SQL statement: " + selectStatement);

        try (Statement select = source.createStatement()) {
            select.setFetchSize(request.fetchSize());
            try (ResultSet rows = select.executeQuery(selectStatement);
                 PreparedStatement insert = createInsertStatement(target, rows.getMetaData(), transfer)) {
                return copyRows(rows, insert, rows.getMetaData().getColumnCount(), request.batchSize(), transfer);
            }
        }
    }

    private void deleteTargetRows(Connection target, DbTransferSpec transfer) throws SQLException {
        String sql = "DELETE FROM " + transfer.targetTable().toSql(target);
        try (Statement statement = target.createStatement()) {
            statement.executeUpdate(sql);
        }
        log.info("DELETE executed: " + transfer.targetTable().displayName());
    }

    private PreparedStatement createInsertStatement(Connection target, ResultSetMetaData sourceMeta,
            DbTransferSpec transfer) throws SQLException {
        ColumnNameMap targetColumns = ColumnNameMap.from(target, transfer.targetTable());
        DatabaseMetaData targetMeta = target.getMetaData();
        String quote = targetMeta.getIdentifierQuoteString();

        List<String> columnNames = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 1; i <= sourceMeta.getColumnCount(); i++) {
            String sourceName = sourceColumnName(sourceMeta, i);
            String targetColumn = targetColumns.get(sourceName);
            columnNames.add(quoteIdentifier(targetColumn, quote));
            values.add(transfer.valueExpression(sourceName));
        }

        String sql = "INSERT INTO " + transfer.targetTable().toSql(target)
                + " (" + String.join(", ", columnNames) + ") VALUES ("
                + String.join(", ", values) + ")";
        log.info("Sql insert statement: [" + sql + "]");
        return target.prepareStatement(sql);
    }

    private int copyRows(ResultSet rows, PreparedStatement insert, int columnCount, int batchSize,
            DbTransferSpec transfer) throws SQLException {
        int rowCount = 0;
        int pendingBatchRows = 0;
        while (rows.next()) {
            for (int column = 1; column <= columnCount; column++) {
                insert.setObject(column, rows.getObject(column));
            }
            insert.addBatch();
            rowCount++;
            pendingBatchRows++;

            if (pendingBatchRows >= batchSize) {
                log.debug("Executing batch of " + pendingBatchRows + " records. (Total: " + rowCount + ")");
                insert.executeBatch();
                insert.clearBatch();
                pendingBatchRows = 0;
            }
        }

        if (pendingBatchRows > 0) {
            insert.executeBatch();
        }

        log.debug("Transferred " + rowCount + " rows and " + columnCount + " columns to table "
                + transfer.targetTable().displayName());
        return rowCount;
    }

    private List<String> extractStatements(Path sqlFile, Map<String, String> params) throws Exception {
        SqlReader reader = new SqlReader();
        try {
            String first = reader.readSqlStmt(sqlFile.toFile(), params);
            if (first == null) {
                throw new EmptyFileException("Empty file: " + sqlFile.getFileName());
            }

            String second = reader.nextSqlStmt();
            if (second != null && !first.toLowerCase(Locale.ROOT).trim().startsWith("set search_path to")) {
                throw new IllegalArgumentException("First statement must be a set search_path statement.");
            }

            String third = reader.nextSqlStmt();
            if (third != null) {
                throw new IllegalArgumentException("There are more than 2 statements in the file");
            }

            if (second == null) {
                return List.of(first);
            }
            return List.of(first, second);
        } finally {
            reader.close();
        }
    }

    private static String sourceColumnName(ResultSetMetaData sourceMeta, int column) throws SQLException {
        String label = sourceMeta.getColumnLabel(column);
        if (label != null && !label.isBlank()) {
            return label;
        }
        return sourceMeta.getColumnName(column);
    }

    private static String quoteIdentifier(String identifier, String quote) {
        if (quote == null || quote.isBlank()) {
            return identifier;
        }
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.error("failed to rollback", e);
        }
    }

    private static final class ColumnNameMap {
        private final Map<String, String> columnNames;

        private ColumnNameMap(Map<String, String> columnNames) {
            this.columnNames = columnNames;
        }

        static ColumnNameMap from(Connection target, TargetIdentifier targetTable) throws SQLException {
            Map<String, String> columnNames = new LinkedHashMap<>();
            String sql = "SELECT * FROM " + targetTable.toSql(target) + " WHERE 0=1";
            try (Statement statement = target.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                ResultSetMetaData metadata = resultSet.getMetaData();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    String columnName = metadata.getColumnName(i);
                    columnNames.put(columnName.toLowerCase(Locale.ROOT), columnName);
                }
            }
            return new ColumnNameMap(columnNames);
        }

        String get(String sourceColumnName) {
            String targetColumn = columnNames.get(sourceColumnName.toLowerCase(Locale.ROOT));
            if (targetColumn == null) {
                throw new GretlException(GretlException.TYPE_COLUMN_MISMATCH,
                        String.format("Requested attribute [%s] is not contained in target table.", sourceColumnName));
            }
            return targetColumn;
        }
    }
}
