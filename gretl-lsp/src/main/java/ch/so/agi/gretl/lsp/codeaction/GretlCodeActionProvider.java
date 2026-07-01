package ch.so.agi.gretl.lsp.codeaction;

import ch.so.agi.gretl.lsp.analysis.AnalysisResult;
import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlArgument;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlExpression;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import ch.so.agi.gretl.lsp.model.ListExpression;
import ch.so.agi.gretl.lsp.util.LevenshteinUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class GretlCodeActionProvider {

    private final GretlMetadata metadata;

    public GretlCodeActionProvider(GretlMetadata metadata) {
        this.metadata = metadata;
    }

    public List<Either<Command, CodeAction>> codeActions(CodeActionParams params, AnalysisResult analysis) {
        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        for (Diagnostic diagnostic : params.getContext().getDiagnostics()) {
            String code = diagnostic.getCode() != null ? diagnostic.getCode().getLeft() : null;
            if (code == null) {
                continue;
            }

            switch (code) {
                case "GRETL1001":
                    actions.addAll(handleMissingRequired(diagnostic, params, analysis));
                    break;
                case "GRETL1002":
                    actions.addAll(handleUnknownProperty(diagnostic, params, analysis));
                    break;
                case "GRETL1101":
                    actions.addAll(handleUnknownDependency(diagnostic, params, analysis));
                    break;
                case "GRETL1201":
                    actions.addAll(handleLegacyDsl(diagnostic, params, analysis));
                    break;
                default:
                    break;
            }
        }

        return actions;
    }

    private List<Either<Command, CodeAction>> handleMissingRequired(Diagnostic diagnostic,
                                                                      CodeActionParams params,
                                                                      AnalysisResult analysis) {
        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        Optional<GretlTaskBlock> taskOpt = findTaskAtRange(analysis, diagnostic.getRange());
        if (taskOpt.isEmpty()) {
            return actions;
        }
        GretlTaskBlock task = taskOpt.get();

        Optional<TaskMetadata> taskMetaOpt = task.typeName().flatMap(metadata::findTask);
        if (taskMetaOpt.isEmpty()) {
            return actions;
        }
        TaskMetadata taskMeta = taskMetaOpt.get();

        Set<String> presentNames = presentCallNames(task);

        for (PropertyMetadata prop : taskMeta.requiredProperties()) {
            if (presentNames.contains(prop.name())) {
                continue;
            }

            Optional<AcceptedForm> modernForm = findModernForm(prop);
            if (modernForm.isEmpty()) {
                continue;
            }
            String insertText = buildInsertText(modernForm.get(), prop);

            Position insertPos = computeInsertPosition(task, analysis.document().lineIndex());
            String indentation = detectIndentation(task, analysis.document().lineIndex());

            TextEdit edit = new TextEdit(
                    new Range(insertPos, insertPos),
                    indentation + insertText + "\n");

            WorkspaceEdit workspaceEdit = new WorkspaceEdit(
                    Map.of(params.getTextDocument().getUri(), List.of(edit)));

            CodeAction action = new CodeAction("F\u00fcge `" + prop.name() + "` hinzu");
            action.setKind(CodeActionKind.QuickFix);
            action.setDiagnostics(List.of(diagnostic));
            action.setEdit(workspaceEdit);

            actions.add(Either.forRight(action));
        }

        return actions;
    }

    private List<Either<Command, CodeAction>> handleUnknownProperty(Diagnostic diagnostic,
                                                                      CodeActionParams params,
                                                                      AnalysisResult analysis) {
        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        Optional<GretlDslCall> callOpt = findCallAtRange(analysis, diagnostic.getRange());
        if (callOpt.isEmpty()) {
            return actions;
        }
        GretlDslCall call = callOpt.get();

        Optional<GretlTaskBlock> taskOpt = findTaskContaining(analysis, diagnostic.getRange());
        if (taskOpt.isEmpty()) {
            return actions;
        }

        Optional<TaskMetadata> taskMetaOpt = taskOpt.get().typeName().flatMap(metadata::findTask);
        if (taskMetaOpt.isEmpty()) {
            return actions;
        }

        Set<String> knownProperties = taskMetaOpt.get().properties().stream()
                .map(PropertyMetadata::name)
                .collect(Collectors.toSet());

        Optional<String> suggestion = LevenshteinUtil.suggestClosest(call.name(), knownProperties);
        if (suggestion.isEmpty()) {
            return actions;
        }

        TextEdit edit = new TextEdit(call.nameRange(), suggestion.get());
        WorkspaceEdit workspaceEdit = new WorkspaceEdit(
                Map.of(params.getTextDocument().getUri(), List.of(edit)));

        CodeAction action = new CodeAction("Korrigiere `" + call.name() + "` zu `" + suggestion.get() + "`");
        action.setKind(CodeActionKind.QuickFix);
        action.setDiagnostics(List.of(diagnostic));
        action.setEdit(workspaceEdit);

        actions.add(Either.forRight(action));
        return actions;
    }

    private List<Either<Command, CodeAction>> handleUnknownDependency(Diagnostic diagnostic,
                                                                        CodeActionParams params,
                                                                        AnalysisResult analysis) {
        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        Optional<GretlDependency> depOpt = findDependencyAtRange(analysis, diagnostic.getRange());
        if (depOpt.isEmpty()) {
            return actions;
        }
        GretlDependency dep = depOpt.get();

        Set<String> knownTaskNames = analysis.script().taskNames();
        if (knownTaskNames.isEmpty()) {
            return actions;
        }

        Optional<String> suggestion = LevenshteinUtil.suggestClosest(dep.targetTaskName(), knownTaskNames);
        if (suggestion.isEmpty()) {
            return actions;
        }

        String originalText = analysis.document().text().substring(
                analysis.document().lineIndex().offsetAt(dep.range().getStart()),
                analysis.document().lineIndex().offsetAt(dep.range().getEnd()));

        char quoteChar = '\'';
        if (!originalText.isEmpty() && originalText.charAt(0) == '"') {
            quoteChar = '"';
        }
        String replacementText = quoteChar + suggestion.get() + quoteChar;

        TextEdit edit = new TextEdit(dep.range(), replacementText);
        WorkspaceEdit workspaceEdit = new WorkspaceEdit(
                Map.of(params.getTextDocument().getUri(), List.of(edit)));

        CodeAction action = new CodeAction("Korrigiere `" + dep.targetTaskName()
                + "` zu `" + suggestion.get() + "`");
        action.setKind(CodeActionKind.QuickFix);
        action.setDiagnostics(List.of(diagnostic));
        action.setEdit(workspaceEdit);

        actions.add(Either.forRight(action));
        return actions;
    }

    private List<Either<Command, CodeAction>> handleLegacyDsl(Diagnostic diagnostic,
                                                               CodeActionParams params,
                                                               AnalysisResult analysis) {
        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        Optional<GretlDslCall> callOpt = findCallAtRange(analysis, diagnostic.getRange());
        if (callOpt.isEmpty()) {
            return actions;
        }
        GretlDslCall call = callOpt.get();

        if (call.style() != DslCallStyle.ASSIGNMENT) {
            return actions;
        }

        String modernText = buildModernMethodCall(call);
        if (modernText == null) {
            return actions;
        }

        TextEdit edit = new TextEdit(call.fullRange(), modernText);
        WorkspaceEdit workspaceEdit = new WorkspaceEdit(
                Map.of(params.getTextDocument().getUri(), List.of(edit)));

        CodeAction action = new CodeAction("Migriere zu moderner DSL-Schreibweise");
        action.setKind(CodeActionKind.QuickFix);
        action.setDiagnostics(List.of(diagnostic));
        action.setEdit(workspaceEdit);

        actions.add(Either.forRight(action));
        return actions;
    }

    private static Optional<AcceptedForm> findModernForm(PropertyMetadata propMeta) {
        for (AcceptedForm form : propMeta.acceptedForms()) {
            if ("method-call".equals(form.style()) && !form.legacy()) {
                return Optional.of(form);
            }
        }
        return Optional.empty();
    }

    static String buildInsertText(AcceptedForm modernForm, PropertyMetadata prop) {
        String sig = modernForm.signature();
        if (sig != null && !sig.isBlank()) {
            return sig;
        }
        return prop.name() + " value";
    }

    static String buildModernMethodCall(GretlDslCall call) {
        if (call.arguments().isEmpty()) {
            return call.name();
        }
        String argsText = convertArgumentsToMethodCall(call.arguments());
        return call.name() + " " + argsText;
    }

    private static String convertArgumentsToMethodCall(List<GretlArgument> arguments) {
        if (arguments.size() == 1) {
            GretlExpression expr = arguments.get(0).expression();
            if (expr instanceof ListExpression le) {
                return le.values().stream()
                        .map(GretlExpression::sourceText)
                        .collect(Collectors.joining(", "));
            }
            return expr.sourceText();
        }
        return arguments.stream()
                .map(a -> a.expression().sourceText())
                .collect(Collectors.joining(", "));
    }

    private static Position computeInsertPosition(GretlTaskBlock task, LineIndex lineIndex) {
        int endLine = task.bodyRange().getEnd().getLine();
        int endChar = task.bodyRange().getEnd().getCharacter();

        if (endChar == 0 && endLine > 0) {
            return new Position(endLine - 1, lineIndex.lineText(endLine - 1).length());
        }
        if (endChar <= 1 && endLine > 0) {
            return new Position(endLine - 1, 0);
        }
        return new Position(endLine, 0);
    }

    private static String detectIndentation(GretlTaskBlock task, LineIndex lineIndex) {
        for (GretlDslCall call : task.calls()) {
            int line = call.fullRange().getStart().getLine();
            String lineText = lineIndex.lineText(line);
            if (lineText != null) {
                int nonSpace = 0;
                while (nonSpace < lineText.length() && lineText.charAt(nonSpace) == ' ') {
                    nonSpace++;
                }
                if (nonSpace > 0) {
                    return lineText.substring(0, nonSpace);
                }
            }
        }
        return "    ";
    }

    private static Set<String> presentCallNames(GretlTaskBlock task) {
        return task.calls().stream()
                .map(GretlDslCall::name)
                .collect(Collectors.toSet());
    }

    private static Optional<GretlTaskBlock> findTaskAtRange(AnalysisResult analysis, Range range) {
        return analysis.script().tasks().stream()
                .filter(t -> rangesOverlap(t.fullRange(), range))
                .findFirst();
    }

    private static Optional<GretlTaskBlock> findTaskContaining(AnalysisResult analysis, Range range) {
        return analysis.script().tasks().stream()
                .filter(t -> rangeInsideRange(range, t.fullRange()))
                .findFirst();
    }

    private static Optional<GretlDslCall> findCallAtRange(AnalysisResult analysis, Range range) {
        for (GretlTaskBlock task : analysis.script().tasks()) {
            for (GretlDslCall call : task.calls()) {
                if (rangesEqual(call.fullRange(), range)) {
                    return Optional.of(call);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<GretlDependency> findDependencyAtRange(AnalysisResult analysis, Range range) {
        for (GretlTaskBlock task : analysis.script().tasks()) {
            for (GretlDependency dep : task.dependencies()) {
                if (rangesEqual(dep.range(), range)) {
                    return Optional.of(dep);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean rangesEqual(Range a, Range b) {
        return a.getStart().equals(b.getStart()) && a.getEnd().equals(b.getEnd());
    }

    private static boolean rangesOverlap(Range a, Range b) {
        if (a.getStart().getLine() > b.getEnd().getLine()) return false;
        if (a.getEnd().getLine() < b.getStart().getLine()) return false;
        if (a.getStart().getLine() == b.getEnd().getLine()
                && a.getStart().getCharacter() > b.getEnd().getCharacter()) return false;
        if (a.getEnd().getLine() == b.getStart().getLine()
                && a.getEnd().getCharacter() < b.getStart().getCharacter()) return false;
        return true;
    }

    private static boolean rangeInsideRange(Range inner, Range outer) {
        if (inner.getStart().getLine() < outer.getStart().getLine()) return false;
        if (inner.getEnd().getLine() > outer.getEnd().getLine()) return false;
        if (inner.getStart().getLine() == outer.getStart().getLine()
                && inner.getStart().getCharacter() < outer.getStart().getCharacter()) return false;
        if (inner.getEnd().getLine() == outer.getEnd().getLine()
                && inner.getEnd().getCharacter() > outer.getEnd().getCharacter()) return false;
        return true;
    }
}
