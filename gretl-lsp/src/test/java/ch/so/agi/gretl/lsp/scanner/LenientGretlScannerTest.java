package ch.so.agi.gretl.lsp.scanner;

import ch.so.agi.gretl.lsp.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LenientGretlScannerTest {

    private LenientGretlScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new LenientGretlScanner();
    }

    @Test
    @DisplayName("finds task in valid build.gradle")
    void findsValidTask() {
        String text = "tasks.register('executeSql', SqlExecutor) {\n" +
                "    database dbUri, dbUser, dbPwd\n" +
                "}";
        GretlScript script = scanner.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals("executeSql", task.name());
        assertEquals("SqlExecutor", task.typeName().orElse(null));
        assertTrue(script.scannerFallbackUsed());
    }

    @Test
    @DisplayName("handles incomplete line")
    void handlesIncompleteLine() {
        String text = "tasks.register('x', SqlExecutor) {\n" +
                "    database dbUri,";
        GretlScript script = scanner.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals("x", task.name());
    }

    @Test
    @DisplayName("handles partial DSL call")
    void handlesPartialCall() {
        String text = "tasks.register('x', SqlExecutor) {\n" +
                "    sql";
        GretlScript script = scanner.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals("x", task.name());
    }

    @Test
    @DisplayName("handles empty task body")
    void handlesEmptyBody() {
        String text = "tasks.register('x', SqlExecutor) {\n}";
        GretlScript script = scanner.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals("x", task.name());
        assertTrue(task.calls().isEmpty());
    }

    @Test
    @DisplayName("handles missing closing brace")
    void handlesMissingClosingBrace() {
        String text = "tasks.register('x', SqlExecutor) {\n" +
                "    dependsOn 'y'";
        GretlScript script = scanner.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals("x", task.name());
    }

    @Test
    @DisplayName("extracts dependencies from scanner")
    void extractsDependencies() {
        String text = "tasks.register('x', SqlExecutor) {\n" +
                "    dependsOn 'y'\n" +
                "}";
        GretlScript script = scanner.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertTrue(task.dependencies().stream()
                .anyMatch(d -> d.kind() == DependencyKind.DEPENDS_ON
                        && d.targetTaskName().equals("y")));
    }

    @Test
    @DisplayName("marks scannerFallbackUsed=true")
    void marksScannerFallbackUsed() {
        String text = "tasks.register('x', SqlExecutor) { }";
        GretlScript script = scanner.parse("test.gradle", text);

        assertTrue(script.scannerFallbackUsed());
        assertFalse(script.astBased());
    }

    @Test
    @DisplayName("handles no tasks at all")
    void handlesNoTasks() {
        String text = "println 'hello'";
        GretlScript script = scanner.parse("test.gradle", text);

        assertTrue(script.tasks().isEmpty());
        assertNotNull(script);
    }

    @Test
    @DisplayName("extracts defaultTasks from scanner")
    void extractsDefaultTasks() {
        String text = "defaultTasks 'executeSql'";
        GretlScript script = scanner.parse("test.gradle", text);

        assertEquals(1, script.defaultTasks().size());
        assertEquals("executeSql", script.defaultTasks().get(0).taskName());
    }
}
