package ch.so.agi.gretl.lsp.sql;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SqlParameterExtractor {

    private static final Pattern SQL_PARAM_PATTERN =
            Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)\\}");

    public List<SqlParameterOccurrence> extract(String sqlText) {
        if (sqlText == null || sqlText.isEmpty()) {
            return List.of();
        }
        List<SqlParameterOccurrence> occurrences = new ArrayList<>();
        int[] lineOffsets = computeLineOffsets(sqlText);
        Matcher matcher = SQL_PARAM_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            String name = matcher.group(1);
            int offset = matcher.start();
            Position pos = offsetToPosition(offset, lineOffsets);
            Position endPos = offsetToPosition(matcher.end(), lineOffsets);
            occurrences.add(new SqlParameterOccurrence(name, new Range(pos, endPos)));
        }
        return Collections.unmodifiableList(occurrences);
    }

    public Set<String> extractNames(String sqlText) {
        return extract(sqlText).stream()
                .map(SqlParameterOccurrence::name)
                .collect(Collectors.toSet());
    }

    private static int[] computeLineOffsets(String text) {
        int[] offsets = new int[text.length() + 1];
        int line = 0;
        for (int i = 0; i < text.length(); i++) {
            offsets[i] = line;
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        offsets[text.length()] = line;
        return offsets;
    }

    private static Position offsetToPosition(int offset, int[] lineOffsets) {
        int line = lineOffsets[offset];
        int lineStart = 0;
        for (int i = 0; i < offset; i++) {
            if (lineOffsets[i] != lineOffsets[i + 1]) {
                lineStart = i + 1;
            }
        }
        if (line == 0) {
            lineStart = 0;
        } else {
            lineStart = indexOfLineStart(offset, lineOffsets);
        }
        int character = offset - lineStart;
        return new Position(line, character);
    }

    private static int indexOfLineStart(int offset, int[] lineOffsets) {
        int targetLine = lineOffsets[offset];
        for (int i = offset - 1; i >= 0; i--) {
            if (lineOffsets[i] != targetLine) {
                return i + 1;
            }
        }
        return 0;
    }
}
