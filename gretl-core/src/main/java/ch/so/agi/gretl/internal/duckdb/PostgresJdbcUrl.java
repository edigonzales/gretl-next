package ch.so.agi.gretl.internal.duckdb;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record PostgresJdbcUrl(
        String host,
        int port,
        String database,
        Map<String, String> parameters
) {
    private static final Set<String> LIBPQ_OPTIONS = Set.of(
            "sslmode",
            "connect_timeout",
            "application_name",
            "options",
            "keepalives",
            "keepalives_idle",
            "keepalives_interval",
            "keepalives_count",
            "target_session_attrs",
            "sslcert",
            "sslkey",
            "sslrootcert",
            "sslcrl",
            "gssencmode",
            "krbsrvname",
            "service");

    public PostgresJdbcUrl {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL host must not be blank");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL port must be greater than zero");
        }
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL database must not be blank");
        }
        parameters = Map.copyOf(new LinkedHashMap<>(parameters == null ? Map.of() : parameters));
    }

    public static PostgresJdbcUrl parse(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Only jdbc:postgresql:// URLs are supported: " + jdbcUrl);
        }

        String rest = jdbcUrl.substring("jdbc:postgresql://".length());
        int queryStart = rest.indexOf('?');
        String authorityAndPath = queryStart >= 0 ? rest.substring(0, queryStart) : rest;
        String query = queryStart >= 0 ? rest.substring(queryStart + 1) : "";
        int slash = authorityAndPath.indexOf('/');
        if (slash <= 0 || slash == authorityAndPath.length() - 1) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL must contain host and database: " + jdbcUrl);
        }

        String authority = authorityAndPath.substring(0, slash);
        String database = authorityAndPath.substring(slash + 1);
        int port = 5432;
        String host = authority;
        int colon = authority.lastIndexOf(':');
        if (colon > 0) {
            host = authority.substring(0, colon);
            try {
                port = Integer.parseInt(authority.substring(colon + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid PostgreSQL JDBC URL port: " + jdbcUrl, e);
            }
        }
        return new PostgresJdbcUrl(decode(host), port, decode(database), parseQuery(query));
    }

    public String libpqOptionsWithoutCredentials() {
        Map<String, String> safe = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            String normalized = key.toLowerCase();
            if (LIBPQ_OPTIONS.contains(normalized)) {
                safe.put(normalized, value);
            }
        });
        if (safe.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String separator = "";
        for (Map.Entry<String, String> entry : safe.entrySet()) {
            builder.append(separator)
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue().replace("'", "\\'"));
            separator = " ";
        }
        return builder.toString();
    }

    public String inputSignatureWithoutCredentials() {
        StringBuilder builder = new StringBuilder("jdbc:postgresql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(database);
        String separator = "?";
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String normalized = entry.getKey().toLowerCase();
            if (normalized.equals("user") || normalized.equals("password")) {
                continue;
            }
            builder.append(separator).append(entry.getKey()).append("=").append(entry.getValue());
            separator = "&";
        }
        return builder.toString();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return parameters;
        }
        for (String part : query.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            int equals = part.indexOf('=');
            if (equals < 0) {
                parameters.put(decode(part), "");
            } else {
                parameters.put(decode(part.substring(0, equals)), decode(part.substring(equals + 1)));
            }
        }
        return parameters;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
