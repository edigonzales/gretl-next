package ch.so.agi.gretl.internal.duckdb;

import java.util.List;

public interface DuckDbTargetSpec {
    String alias();

    List<String> requiredExtensions();

    String inputSignature();
}
