package ch.so.agi.gretl.internal.duckdb;

import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DuckDbSpecValidationTest {
    @Test
    void requestRejectsMissingDatabase() {
        assertThrows(IllegalArgumentException.class, () -> new DuckDbExecutionRequest(
                "duck",
                null,
                false,
                false,
                List.of(),
                List.of(),
                List.of(Path.of("sql/analyse.sql")),
                List.of(Map.of()),
                List.of()));
    }

    @Test
    void requestRejectsDatabaseAndInMemoryTogether() {
        assertThrows(IllegalArgumentException.class, () -> new DuckDbExecutionRequest(
                "duck",
                Path.of("build/work.duckdb"),
                true,
                false,
                List.of(),
                List.of(),
                List.of(Path.of("sql/analyse.sql")),
                List.of(Map.of()),
                List.of()));
    }

    @Test
    void requestRejectsEmptySqlFiles() {
        assertThrows(IllegalArgumentException.class, () -> new DuckDbExecutionRequest(
                "duck",
                Path.of("build/work.duckdb"),
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(Map.of()),
                List.of()));
    }

    @Test
    void rejectsInvalidSourceAlias() {
        assertThrows(IllegalArgumentException.class, () -> new PostgresSourceSpec(
                "not-valid",
                new DatabaseSpec("jdbc:postgresql://localhost/db", "u", "p"),
                "view",
                true,
                List.of(PostgresTableSpec.fromQualifiedName("agi_pub.gemeinden", null, null, List.of(), List.of()))));
    }

    @Test
    void rejectsInvalidPostgresTableName() {
        assertThrows(IllegalArgumentException.class,
                () -> PostgresTableSpec.fromQualifiedName("gemeinden", null, null, List.of(), List.of()));
    }

    @Test
    void rejectsInvalidGeometryEncoding() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeometryOverrideSpec("geom", null, null, null, "shape", false, true));
    }

    @Test
    void rejectsInvalidTargetAlias() {
        assertThrows(IllegalArgumentException.class, () -> new PostgresTargetSpec(
                "not-valid",
                new DatabaseSpec("jdbc:postgresql://localhost/db", "u", "p")));
    }

    @Test
    void rejectsPostgresExportWithoutMode() {
        assertThrows(IllegalArgumentException.class, () -> new PostgresExportSpec(
                "out",
                "pg",
                "SELECT 1 AS id",
                "public.result",
                null,
                PostgresWritePath.JDBC,
                false,
                List.of()));
    }

    @Test
    void rejectsReplaceWithoutCreate() {
        assertThrows(IllegalArgumentException.class, () -> new PostgresExportSpec(
                "out",
                "pg",
                "SELECT 1 AS id",
                "public.result",
                PostgresWriteMode.REPLACE,
                PostgresWritePath.JDBC,
                false,
                List.of()));
    }

    @Test
    void requestRejectsDuplicateSourceAndTargetAlias() {
        PostgresSourceSpec source = new PostgresSourceSpec(
                "pg",
                new DatabaseSpec("jdbc:postgresql://localhost/db", "u", "p"),
                "view",
                true,
                List.of(PostgresTableSpec.fromQualifiedName("agi_pub.gemeinden", null, null, List.of(), List.of())));
        PostgresTargetSpec target = new PostgresTargetSpec(
                "pg",
                new DatabaseSpec("jdbc:postgresql://localhost/db", "u", "p"));

        assertThrows(IllegalArgumentException.class, () -> new DuckDbExecutionRequest(
                "duck",
                Path.of("build/work.duckdb"),
                false,
                false,
                List.of(source),
                List.of(target),
                List.of(Path.of("sql/analyse.sql")),
                List.of(Map.of()),
                List.of()));
    }
}
