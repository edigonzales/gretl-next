package ch.so.agi.gretl.internal.duckdb;

import java.util.List;

record DuckDbSessionArtifacts(List<String> logicalSchemas, List<String> rawAttachments) {
    DuckDbSessionArtifacts {
        logicalSchemas = List.copyOf(logicalSchemas);
        rawAttachments = List.copyOf(rawAttachments);
    }
}
