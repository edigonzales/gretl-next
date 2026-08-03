package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SqliteTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "core-sqlite"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        var database = context.job().resolve("build/db/test.db");
        assertTrue(Files.isRegularFile(database), "SQLite database missing: " + database);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var rows = connection.createStatement().executeQuery("select id, label from canonical_values order by id")) {
            assertTrue(rows.next()); assertEquals(1, rows.getInt("id")); assertEquals("alpha", rows.getString("label"));
            assertTrue(rows.next()); assertEquals(2, rows.getInt("id")); assertEquals("beta", rows.getString("label"));
            assertFalse(rows.next());
        }
    }
}
