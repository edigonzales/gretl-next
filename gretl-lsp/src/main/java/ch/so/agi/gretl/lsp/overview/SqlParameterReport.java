package ch.so.agi.gretl.lsp.overview;

import java.util.List;

public record SqlParameterReport(List<String> sqlFiles, List<MissingParam> missingParams,
                                  List<UnusedParam> unusedParams) {

    public SqlParameterReport {
        sqlFiles = sqlFiles != null ? List.copyOf(sqlFiles) : List.of();
        missingParams = missingParams != null ? List.copyOf(missingParams) : List.of();
        unusedParams = unusedParams != null ? List.copyOf(unusedParams) : List.of();
    }
}
