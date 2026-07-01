package ch.so.agi.gretl.lsp.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SqlParameterExtractorTest {

    private final SqlParameterExtractor extractor = new SqlParameterExtractor();

    @Test
    @DisplayName("extracts single parameter from SQL")
    void extractsSingleParameter() {
        String sql = "DELETE FROM target WHERE dataset = ${dataset};";
        Set<String> names = extractor.extractNames(sql);
        assertEquals(1, names.size());
        assertTrue(names.contains("dataset"));
    }

    @Test
    @DisplayName("extracts multiple parameters from SQL")
    void extractsMultipleParameters() {
        String sql = "SELECT * FROM t WHERE a = ${param_a} AND b = ${param_b};";
        Set<String> names = extractor.extractNames(sql);
        assertEquals(2, names.size());
        assertTrue(names.contains("param_a"));
        assertTrue(names.contains("param_b"));
    }

    @Test
    @DisplayName("returns empty set for SQL without parameters")
    void returnsEmptyForNoParameters() {
        String sql = "SELECT * FROM table;";
        Set<String> names = extractor.extractNames(sql);
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("returns empty for null input")
    void returnsEmptyForNull() {
        Set<String> names = extractor.extractNames(null);
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("returns empty for empty string")
    void returnsEmptyForEmpty() {
        Set<String> names = extractor.extractNames("");
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("ignores incomplete parameter syntax")
    void ignoresIncompleteSyntax() {
        String sql = "SELECT ${ and ${x} and $} and $x};";
        Set<String> names = extractor.extractNames(sql);
        assertEquals(1, names.size());
        assertTrue(names.contains("x"));
    }

    @Test
    @DisplayName("returns occurrence objects with correct ranges")
    void returnsOccurrencesWithRanges() {
        String sql = "DELETE FROM t WHERE dataset = ${dataset};\nSELECT ${other};";
        List<SqlParameterOccurrence> occurrences = extractor.extract(sql);
        assertEquals(2, occurrences.size());
        assertEquals("dataset", occurrences.get(0).name());
        assertEquals(0, occurrences.get(0).range().getStart().getLine());
        assertEquals("other", occurrences.get(1).name());
        assertEquals(1, occurrences.get(1).range().getStart().getLine());
    }

    @Test
    @DisplayName("handles multiline SQL")
    void handlesMultilineSql() {
        String sql = "DELETE\nFROM t\nWHERE dataset = ${dataset};";
        Set<String> names = extractor.extractNames(sql);
        assertEquals(1, names.size());
        assertTrue(names.contains("dataset"));
    }
}
