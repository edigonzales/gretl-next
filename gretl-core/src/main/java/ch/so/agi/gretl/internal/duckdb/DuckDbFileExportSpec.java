package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;

public interface DuckDbFileExportSpec extends DuckDbExportSpec {
    Path file();

    boolean overwrite();
}
