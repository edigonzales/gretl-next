package ch.so.agi.gretl.internal.duckdb;

import java.util.List;

public interface DuckDbSourceSpec {
    String alias();

    List<String> requiredExtensions();

    String inputSignature();
}
