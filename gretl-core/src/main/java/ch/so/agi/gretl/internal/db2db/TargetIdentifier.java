package ch.so.agi.gretl.internal.db2db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public record TargetIdentifier(List<String> parts) {

    public TargetIdentifier {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("target table must not be empty");
        }
        parts = List.copyOf(parts);
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                throw new IllegalArgumentException("target table must not contain empty identifier parts");
            }
        }
    }

    static TargetIdentifier parse(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("target table must not be null or blank");
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean partWasQuoted = false;

        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (ch == '"') {
                if (inQuote && i + 1 < identifier.length() && identifier.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuote = !inQuote;
                    partWasQuoted = true;
                }
            } else if (ch == '.' && !inQuote) {
                parts.add(normalizePart(current, partWasQuoted));
                current.setLength(0);
                partWasQuoted = false;
            } else {
                current.append(ch);
            }
        }

        if (inQuote) {
            throw new IllegalArgumentException("target table contains an unterminated quoted identifier: " + identifier);
        }

        parts.add(normalizePart(current, partWasQuoted));
        return new TargetIdentifier(parts);
    }

    String toSql(Connection connection) throws SQLException {
        String quote = connection.getMetaData().getIdentifierQuoteString();
        List<String> quoted = parts.stream()
                .map(part -> quoteIdentifier(part, quote))
                .toList();
        return String.join(".", quoted);
    }

    String displayName() {
        return String.join(".", parts);
    }

    private static String normalizePart(StringBuilder current, boolean wasQuoted) {
        String part = wasQuoted ? current.toString() : current.toString().trim();
        if (part.isBlank()) {
            throw new IllegalArgumentException("target table must not contain empty identifier parts");
        }
        return part;
    }

    private static String quoteIdentifier(String identifier, String quote) {
        if (quote == null || quote.isBlank()) {
            return identifier;
        }
        return quote + identifier.replace(quote, quote + quote) + quote;
    }
}
