package ch.so.agi.gretl.lsp.overview;

import ch.so.agi.gretl.lsp.model.DefaultTaskDeclaration;
import ch.so.agi.gretl.lsp.model.DependencyKind;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlParseProblem;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import ch.so.agi.gretl.lsp.model.GretlVariableDeclaration;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskGraphBuilderTest {

    private final TaskGraphBuilder builder = new TaskGraphBuilder();

    @Test
    @DisplayName("builds graph with nodes for each task")
    void buildsNodesForEachTask() {
        GretlScript script = scriptWithTasks(
                task("importData", "SqlExecutor"),
                task("executeSql", "DuckDbSqlExecutor")
        );

        TaskGraph graph = builder.build(script);

        assertEquals(2, graph.nodes().size());
        assertTrue(graph.findNode("importData").isPresent());
        assertTrue(graph.findNode("executeSql").isPresent());
    }

    @Test
    @DisplayName("graph nodes have correct task type")
    void nodesHaveCorrectTaskType() {
        GretlScript script = scriptWithTasks(
                task("importData", "Ili2duckdbImport")
        );

        TaskGraph graph = builder.build(script);

        TaskGraphNode node = graph.findNode("importData").orElseThrow();
        assertEquals("importData", node.taskName());
        assertEquals("Ili2duckdbImport", node.taskType());
    }

    @Test
    @DisplayName("builds edges for dependsOn dependencies")
    void buildsEdgesForDependsOn() {
        GretlTaskBlock taskA = task("executeSql", "SqlExecutor");
        GretlTaskBlock taskB = task("importData", "Ili2duckdbImport");
        GretlScript script = scriptWithDependencies(
                List.of(taskA, taskB),
                List.of(new GretlDependency(DependencyKind.DEPENDS_ON, "importData",
                        range(1, 4, 1, 22))),
                taskA
        );

        TaskGraph graph = builder.build(script);

        assertEquals(1, graph.edges().size());
        TaskGraphEdge edge = graph.edges().get(0);
        assertEquals("executeSql", edge.fromTask());
        assertEquals("importData", edge.toTask());
        assertEquals(DependencyKind.DEPENDS_ON, edge.kind());
    }

    @Test
    @DisplayName("builds edges for finalizedBy dependencies")
    void buildsEdgesForFinalizedBy() {
        GretlTaskBlock taskA = task("executeSql", "SqlExecutor");
        GretlTaskBlock taskB = task("validate", "IliValidator");
        GretlScript script = scriptWithDependencies(
                List.of(taskA, taskB),
                List.of(new GretlDependency(DependencyKind.FINALIZED_BY, "validate",
                        range(2, 4, 2, 23))),
                taskA
        );

        TaskGraph graph = builder.build(script);

        assertEquals(1, graph.edges().size());
        assertEquals(DependencyKind.FINALIZED_BY, graph.edges().get(0).kind());
    }

    @Test
    @DisplayName("reports unknown dependency target as problem")
    void reportsUnknownDependencyTarget() {
        GretlTaskBlock taskA = task("executeSql", "SqlExecutor");
        GretlScript script = scriptWithDependencies(
                List.of(taskA),
                List.of(new GretlDependency(DependencyKind.DEPENDS_ON, "nonexistent",
                        range(1, 4, 1, 28))),
                taskA
        );

        TaskGraph graph = builder.build(script);

        assertEquals(1, graph.problems().size());
        assertTrue(graph.problems().get(0).message().contains("nonexistent"));
        assertEquals("Warning", graph.problems().get(0).severity());
    }

    @Test
    @DisplayName("nodes show OK status when no diagnostics")
    void nodesShowOkStatusWithoutDiagnostics() {
        GretlScript script = scriptWithTasks(
                task("importData", "SqlExecutor")
        );

        TaskGraph graph = builder.build(script);

        assertEquals(NodeStatus.OK, graph.nodes().get(0).status());
    }

    @Test
    @DisplayName("nodes reflect diagnostics count")
    void nodesReflectDiagnosticCount() {
        GretlScript script = scriptWithTasks(
                task("importData", "SqlExecutor")
        );

        List<OverviewDiagnostic> diagnostics = List.of(
                new OverviewDiagnostic("Missing property", "Error", range(1, 0, 1, 10), "importData"),
                new OverviewDiagnostic("Unknown property", "Warning", range(1, 0, 1, 10), "importData")
        );

        TaskGraph graph = builder.build(script, diagnostics);

        TaskGraphNode node = graph.findNode("importData").orElseThrow();
        assertEquals(2, node.diagnosticCount());
    }

    @Test
    @DisplayName("empty script produces empty graph")
    void emptyScriptProducesEmptyGraph() {
        GretlScript script = new GretlScript("file:///empty.gradle", List.of(), List.of(), List.of(),
                List.of(), true, false);

        TaskGraph graph = builder.build(script);

        assertEquals(0, graph.nodes().size());
        assertEquals(0, graph.edges().size());
        assertEquals(0, graph.problems().size());
    }

    @Test
    @DisplayName("multiple edges from same task")
    void multipleEdgesFromSameTask() {
        GretlTaskBlock taskA = task("pipeline", "SqlExecutor");
        GretlTaskBlock taskB = task("download", "Curl");
        GretlTaskBlock taskC = task("validate", "IliValidator");
        GretlScript script = scriptWithDependencies(
                List.of(taskA, taskB, taskC),
                List.of(
                        new GretlDependency(DependencyKind.DEPENDS_ON, "download",
                                range(2, 4, 2, 24)),
                        new GretlDependency(DependencyKind.DEPENDS_ON, "validate",
                                range(3, 4, 3, 24))
                ),
                taskA
        );

        TaskGraph graph = builder.build(script);

        assertEquals(2, graph.edges().size());
    }

    private GretlScript scriptWithTasks(GretlTaskBlock... tasks) {
        return new GretlScript("file:///build.gradle", List.of(tasks), List.of(), List.of(),
                List.of(), true, false);
    }

    private GretlScript scriptWithDependencies(List<GretlTaskBlock> allTasks,
                                                List<GretlDependency> depsForFirstTask,
                                                GretlTaskBlock firstTask) {
        List<GretlTaskBlock> updatedTasks = new java.util.ArrayList<>();
        updatedTasks.add(new GretlTaskBlock(
                firstTask.name(), firstTask.typeName(), firstTask.nameRange(),
                firstTask.typeRange(), firstTask.fullRange(), firstTask.bodyRange(),
                firstTask.calls(), depsForFirstTask, firstTask.rawExpressions()
        ));
        for (int i = 1; i < allTasks.size(); i++) {
            updatedTasks.add(allTasks.get(i));
        }
        return new GretlScript("file:///build.gradle", updatedTasks, List.of(), List.of(),
                List.of(), true, false);
    }

    private GretlTaskBlock task(String name, String typeName) {
        Range nameRange = range(1, 20, 1, 20 + name.length());
        Range typeRange = range(1, 23, 1, 23 + typeName.length());
        Range fullRange = range(0, 0, 5, 1);
        Range bodyRange = range(1, 25, 4, 1);
        return new GretlTaskBlock(name, Optional.of(typeName), nameRange, typeRange,
                fullRange, bodyRange, List.of(), List.of(), List.of());
    }

    private Range range(int startLine, int startChar, int endLine, int endChar) {
        return new Range(new Position(startLine, startChar), new Position(endLine, endChar));
    }
}
