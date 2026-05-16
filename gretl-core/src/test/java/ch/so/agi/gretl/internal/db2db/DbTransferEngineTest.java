package ch.so.agi.gretl.internal.db2db;

import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import ch.so.agi.gretl.util.EmptyFileException;
import ch.so.agi.gretl.util.GretlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbTransferEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void requestRejectsEmptyTransferList() {
        DatabaseSpec database = new DatabaseSpec("jdbc:sqlite:" + tempDir.resolve("test.db"), null, null);

        assertThrows(IllegalArgumentException.class,
                () -> new DbTransferRequest("copy", database, database, List.of(), List.of(Map.of()), 100, 0));
    }

    @Test
    void rejectsMultipleStatementsWithoutSearchPathPrefix() throws Exception {
        Path sqlFile = tempDir.resolve("multi.sql");
        Files.writeString(sqlFile, "select * from colors; select * from colors;", StandardCharsets.UTF_8);
        DatabasePair databases = createDatabases();

        assertThrows(IllegalArgumentException.class,
                () -> new DbTransferEngine().execute(request(databases, sqlFile, "colors_copy")));
    }

    @Test
    void rejectsEmptySqlFile() throws Exception {
        Path sqlFile = tempDir.resolve("empty.sql");
        Files.writeString(sqlFile, "", StandardCharsets.UTF_8);
        DatabasePair databases = createDatabases();

        assertThrows(EmptyFileException.class,
                () -> new DbTransferEngine().execute(request(databases, sqlFile, "colors_copy")));
    }

    @Test
    void propagatesInvalidSql() throws Exception {
        Path sqlFile = tempDir.resolve("invalid.sql");
        Files.writeString(sqlFile, "select missing_column from colors;", StandardCharsets.UTF_8);
        DatabasePair databases = createDatabases();

        assertThrows(SQLException.class,
                () -> new DbTransferEngine().execute(request(databases, sqlFile, "colors_copy")));
    }

    @Test
    void rejectsColumnMismatch() throws Exception {
        Path sqlFile = tempDir.resolve("extra-column.sql");
        Files.writeString(sqlFile, "select id, name, 'extra' as extra from colors;", StandardCharsets.UTF_8);
        DatabasePair databases = createDatabases();

        GretlException exception = assertThrows(GretlException.class,
                () -> new DbTransferEngine().execute(request(databases, sqlFile, "colors_copy")));

        assertEquals(GretlException.TYPE_COLUMN_MISMATCH, exception.getType());
    }

    @Test
    void propagatesIncompatibleDataType() throws Exception {
        Path sqlFile = tempDir.resolve("bad-type.sql");
        Files.writeString(sqlFile, "select 'abc' as id, 'red' as name;", StandardCharsets.UTF_8);
        DatabasePair databases = createDatabases();

        assertThrows(SQLException.class,
                () -> new DbTransferEngine().execute(request(databases, sqlFile, "colors_copy")));
    }

    @Test
    void copiesEmptySourceTable() throws Exception {
        Path sqlFile = tempDir.resolve("empty-copy.sql");
        Files.writeString(sqlFile, "select id, name from colors where 1 = 0;", StandardCharsets.UTF_8);
        DatabasePair databases = createDatabases();

        new DbTransferEngine().execute(request(databases, sqlFile, "colors_copy"));

        assertEquals(0, count(databases.target(), "colors_copy"));
    }

    @Test
    void copiesRowsAndDeletesTargetRowsInSameTransfer() throws Exception {
        Path sqlFile = tempDir.resolve("copy.sql");
        Files.writeString(sqlFile, "select id, name from colors;", StandardCharsets.UTF_8);
        DatabasePair databases = createDatabases();
        insert(databases.target(), "colors_copy", 99, "old");

        DbTransferRequest request = request(databases, sqlFile, "colors_copy", true);
        new DbTransferEngine().execute(request);

        assertEquals(1, count(databases.target(), "colors_copy"));
        assertTrue(exists(databases.target(), "colors_copy", 1));
    }

    private DbTransferRequest request(DatabasePair databases, Path sqlFile, String targetTable) {
        return request(databases, sqlFile, targetTable, false);
    }

    private DbTransferRequest request(DatabasePair databases, Path sqlFile, String targetTable, boolean deleteAllRows) {
        return new DbTransferRequest(
                "copy",
                new DatabaseSpec("jdbc:sqlite:" + databases.source(), null, null),
                new DatabaseSpec("jdbc:sqlite:" + databases.target(), null, null),
                List.of(new DbTransferSpec(sqlFile, targetTable, deleteAllRows, List.of())),
                List.of(Map.of()),
                2,
                0
        );
    }

    private DatabasePair createDatabases() throws Exception {
        Path source = tempDir.resolve("source.db");
        Path target = tempDir.resolve("target.db");
        createTable(source, "colors");
        insert(source, "colors", 1, "red");
        createTable(target, "colors_copy");
        return new DatabasePair(source, target);
    }

    private void createTable(Path database, String tableName) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement()) {
            statement.execute("create table " + tableName + " (id integer primary key, name text)");
        }
    }

    private void insert(Path database, String tableName, int id, String name) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement()) {
            statement.execute("insert into " + tableName + " (id, name) values (" + id + ", '" + name + "')");
        }
    }

    private int count(Path database, String tableName) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from " + tableName)) {
            result.next();
            return result.getInt(1);
        }
    }

    private boolean exists(Path database, String tableName, int id) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement();
             var result = statement.executeQuery("select 1 from " + tableName + " where id = " + id)) {
            return result.next();
        }
    }

    private record DatabasePair(Path source, Path target) {
    }
}
