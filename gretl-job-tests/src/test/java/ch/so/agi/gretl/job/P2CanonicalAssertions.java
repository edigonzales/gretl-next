package ch.so.agi.gretl.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.fixture.FtpTestFixtureLease;
import ch.so.agi.gretl.test.fixture.HttpRecordedRequest;
import ch.so.agi.gretl.test.fixture.HttpTestFixtureLease;
import ch.so.agi.gretl.test.fixture.PostgisTestFixtureLease;
import ch.so.agi.gretl.test.fixture.S3TestFixtureLease;
import ch.so.agi.gretl.test.job.CommonTestJobAssertions;
import ch.so.agi.gretl.test.job.MaterializedTestJob;
import ch.so.agi.gretl.test.job.TestJobAssertions;
import ch.so.agi.gretl.test.job.TestJobVerificationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class P2CanonicalAssertions implements TestJobAssertions {
    private final String id;

    P2CanonicalAssertions(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void verify(TestJobVerificationContext context) throws Exception {
        MaterializedTestJob job = context.job();
        GretlBuildResult result = context.result();
        CommonTestJobAssertions.assertSuccessful(result);
        CommonTestJobAssertions.assertNoClassloaderFailure(result);
        CommonTestJobAssertions.assertSecretsAbsent(result,
                context.environment().environment().secretValues());

        switch (job.descriptor().id()) {
            case "core-duckdb-spatial" -> assertDuckDb(job);
            case "network-http-curl" -> assertHttp(context);
            case "network-ftp-roundtrip" -> assertFtp(context);
            case "network-s3-roundtrip" -> assertS3(context);
            case "database-postgis-sql" -> assertPostgis(context);
            case "interlis-ili2duckdb-roundtrip" -> assertIli2DuckDb(job);
            case "interlis-ili2pg-lifecycle" -> assertIli2Pg(job);
            default -> throw new AssertionError("No P2 assertion for " + job.descriptor().id());
        }
    }

    private void assertDuckDb(MaterializedTestJob job) throws Exception {
        Path database = job.resolve("build/spatial/spatial.duckdb");
        assertTrue(Files.isRegularFile(database), "DuckDB output missing: " + database);
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:" + database);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("select id, name from p2_colors order by id")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt("id"));
            assertEquals("red", rows.getString("name"));
            assertTrue(rows.next());
            assertEquals(2, rows.getInt("id"));
            assertEquals("blue", rows.getString("name"));
            assertFalse(rows.next());
        }
    }

    private void assertHttp(TestJobVerificationContext context) throws Exception {
        HttpTestFixtureLease lease = context.requireFixture("http", HttpTestFixtureLease.class);
        List<HttpRecordedRequest> requests = lease.requests();
        assertEquals(3, requests.size(), "HTTP fixture request count");
        assertEquals(List.of("/text", "/binary", "/form"), requests.stream()
                .map(HttpRecordedRequest::path).toList());
        assertEquals(Files.readString(context.job().resolveExpected("download.txt")).trim(),
                Files.readString(context.job().resolve("build/download/payload.bin")).trim());
    }

    private void assertFtp(TestJobVerificationContext context) throws Exception {
        FtpTestFixtureLease lease = context.requireFixture("ftp", FtpTestFixtureLease.class);
        assertTrue(lease.isHealthy());
        assertArrayEquals(Files.readAllBytes(context.job().resolve("input/payload.bin")),
                Files.readAllBytes(context.job().resolve("build/download/payload.bin")));
    }

    private void assertS3(TestJobVerificationContext context) throws Exception {
        S3TestFixtureLease lease = context.requireFixture("s3", S3TestFixtureLease.class);
        assertTrue(lease.isHealthy());
        assertEquals("one", Files.readString(context.job().resolve("build/download/one.txt")).trim());
    }

    private void assertPostgis(TestJobVerificationContext context) throws Exception {
        PostgisTestFixtureLease lease = context.requireFixture("postgis", PostgisTestFixtureLease.class);
        String jdbcUrl = lease.endpointView(ch.so.agi.gretl.test.job.TestJobExecutionTarget.PLUGIN_CLASSPATH)
                .require("jdbcUrl").value();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "gretl_user", "gretl_password");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("select id, name from p2_colors order by id")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt("id"));
            assertEquals("red", rows.getString("name"));
            assertTrue(rows.next());
            assertEquals(2, rows.getInt("id"));
            assertEquals("blue", rows.getString("name"));
            assertFalse(rows.next());
        }
    }

    private void assertIli2DuckDb(MaterializedTestJob job) {
        assertTrue(Files.isRegularFile(job.resolve("build/db/data.duckdb")));
        Path export = job.resolve("build/export/export.xml");
        assertTrue(Files.isRegularFile(export));
        assertTrue(read(export).contains("TRANSFER"));
    }

    private void assertIli2Pg(MaterializedTestJob job) throws Exception {
        Path export = job.resolve("build/export/DatasetA-out.xtf");
        Path validation = job.resolve("build/validation.log");
        assertTrue(Files.isRegularFile(export));
        assertTrue(Files.size(export) > 0);
        assertTrue(Files.isRegularFile(validation));
        assertTrue(Files.size(validation) > 0);
    }

    private String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new AssertionError("Cannot read " + file, e);
        }
    }
}
