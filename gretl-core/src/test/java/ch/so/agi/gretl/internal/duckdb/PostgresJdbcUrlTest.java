package ch.so.agi.gretl.internal.duckdb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostgresJdbcUrlTest {
    @Test
    void parsesHostPortDatabaseAndQueryParameters() {
        PostgresJdbcUrl url = PostgresJdbcUrl.parse(
                "jdbc:postgresql://db.example.com:5544/pub?sslmode=require&connect_timeout=10");

        assertEquals("db.example.com", url.host());
        assertEquals(5544, url.port());
        assertEquals("pub", url.database());
        assertEquals("require", url.parameters().get("sslmode"));
        assertEquals("10", url.parameters().get("connect_timeout"));
    }

    @Test
    void omitsCredentialsFromLibpqOptions() {
        PostgresJdbcUrl url = PostgresJdbcUrl.parse(
                "jdbc:postgresql://localhost/pub?user=secret_user&password=secret_pwd&sslmode=require");

        String options = url.libpqOptionsWithoutCredentials();

        assertEquals("sslmode=require", options);
        assertFalse(options.contains("secret_user"));
        assertFalse(options.contains("secret_pwd"));
    }

    @Test
    void omitsCredentialsFromInputSignature() {
        PostgresJdbcUrl url = PostgresJdbcUrl.parse(
                "jdbc:postgresql://localhost/pub?user=secret_user&password=secret_pwd&loggerLevel=OFF");

        String signature = url.inputSignatureWithoutCredentials();

        assertEquals("jdbc:postgresql://localhost:5432/pub?loggerLevel=OFF", signature);
        assertFalse(signature.contains("secret_user"));
        assertFalse(signature.contains("secret_pwd"));
    }

    @Test
    void rejectsUnsupportedUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> PostgresJdbcUrl.parse("jdbc:sqlite:/tmp/test.db"));
    }
}
