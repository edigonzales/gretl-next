package ch.so.agi.gretl.lsp.completion;

import ch.so.agi.gretl.lsp.model.DefaultTaskDeclaration;
import ch.so.agi.gretl.lsp.model.DependencyKind;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompletionContextDetectorTest {

    private final CompletionContextDetector detector = new CompletionContextDetector();

    @Test
    @DisplayName("detects TASK_TYPE context when cursor inside parsed type range")
    void detectsTaskTypeFromTypeRange() {
        Range typeRange = new Range(new Position(0, 30), new Position(0, 41));
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                typeRange,
                new Range(new Position(0, 1), new Position(2, 1)),
                new Range(new Position(1, 0), new Position(2, 0)),
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script,
                new Position(0, 35), "");

        assertEquals(CompletionContextKind.TASK_TYPE, ctx.kind());
    }

    @Test
    @DisplayName("detects TASK_TYPE context from line text regex")
    void detectsTaskTypeFromLineText() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script,
                new Position(0, 30), "tasks.register('x', SqlE");

        assertEquals(CompletionContextKind.TASK_TYPE, ctx.kind());
    }

    @Test
    @DisplayName("detects INSIDE_GRETL_TASK_BODY context")
    void detectsInsideTaskBody() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script, new Position(2, 0), "");

        assertEquals(CompletionContextKind.INSIDE_GRETL_TASK_BODY, ctx.kind());
        assertNotNull(ctx.taskBlock());
        assertEquals("x", ctx.taskBlock().name());
    }

    @Test
    @DisplayName("detects DEPENDENCY_TASK_NAME context when inside dependency range")
    void detectsDependencyContext() {
        Range depRange = new Range(new Position(1, 4), new Position(1, 20));
        GretlDependency dep = new GretlDependency(DependencyKind.DEPENDS_ON, "importData", depRange);
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(), List.of(dep), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script, new Position(1, 10), "");

        assertEquals(CompletionContextKind.DEPENDENCY_TASK_NAME, ctx.kind());
    }

    @Test
    @DisplayName("returns UNKNOWN for empty script")
    void returnsUnknownForEmptyScript() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script, new Position(0, 0), "");

        assertEquals(CompletionContextKind.UNKNOWN, ctx.kind());
    }

    @Test
    @DisplayName("returns UNKNOWN for position outside all blocks")
    void returnsUnknownOutsideBlocks() {
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(1, 17), new Position(1, 18)),
                new Range(new Position(1, 30), new Position(1, 41)),
                new Range(new Position(1, 1), new Position(4, 1)),
                new Range(new Position(2, 4), new Position(4, 1)),
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script, new Position(0, 0), "");

        assertEquals(CompletionContextKind.UNKNOWN, ctx.kind());
    }

    @Test
    @DisplayName("positionInside returns true when position is inside range")
    void positionInsideTrue() {
        Range range = new Range(new Position(1, 4), new Position(1, 10));
        assertTrue(CompletionContextDetector.positionInside(new Position(1, 5), range));
    }

    @Test
    @DisplayName("positionInside returns false when position is before range")
    void positionInsideBefore() {
        Range range = new Range(new Position(1, 4), new Position(1, 10));
        assertFalse(CompletionContextDetector.positionInside(new Position(1, 3), range));
    }

    @Test
    @DisplayName("positionInside returns false when position is after range")
    void positionInsideAfter() {
        Range range = new Range(new Position(1, 4), new Position(1, 10));
        assertFalse(CompletionContextDetector.positionInside(new Position(1, 11), range));
    }

    @Test
    @DisplayName("positionInside returns false for null range")
    void positionInsideNullRange() {
        assertFalse(CompletionContextDetector.positionInside(new Position(0, 0), null));
    }

    @Test
    @DisplayName("detects IMPORT context from line text")
    void detectsImportContext() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script,
                new Position(0, 40), "import ch.so.agi.gretl.tasks.SqlE");

        assertEquals(CompletionContextKind.IMPORT, ctx.kind());
        assertEquals("ch.so.agi.gretl.tasks.SqlE", ctx.importPrefix());
    }

    @Test
    @DisplayName("detects IMPORT context when cursor at end of import")
    void detectsImportContextAtEndOfImport() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script,
                new Position(0, 7), "import ");

        assertEquals(CompletionContextKind.IMPORT, ctx.kind());
        assertEquals("", ctx.importPrefix());
    }

    @Test
    @DisplayName("import context returns empty prefix when cursor on keyword")
    void importContextEmptyPrefixOnKeyword() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        CompletionContext ctx = detector.detect(script,
                new Position(0, 5), "import");

        assertEquals(CompletionContextKind.IMPORT, ctx.kind());
        assertEquals("", ctx.importPrefix());
    }
}
