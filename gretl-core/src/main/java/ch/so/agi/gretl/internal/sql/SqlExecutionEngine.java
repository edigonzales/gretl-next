package ch.so.agi.gretl.internal.sql;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class SqlExecutionEngine {

    private final GretlLogger log;

    public SqlExecutionEngine() {
        this(LogEnvironment.getLogger(SqlExecutionEngine.class));
    }

    SqlExecutionEngine(GretlLogger log) {
        this.log = log;
    }

    public void execute(SqlExecutionRequest request) throws Exception {
        List<Path> sqlFiles = validateSqlFiles(request.sqlFiles());

        log.lifecycle(request.taskName() + ": Start SqlExecutor");
        log.lifecycle(request.taskName() + ": Given parameters DB-URL: " + request.database().jdbcUrl()
                + ", DB-User: " + request.database().username() + ", Files: " + sqlFiles);

        try (Connection connection = openConnection(request.database())) {
            try {
                for (Map<String, String> parameterSet : request.parameterSets()) {
                    executeSqlFiles(request.taskName(), connection, sqlFiles, parameterSet);
                }
                connection.commit();
                log.lifecycle(request.taskName() + ": End SqlExecutor (successful)");
            } catch (Exception e) {
                rollback(connection);
                throw e;
            }
        }
    }

    private Connection openConnection(DatabaseSpec database) throws SQLException {
        Connection connection = DbConnector.connect(database.jdbcUrl(), database.username(), database.password());
        connection.setAutoCommit(false);
        return connection;
    }

    private List<Path> validateSqlFiles(List<Path> sqlFiles) throws Exception {
        if (sqlFiles == null || sqlFiles.isEmpty()) {
            throw new GretlException(GretlException.TYPE_NO_FILE, "Inputfile list is null or empty");
        }

        for (Path sqlFile : sqlFiles) {
            validateSqlFile(sqlFile);
        }

        return sqlFiles;
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

    private void executeSqlStatement(String taskName, Connection connection, String statement) throws Exception {
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
                        result.append(separator);
                        result.append(value);
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

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.error("failed to rollback", e);
        }
    }
}
