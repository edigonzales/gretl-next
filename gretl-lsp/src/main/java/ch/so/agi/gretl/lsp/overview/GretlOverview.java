package ch.so.agi.gretl.lsp.overview;

import java.util.List;

public record GretlOverview(String uri, List<OverviewTask> tasks, TaskGraph graph,
                              List<OverviewDiagnostic> diagnostics,
                              SqlParameterReport sqlParameterReport) {

    public GretlOverview {
        tasks = tasks != null ? List.copyOf(tasks) : List.of();
        diagnostics = diagnostics != null ? List.copyOf(diagnostics) : List.of();
    }
}
