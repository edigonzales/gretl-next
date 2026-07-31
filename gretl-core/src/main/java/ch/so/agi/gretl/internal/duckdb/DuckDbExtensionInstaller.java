package ch.so.agi.gretl.internal.duckdb;

import java.sql.DriverManager;
import java.sql.Statement;

public final class DuckDbExtensionInstaller {
    private DuckDbExtensionInstaller() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[] {"postgres", "spatial"};
        }
        Class.forName("org.duckdb.DuckDBDriver");
        try (var connection = DriverManager.getConnection("jdbc:duckdb:");
             Statement statement = connection.createStatement()) {
            String extensionDirectory = System.getProperty("duckdb.extensionDirectory",
                    System.getenv("DUCKDB_EXTENSION_DIRECTORY"));
            if (extensionDirectory != null && !extensionDirectory.isBlank()) {
                statement.execute("SET extension_directory = '" + extensionDirectory.replace("'", "''") + "'");
            }
            for (String extension : args) {
                statement.execute("INSTALL " + extension);
                statement.execute("LOAD " + extension);
            }
        }
    }
}
