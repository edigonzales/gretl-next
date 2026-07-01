package ch.so.agi.gretl.lsp.codeaction;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.AnalysisResult;
import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.MetadataLoader;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlExpression;
import ch.so.agi.gretl.lsp.model.VariableExpression;
import ch.so.agi.gretl.lsp.parser.GroovyAstGretlParser;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GretlCodeActionProviderTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("provides quick fix for missing required property sqlFiles")
    void missingRequiredPropertyQuickFix() {
        var parser = new GroovyAstGretlParser();
        String text = "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}";
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        AnalysisResult analysis = new AnalysisResult(doc, script, metadata, List.of());

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode("GRETL1001");
        diagnostic.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity.Error);
        diagnostic.setRange(script.tasks().get(0).nameRange());
        diagnostic.setMessage("GRETL1001: Pflichtparameter `sqlFiles` fehlt fuer Task `x`.");
        diagnostic.setSource("gretl-lsp");

        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier("test.gradle"),
                new Range(new Position(0, 0), new Position(0, 0)),
                new CodeActionContext(List.of(diagnostic)));

        var provider = new GretlCodeActionProvider(metadata);
        List<Either<Command, CodeAction>> actions = provider.codeActions(params, analysis);

        assertEquals(1, actions.size());
        CodeAction action = actions.get(0).getRight();
        assertEquals(CodeActionKind.QuickFix, action.getKind());
        assertTrue(action.getTitle().contains("sqlFiles"));

        var edit = action.getEdit().getChanges().get("test.gradle").get(0);
        assertTrue(edit.getNewText().contains("sqlFiles"));
        assertTrue(edit.getNewText().contains("files"));
    }

    @Test
    @DisplayName("inserted text for missing required property preserves indentation")
    void missingRequiredPropertyPreservesIndentation() {
        var parser = new GroovyAstGretlParser();
        String text = "tasks.register('x', SqlExecutor) {\n        database 'url', 'usr', 'pwd'\n}";
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        AnalysisResult analysis = new AnalysisResult(doc, script, metadata, List.of());

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode("GRETL1001");
        diagnostic.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity.Error);
        diagnostic.setRange(script.tasks().get(0).nameRange());
        diagnostic.setMessage("GRETL1001: Pflichtparameter `sqlFiles` fehlt fuer Task `x`.");
        diagnostic.setSource("gretl-lsp");

        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier("test.gradle"),
                new Range(new Position(0, 0), new Position(0, 0)),
                new CodeActionContext(List.of(diagnostic)));

        var provider = new GretlCodeActionProvider(metadata);
        List<Either<Command, CodeAction>> actions = provider.codeActions(params, analysis);

        assertEquals(1, actions.size());
        var edit = actions.get(0).getRight().getEdit().getChanges().get("test.gradle").get(0);
        assertTrue(edit.getNewText().startsWith("        "));
    }

    @Test
    @DisplayName("provides quick fix for unknown property typo")
    void unknownPropertyQuickFix() {
        var parser = new GroovyAstGretlParser();
        String text = "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n    sqlFile files('demo.sql')\n}";
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        AnalysisResult analysis = new AnalysisResult(doc, script, metadata, List.of());

        var call = script.tasks().get(0).calls().stream()
                .filter(c -> c.name().equals("sqlFile")).findFirst().orElseThrow();

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode("GRETL1002");
        diagnostic.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity.Warning);
        diagnostic.setRange(call.fullRange());
        diagnostic.setMessage("GRETL1002: Unbekannte Property `sqlFile`. Meintest du `sqlFiles`?");
        diagnostic.setSource("gretl-lsp");

        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier("test.gradle"),
                new Range(new Position(0, 0), new Position(0, 0)),
                new CodeActionContext(List.of(diagnostic)));

        var provider = new GretlCodeActionProvider(metadata);
        List<Either<Command, CodeAction>> actions = provider.codeActions(params, analysis);

        assertEquals(1, actions.size());
        CodeAction action = actions.get(0).getRight();
        assertEquals(CodeActionKind.QuickFix, action.getKind());
        assertTrue(action.getTitle().contains("sqlFiles"));

        var edit = action.getEdit().getChanges().get("test.gradle").get(0);
        assertEquals("sqlFiles", edit.getNewText());
        // edit replaces only the name portion, not the whole call
        assertEquals(call.nameRange().getStart(), edit.getRange().getStart());
        assertEquals(call.nameRange().getEnd(), edit.getRange().getEnd());
    }

    @Test
    @DisplayName("provides quick fix for unknown dependency")
    void unknownDependencyQuickFix() {
        var parser = new GroovyAstGretlParser();
        String text = "tasks.register('importData', SqlExecutor) {\n" +
                "    database 'url', 'usr', 'pwd'\n" +
                "    sqlFiles files('x.sql')\n" +
                "}\n" +
                "tasks.register('doStuff', SqlExecutor) {\n" +
                "    dependsOn 'importDat'\n" +
                "    database 'url', 'usr', 'pwd'\n" +
                "    sqlFiles files('y.sql')\n" +
                "}";
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        AnalysisResult analysis = new AnalysisResult(doc, script, metadata, List.of());

        var dep = script.tasks().get(1).dependencies().get(0);
        assertEquals("importDat", dep.targetTaskName());

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode("GRETL1101");
        diagnostic.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity.Warning);
        diagnostic.setRange(dep.range());
        diagnostic.setMessage("GRETL1101: Task `importDat` existiert nicht. Meintest du `importData`?");
        diagnostic.setSource("gretl-lsp");

        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier("test.gradle"),
                new Range(new Position(0, 0), new Position(0, 0)),
                new CodeActionContext(List.of(diagnostic)));

        var provider = new GretlCodeActionProvider(metadata);
        List<Either<Command, CodeAction>> actions = provider.codeActions(params, analysis);

        assertEquals(1, actions.size());
        CodeAction action = actions.get(0).getRight();
        assertEquals(CodeActionKind.QuickFix, action.getKind());
        assertTrue(action.getTitle().contains("importData"));

        var edit = action.getEdit().getChanges().get("test.gradle").get(0);
        assertEquals("'importData'", edit.getNewText());
    }

    @Test
    @DisplayName("provides quick fix for legacy DSL assignment migration with list argument")
    void legacyDslMigrationWithList() {
        var parser = new GroovyAstGretlParser();
        String text = "tasks.register('x', SqlExecutor) {\n    database = ['url', 'usr', 'pwd']\n}";
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        AnalysisResult analysis = new AnalysisResult(doc, script, metadata, List.of());

        var call = script.tasks().get(0).calls().get(0);
        assertEquals(DslCallStyle.ASSIGNMENT, call.style());

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode("GRETL1201");
        diagnostic.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity.Information);
        diagnostic.setRange(call.fullRange());
        diagnostic.setMessage("GRETL1201: Alte GRETL-DSL-Schreibweise.");
        diagnostic.setSource("gretl-lsp");

        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier("test.gradle"),
                new Range(new Position(0, 0), new Position(0, 0)),
                new CodeActionContext(List.of(diagnostic)));

        var provider = new GretlCodeActionProvider(metadata);
        List<Either<Command, CodeAction>> actions = provider.codeActions(params, analysis);

        assertEquals(1, actions.size());
        CodeAction action = actions.get(0).getRight();
        assertEquals(CodeActionKind.QuickFix, action.getKind());
        assertTrue(action.getTitle().contains("Migriere"));

        var edit = action.getEdit().getChanges().get("test.gradle").get(0);
        String newText = edit.getNewText();
        assertTrue(newText.startsWith("database "));
        assertTrue(newText.contains("url"));
        assertTrue(newText.contains("usr"));
        assertTrue(newText.contains("pwd"));
        assertFalse(newText.contains("="));
        assertFalse(newText.contains("["));
        assertFalse(newText.contains("]"));
    }

    @Test
    @DisplayName("provides quick fix for legacy DSL assignment migration with method call value")
    void legacyDslMigrationWithMethodCall() {
        var parser = new GroovyAstGretlParser();
        String text = "tasks.register('x', SqlExecutor) {\n    sqlFiles = files('demo.sql')\n}";
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        AnalysisResult analysis = new AnalysisResult(doc, script, metadata, List.of());

        var call = script.tasks().get(0).calls().get(0);
        assertEquals(DslCallStyle.ASSIGNMENT, call.style());
        assertEquals("sqlFiles", call.name());

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode("GRETL1201");
        diagnostic.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity.Information);
        diagnostic.setRange(call.fullRange());
        diagnostic.setMessage("GRETL1201: Alte GRETL-DSL-Schreibweise.");
        diagnostic.setSource("gretl-lsp");

        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier("test.gradle"),
                new Range(new Position(0, 0), new Position(0, 0)),
                new CodeActionContext(List.of(diagnostic)));

        var provider = new GretlCodeActionProvider(metadata);
        List<Either<Command, CodeAction>> actions = provider.codeActions(params, analysis);

        assertEquals(1, actions.size());
        CodeAction action = actions.get(0).getRight();
        assertEquals(CodeActionKind.QuickFix, action.getKind());

        var edit = action.getEdit().getChanges().get("test.gradle").get(0);
        String newText = edit.getNewText();
        assertTrue(newText.startsWith("sqlFiles "));
        assertFalse(newText.contains("="));
        assertTrue(newText.contains("files('demo.sql')"));
    }

    @Test
    @DisplayName("no code actions for diagnostics without matching code")
    void noActionsForUnknownCode() {
        var parser = new GroovyAstGretlParser();
        String text = "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}";
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        AnalysisResult analysis = new AnalysisResult(doc, script, metadata, List.of());

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode("GRETL9999");
        diagnostic.setSeverity(org.eclipse.lsp4j.DiagnosticSeverity.Error);
        diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 0)));
        diagnostic.setMessage("Unknown diagnostic.");
        diagnostic.setSource("gretl-lsp");

        CodeActionParams params = new CodeActionParams(
                new TextDocumentIdentifier("test.gradle"),
                new Range(new Position(0, 0), new Position(0, 0)),
                new CodeActionContext(List.of(diagnostic)));

        var provider = new GretlCodeActionProvider(metadata);
        List<Either<Command, CodeAction>> actions = provider.codeActions(params, analysis);

        assertTrue(actions.isEmpty());
    }

    @Test
    @DisplayName("buildModernMethodCall converts list arguments correctly")
    void buildModernMethodCallWithList() {
        var listExpr = new ch.so.agi.gretl.lsp.model.ListExpression(
                List.of(
                        new VariableExpression("url", new Range(new Position(0, 0), new Position(0, 3)), "url"),
                        new VariableExpression("usr", new Range(new Position(0, 0), new Position(0, 3)), "usr"),
                        new VariableExpression("pwd", new Range(new Position(0, 0), new Position(0, 3)), "pwd")
                ),
                new Range(new Position(0, 0), new Position(0, 1)),
                "[url, usr, pwd]");

        var call = new GretlDslCall("database", DslCallStyle.ASSIGNMENT,
                new Range(new Position(0, 0), new Position(0, 1)),
                new Range(new Position(0, 0), new Position(0, 1)),
                List.of(new ch.so.agi.gretl.lsp.model.GretlArgument(
                        listExpr, new Range(new Position(0, 0), new Position(0, 1)), java.util.Optional.empty())),
                "database = [url, usr, pwd]");

        String result = GretlCodeActionProvider.buildModernMethodCall(call);
        assertEquals("database url, usr, pwd", result);
    }

    @Test
    @DisplayName("buildModernMethodCall handles single non-list argument")
    void buildModernMethodCallNonList() {
        var methodCall = new ch.so.agi.gretl.lsp.model.MethodCallExpression("files",
                List.of(new ch.so.agi.gretl.lsp.model.GretlArgument(
                        new ch.so.agi.gretl.lsp.model.StringLiteralExpression(
                                "demo.sql",
                                new Range(new Position(0, 0), new Position(0, 1)),
                                "'demo.sql'"),
                        new Range(new Position(0, 0), new Position(0, 1)),
                        java.util.Optional.empty())),
                new Range(new Position(0, 0), new Position(0, 1)),
                "files('demo.sql')");

        var call = new GretlDslCall("sqlFiles", DslCallStyle.ASSIGNMENT,
                new Range(new Position(0, 0), new Position(0, 1)),
                new Range(new Position(0, 0), new Position(0, 1)),
                List.of(new ch.so.agi.gretl.lsp.model.GretlArgument(
                        methodCall, new Range(new Position(0, 0), new Position(0, 1)), java.util.Optional.empty())),
                "sqlFiles = files('demo.sql')");

        String result = GretlCodeActionProvider.buildModernMethodCall(call);
        assertEquals("sqlFiles files('demo.sql')", result);
    }

    @Test
    @DisplayName("buildInsertText uses signature from accepted form")
    void buildInsertTextUsesSignature() {
        AcceptedForm form = new AcceptedForm("method-call", "sqlFiles files('script.sql')",
                "sqlFiles files('${1:script.sql}')", 1, false);
        PropertyMetadata prop = new PropertyMetadata("sqlFiles", null, null, null, null, true, false, null, null,
                List.of(form), null, false, null);

        String result = GretlCodeActionProvider.buildInsertText(form, prop);
        assertEquals("sqlFiles files('script.sql')", result);
    }

    @Test
    @DisplayName("buildInsertText falls back to property name when signature is blank")
    void buildInsertTextFallback() {
        AcceptedForm form = new AcceptedForm("method-call", "", "", 1, false);
        PropertyMetadata prop = new PropertyMetadata("myProp", null, null, null, null, true, false, null, null,
                List.of(form), null, false, null);

        String result = GretlCodeActionProvider.buildInsertText(form, prop);
        assertEquals("myProp value", result);
    }
}
