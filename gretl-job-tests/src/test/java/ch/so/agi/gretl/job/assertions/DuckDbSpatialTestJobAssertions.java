package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DuckDbSpatialTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "core-duckdb-spatial"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        var database = context.job().resolve("build/spatial/spatial.duckdb");
        assertTrue(Files.isRegularFile(database), "DuckDB output missing: " + database);
        try (var connection = DriverManager.getConnection("jdbc:duckdb:" + database)) {
            Set<String> tables = new java.util.HashSet<>();
            try (var rows = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rows.next()) tables.add(rows.getString("TABLE_NAME").toLowerCase());
            }
            assertTrue(tables.containsAll(Set.of("points", "point_geometries", "p2_colors")), tables.toString());
            try (var rows = connection.createStatement().executeQuery("select id, x, y from points order by id")) {
                assertTrue(rows.next()); assertEquals(1, rows.getInt("id")); assertEquals(2600000d, rows.getDouble("x")); assertEquals(1200000d, rows.getDouble("y"));
                assertTrue(rows.next()); assertEquals(2, rows.getInt("id")); assertEquals(2600001d, rows.getDouble("x")); assertEquals(1200001d, rows.getDouble("y")); assertFalse(rows.next());
            }
            try (var rows = connection.createStatement().executeQuery("select id, point_x, point_y, geom is not null as has_geom from point_geometries order by id")) {
                assertTrue(rows.next()); assertEquals(1, rows.getInt("id")); assertEquals(2600000d, rows.getDouble("point_x")); assertEquals(1200000d, rows.getDouble("point_y")); assertTrue(rows.getBoolean("has_geom"));
                assertTrue(rows.next()); assertEquals(2, rows.getInt("id")); assertEquals(2600001d, rows.getDouble("point_x")); assertEquals(1200001d, rows.getDouble("point_y")); assertTrue(rows.getBoolean("has_geom")); assertFalse(rows.next());
            }
            try (var rows = connection.createStatement().executeQuery("select id, name from p2_colors order by id")) {
                assertTrue(rows.next()); assertEquals(1, rows.getInt("id")); assertEquals("red", rows.getString("name"));
                assertTrue(rows.next()); assertEquals(2, rows.getInt("id")); assertEquals("blue", rows.getString("name")); assertFalse(rows.next());
            }
        }
        assertFalse(context.result().output().contains("INSTALL spatial"));
    }
}
