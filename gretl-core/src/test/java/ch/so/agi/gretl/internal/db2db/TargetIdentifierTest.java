package ch.so.agi.gretl.internal.db2db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TargetIdentifierTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesQualifiedAndQuotedIdentifiers() throws Exception {
        TargetIdentifier identifier = TargetIdentifier.parse("main.\"target.table\"");

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"))) {
            assertEquals("\"main\".\"target.table\"", identifier.toSql(connection));
        }
    }

    @Test
    void rejectsEmptyIdentifierPart() {
        assertThrows(IllegalArgumentException.class, () -> TargetIdentifier.parse("schema..table"));
    }

    @Test
    void rejectsUnterminatedQuotedIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> TargetIdentifier.parse("\"schema.table"));
    }
}
