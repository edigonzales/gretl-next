package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GroovyAstGretlParserTest {

    private GroovyAstGretlParser parser;

    @BeforeEach
    void setUp() {
        parser = new GroovyAstGretlParser();
    }

    @Test
    @DisplayName("extracts task name and type from tasks.register")
    void extractsTaskNameAndType() {
        String text = "tasks.register('executeSql', SqlExecutor) { }";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals("executeSql", task.name());
        assertTrue(task.typeName().isPresent());
        assertEquals("SqlExecutor", task.typeName().get());
        assertTrue(script.astBased());
        assertFalse(script.scannerFallbackUsed());
    }

    @Test
    @DisplayName("extracts method-call DSL inside task closure")
    void extractsMethodCallDsl() {
        String text = "tasks.register('x', SqlExecutor) {\n" +
                "    database dbUri, dbUser, dbPwd\n" +
                "}";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals(1, task.calls().size());
        GretlDslCall call = task.calls().get(0);
        assertEquals("database", call.name());
        assertEquals(DslCallStyle.METHOD_CALL, call.style());
    }

    @Test
    @DisplayName("extracts assignment-style DSL")
    void extractsAssignmentDsl() {
        String text = "tasks.register('x', SqlExecutor) {\n" +
                "    sqlFiles = files('demo.sql')\n" +
                "}";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals(1, task.calls().size());
        GretlDslCall call = task.calls().get(0);
        assertEquals("sqlFiles", call.name());
        assertEquals(DslCallStyle.ASSIGNMENT, call.style());
    }

    @Test
    @DisplayName("extracts dependsOn dependency")
    void extractsDependsOn() {
        String text = "tasks.register('executeSql', SqlExecutor) {\n" +
                "    dependsOn 'importData'\n" +
                "}";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals(1, task.dependencies().size());
        GretlDependency dep = task.dependencies().get(0);
        assertEquals(DependencyKind.DEPENDS_ON, dep.kind());
        assertEquals("importData", dep.targetTaskName());
    }

    @Test
    @DisplayName("extracts defaultTasks declaration")
    void extractsDefaultTasks() {
        String text = "defaultTasks 'executeSql'";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(1, script.defaultTasks().size());
        DefaultTaskDeclaration dtd = script.defaultTasks().get(0);
        assertEquals("executeSql", dtd.taskName());
    }

    @Test
    @DisplayName("extracts multiple tasks")
    void extractsMultipleTasks() {
        String text = "tasks.register('a', SqlExecutor) { }\n" +
                "tasks.register('b', SqlExecutor) { }\n" +
                "tasks.register('c', SqlExecutor) { }";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(3, script.tasks().size());
        assertEquals(Set.of("a", "b", "c"), script.taskNames());
    }

    @Test
    @DisplayName("handles fully-qualified task type")
    void handlesFullyQualifiedType() {
        String text = "tasks.register('x', ch.so.agi.gretl.tasks.SqlExecutor) { }";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertTrue(task.typeName().isPresent());
    }

    @Test
    @DisplayName("extracts sqlParameters with named arguments")
    void extractsSqlParameters() {
        String text = "tasks.register('x', SqlExecutor) {\n" +
                "    sqlParameters dataset: 'Olten'\n" +
                "}";
        GretlScript script = parser.parse("test.gradle", text);

        assertEquals(1, script.tasks().size());
        GretlTaskBlock task = script.tasks().get(0);
        assertEquals(1, task.calls().size());
        GretlDslCall call = task.calls().get(0);
        assertEquals("sqlParameters", call.name());
        assertFalse(call.arguments().isEmpty());
    }

    @Test
    @DisplayName("marks astBased=true for successful parse")
    void marksAstBased() {
        String text = "tasks.register('x', SqlExecutor) { database url, usr, pwd }";
        GretlScript script = parser.parse("test.gradle", text);

        assertTrue(script.astBased());
    }

    @Test
    @DisplayName("returns empty script for non-gretl content")
    void nonGretlContent() {
        String text = "println 'hello world'";
        GretlScript script = parser.parse("test.gradle", text);

        assertTrue(script.tasks().isEmpty());
        assertTrue(script.defaultTasks().isEmpty());
    }

    @Test
    @DisplayName("taskByName finds task by name")
    void taskByName() {
        String text = "tasks.register('executeSql', SqlExecutor) { }";
        GretlScript script = parser.parse("test.gradle", text);

        Optional<GretlTaskBlock> task = script.taskByName("executeSql");
        assertTrue(task.isPresent());
        assertEquals("SqlExecutor", task.get().typeName().orElse(null));

        Optional<GretlTaskBlock> missing = script.taskByName("nonexistent");
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("taskAt finds task by position")
    void taskAt() {
        String text = "tasks.register('executeSql', SqlExecutor) {\n" +
                "    database u, v, w\n" +
                "}";
        GretlScript script = parser.parse("test.gradle", text);

        org.eclipse.lsp4j.Position inside = new org.eclipse.lsp4j.Position(1, 10);
        assertTrue(script.taskAt(inside).isPresent());
    }
}
