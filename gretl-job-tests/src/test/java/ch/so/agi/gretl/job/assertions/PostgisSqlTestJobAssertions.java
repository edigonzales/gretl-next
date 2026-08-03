package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.fixture.PostgisTestFixtureLease;
import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PostgisSqlTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "database-postgis-sql"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        PostgisTestFixtureLease lease = context.requireFixture("postgis", PostgisTestFixtureLease.class);
        try (var connection = lease.openHostConnection(); var statement = connection.createStatement()) {
            assertEquals(lease.schema(), connection.getSchema());
            try (ResultSet rows = statement.executeQuery("select id, name from " + quote(lease.schema()) + ".p2_colors order by id")) {
                assertTrue(rows.next()); assertEquals(1, rows.getInt("id")); assertEquals("red", rows.getString("name"));
                assertTrue(rows.next()); assertEquals(2, rows.getInt("id")); assertEquals("blue", rows.getString("name")); assertFalse(rows.next());
            }
            try (ResultSet rows = statement.executeQuery("select count(*) from information_schema.tables where table_schema='public' and table_name='p2_colors'")) {
                assertTrue(rows.next()); assertEquals(0, rows.getInt(1));
            }
        }
    }
    private static String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
