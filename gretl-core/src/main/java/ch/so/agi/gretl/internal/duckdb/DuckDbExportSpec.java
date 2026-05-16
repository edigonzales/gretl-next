package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;
import java.util.List;

public interface DuckDbExportSpec {
    String name();

    String query();

    Path file();

    boolean overwrite();

    List<String> requiredExtensions();

    String inputSignature();
}
