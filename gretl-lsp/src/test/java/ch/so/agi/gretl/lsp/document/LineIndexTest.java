package ch.so.agi.gretl.lsp.document;

import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineIndexTest {

    @Test
    @DisplayName("offset of single-line text")
    void singleLineOffset() {
        LineIndex idx = LineIndex.from("hello");
        assertEquals(0, idx.offsetAt(new Position(0, 0)));
        assertEquals(5, idx.offsetAt(new Position(0, 5)));
        assertEquals(1, idx.lineCount());
        assertEquals("hello", idx.lineText(0));
    }

    @Test
    @DisplayName("position of single-line text")
    void singleLinePosition() {
        LineIndex idx = LineIndex.from("hello");
        assertEquals(new Position(0, 0), idx.positionAt(0));
        assertEquals(new Position(0, 5), idx.positionAt(5));
    }

    @Test
    @DisplayName("multiline with LF")
    void multilineLF() {
        LineIndex idx = LineIndex.from("line1\nline2\nline3");
        assertEquals(3, idx.lineCount());
        assertEquals("line1", idx.lineText(0));
        assertEquals("line2", idx.lineText(1));
        assertEquals("line3", idx.lineText(2));
        assertEquals(new Position(1, 0), idx.positionAt(6));
    }

    @Test
    @DisplayName("multiline with CRLF")
    void multilineCRLF() {
        LineIndex idx = LineIndex.from("line1\r\nline2\r\nline3");
        assertEquals(3, idx.lineCount());
        assertEquals("line1", idx.lineText(0));
        assertEquals("line2", idx.lineText(1));
        assertEquals("line3", idx.lineText(2));
    }

    @Test
    @DisplayName("empty string")
    void emptyString() {
        LineIndex idx = LineIndex.from("");
        assertEquals(1, idx.lineCount());
        assertEquals("", idx.lineText(0));
        assertEquals(0, idx.offsetAt(new Position(0, 0)));
        assertEquals(new Position(0, 0), idx.positionAt(0));
    }

    @Test
    @DisplayName("trailing newline")
    void trailingNewline() {
        LineIndex idx = LineIndex.from("hello\n");
        assertEquals(2, idx.lineCount());
        assertEquals("hello", idx.lineText(0));
        assertEquals("", idx.lineText(1));
    }

    @Test
    @DisplayName("offset past end clamps")
    void offsetPastEnd() {
        LineIndex idx = LineIndex.from("hello");
        assertEquals(5, idx.offsetAt(new Position(0, 99)));
        assertEquals(5, idx.offsetAt(new Position(99, 0)));
    }

    @Test
    @DisplayName("position past end clamps")
    void positionPastEnd() {
        LineIndex idx = LineIndex.from("hello");
        assertEquals(new Position(0, 5), idx.positionAt(99));
        assertEquals(new Position(0, 0), idx.positionAt(-1));
    }

    @Test
    @DisplayName("null returns empty index")
    void nullInput() {
        LineIndex idx = LineIndex.from(null);
        assertEquals(1, idx.lineCount());
        assertEquals("", idx.lineText(0));
    }

    @Test
    @DisplayName("lineText for invalid line")
    void lineTextInvalid() {
        LineIndex idx = LineIndex.from("hello");
        assertEquals("", idx.lineText(99));
        assertEquals("", idx.lineText(-1));
    }
}
