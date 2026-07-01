package ch.so.agi.gretl.lsp.document;

import org.eclipse.lsp4j.Position;

import java.util.ArrayList;
import java.util.List;

public final class LineIndex {

    private final String text;
    private final int[] lineOffsets;
    private final int totalLength;

    private LineIndex(String text, int[] lineOffsets) {
        this.text = text;
        this.lineOffsets = lineOffsets;
        this.totalLength = text != null ? text.length() : 0;
    }

    public static LineIndex from(String text) {
        if (text == null || text.isEmpty()) {
            return new LineIndex(text, new int[]{0});
        }
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                offsets.add(i + 1);
            } else if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    offsets.add(i + 2);
                    i++;
                } else {
                    offsets.add(i + 1);
                }
            }
        }
        int[] arr = offsets.stream().mapToInt(Integer::intValue).toArray();
        return new LineIndex(text, arr);
    }

    public int offsetAt(Position position) {
        int line = position.getLine();
        int character = position.getCharacter();
        if (line < 0) {
            line = 0;
        }
        if (line >= lineCount()) {
            return totalLength;
        }
        int lineStart = lineOffsets[line];
        int maxChar = effectiveLineLength(line);
        if (character > maxChar) {
            character = maxChar;
        }
        return lineStart + character;
    }

    public Position positionAt(int offset) {
        if (offset < 0) {
            offset = 0;
        }
        if (offset > totalLength) {
            offset = totalLength;
        }
        for (int line = lineCount() - 1; line >= 0; line--) {
            int lineStart = lineOffsets[line];
            if (offset >= lineStart) {
                int charOffset = offset - lineStart;
                int maxChar = effectiveLineLength(line);
                if (charOffset > maxChar) {
                    charOffset = maxChar;
                }
                return new Position(line, charOffset);
            }
        }
        return new Position(0, 0);
    }

    public int lineCount() {
        return lineOffsets.length;
    }

    public String lineText(int zeroBasedLine) {
        if (text == null || zeroBasedLine < 0 || zeroBasedLine >= lineCount()) {
            return "";
        }
        int start = lineOffsets[zeroBasedLine];
        int end = (zeroBasedLine + 1 < lineOffsets.length)
                ? lineOffsets[zeroBasedLine + 1]
                : totalLength;
        String raw = text.substring(start, end);
        if (raw.endsWith("\r\n")) {
            return raw.substring(0, raw.length() - 2);
        }
        if (raw.endsWith("\n") || raw.endsWith("\r")) {
            return raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    public int totalLength() {
        return totalLength;
    }

    private int effectiveLineLength(int line) {
        String content = lineText(line);
        return content != null ? content.length() : 0;
    }

    int lineStartOffset(int zeroBasedLine) {
        if (zeroBasedLine < 0 || zeroBasedLine >= lineOffsets.length) {
            return 0;
        }
        return lineOffsets[zeroBasedLine];
    }

    int lineEndOffset(int zeroBasedLine) {
        if (zeroBasedLine < 0 || zeroBasedLine >= lineOffsets.length) {
            return totalLength;
        }
        if (zeroBasedLine + 1 < lineOffsets.length) {
            return lineOffsets[zeroBasedLine + 1];
        }
        return totalLength;
    }
}
