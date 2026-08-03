package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionTarget;

public final class PostgisTestFixtureLease implements TestFixtureLease {
    private final PostgisTestFixture fixture;
    private final String id;
    private final String schema;
    private final String username;
    private final String password;
    private final String database;
    private boolean closed;

    PostgisTestFixtureLease(PostgisTestFixture fixture, String id, String schema,
                            String username, String password, String database) {
        this.fixture = fixture; this.id = id; this.schema = schema; this.username = username;
        this.password = password; this.database = database;
    }
    @Override public String id() { return id; }
    @Override public TestFixtureType type() { return TestFixtureType.POSTGIS; }
    @Override public synchronized TestFixtureEndpointView endpointView(TestJobExecutionTarget target) {
        if (closed) throw new IllegalStateException("PostGIS fixture lease is closed");
        return fixture.endpoint(schema, username, password, database, target);
    }
    @Override public boolean isHealthy() { return !closed && fixture.isRunning(); }
    public String schema() { return schema; }
    public synchronized java.sql.Connection openHostConnection() {
        if (closed) throw new IllegalStateException("PostGIS fixture lease is closed");
        try { return java.sql.DriverManager.getConnection(fixture.hostJdbcUrl(schema), username, password); }
        catch (java.sql.SQLException e) { throw new IllegalStateException("Could not open PostGIS host connection", e); }
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        fixture.dropSchema(schema);
    }
}
