package ch.so.agi.gretl.lsp.overview;

import org.eclipse.lsp4j.Range;

public record OverviewDiagnostic(String message, String severity, Range range, String taskName) {
}
