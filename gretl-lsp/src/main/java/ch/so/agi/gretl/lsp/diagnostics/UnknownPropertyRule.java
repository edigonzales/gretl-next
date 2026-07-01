package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class UnknownPropertyRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (GretlTaskBlock task : input.script().tasks()) {
            Optional<TaskMetadata> taskMeta = findTaskMetadata(input, task);
            if (taskMeta.isEmpty()) {
                continue;
            }
            Set<String> knownProperties = taskMeta.get().properties().stream()
                    .map(PropertyMetadata::name)
                    .collect(Collectors.toSet());

            for (GretlDslCall call : task.calls()) {
                if (knownProperties.contains(call.name())) {
                    continue;
                }
                Optional<String> suggestion = suggestClosest(call.name(), knownProperties);
                String message;
                if (suggestion.isPresent()) {
                    message = DiagnosticCode.UNKNOWN_PROPERTY.format(call.name(), suggestion.get());
                } else {
                    message = DiagnosticCode.UNKNOWN_PROPERTY_NO_SUGGESTION.format(call.name());
                }
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Warning);
                diag.setRange(call.fullRange());
                diag.setCode(DiagnosticCode.UNKNOWN_PROPERTY.code());
                diag.setMessage(message);
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
            }
        }
        return diagnostics;
    }

    private Optional<TaskMetadata> findTaskMetadata(AnalysisInput input, GretlTaskBlock task) {
        return task.typeName().flatMap(tn -> input.metadata().findTask(tn));
    }

    static Optional<String> suggestClosest(String name, Set<String> candidates) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            int distance = levenshteinDistance(name, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best != null && bestDistance <= 3 && bestDistance < name.length()) {
            return Optional.of(best);
        }
        return Optional.empty();
    }

    static int levenshteinDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[a.length()][b.length()];
    }
}
