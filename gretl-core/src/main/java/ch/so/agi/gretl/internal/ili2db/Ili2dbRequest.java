package ch.so.agi.gretl.internal.ili2db;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;

import java.nio.file.Path;
import java.util.List;

public record Ili2dbRequest(
        Ili2dbFlavor flavor,
        Ili2dbOperation operation,
        DatabaseSpec database,
        Path dbFile,
        Config config,
        List<Ili2dbTransfer> transfers,
        List<String> datasets,
        Path logFile,
        boolean failOnException
) {
    public Ili2dbRequest {
        transfers = transfers == null ? List.of() : List.copyOf(transfers);
        datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
}
