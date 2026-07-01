package ch.so.agi.gretl.lsp.server;

import java.nio.file.Path;
import java.util.List;

public final class GretlServerConfig {

    private final boolean stdio;
    private final String logLevel;
    private final Path metadataPath;
    private final boolean trace;
    private final boolean help;

    private GretlServerConfig(boolean stdio, String logLevel, Path metadataPath, boolean trace, boolean help) {
        this.stdio = stdio;
        this.logLevel = logLevel;
        this.metadataPath = metadataPath;
        this.trace = trace;
        this.help = help;
    }

    public static GretlServerConfig parse(String... args) {
        boolean stdio = false;
        String logLevel = "INFO";
        Path metadataPath = null;
        boolean trace = false;
        boolean help = false;

        for (String arg : args) {
            if (arg.equals("--stdio")) {
                stdio = true;
            } else if (arg.equals("--help") || arg.equals("-h")) {
                help = true;
            } else if (arg.equals("--trace")) {
                trace = true;
            } else if (arg.startsWith("--log-level=")) {
                String level = arg.substring("--log-level=".length()).toUpperCase();
                if (List.of("DEBUG", "INFO", "WARN", "ERROR").contains(level)) {
                    logLevel = level;
                }
            } else if (arg.startsWith("--metadata=")) {
                metadataPath = Path.of(arg.substring("--metadata=".length()));
            } else if (arg.startsWith("--")) {
                throw new IllegalArgumentException("Unknown option: " + arg + ". Use --help for usage.");
            }
        }
        return new GretlServerConfig(stdio, logLevel, metadataPath, trace, help);
    }

    public boolean stdio() {
        return stdio;
    }

    public String logLevel() {
        return logLevel;
    }

    public Path metadataPath() {
        return metadataPath;
    }

    public boolean trace() {
        return trace;
    }

    public boolean help() {
        return help;
    }

    public static String usage() {
        return "Usage: gretl-lsp [OPTIONS]\n"
                + "\n"
                + "Options:\n"
                + "  --stdio                Run in stdio mode (default if no mode specified)\n"
                + "  --log-level=LEVEL      Set log level: DEBUG, INFO, WARN, ERROR (default: INFO)\n"
                + "  --metadata=PATH        Path to gretl-lsp-metadata.json file\n"
                + "  --trace                Enable trace-level logging\n"
                + "  --help, -h             Show this help message\n";
    }
}
