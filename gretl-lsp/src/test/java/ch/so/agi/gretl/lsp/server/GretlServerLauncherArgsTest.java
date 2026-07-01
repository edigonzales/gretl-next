package ch.so.agi.gretl.lsp.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GretlServerLauncherArgsTest {

    @Test
    @DisplayName("parses --help flag")
    void parseHelp() {
        GretlServerConfig config = GretlServerConfig.parse("--help");
        assertTrue(config.help());
    }

    @Test
    @DisplayName("parses -h flag")
    void parseShortHelp() {
        GretlServerConfig config = GretlServerConfig.parse("-h");
        assertTrue(config.help());
    }

    @Test
    @DisplayName("parses --stdio flag")
    void parseStdio() {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        assertTrue(config.stdio());
        assertEquals("INFO", config.logLevel());
        assertFalse(config.help());
        assertFalse(config.trace());
    }

    @Test
    @DisplayName("default log level is INFO")
    void defaultLogLevel() {
        GretlServerConfig config = GretlServerConfig.parse();
        assertEquals("INFO", config.logLevel());
    }

    @Test
    @DisplayName("parses --log-level=DEBUG")
    void parseLogLevelDebug() {
        GretlServerConfig config = GretlServerConfig.parse("--log-level=DEBUG", "--stdio");
        assertEquals("DEBUG", config.logLevel());
    }

    @Test
    @DisplayName("parses --log-level=WARN")
    void parseLogLevelWarn() {
        GretlServerConfig config = GretlServerConfig.parse("--log-level=WARN");
        assertEquals("WARN", config.logLevel());
    }

    @Test
    @DisplayName("ignores invalid log level")
    void ignoresInvalidLogLevel() {
        GretlServerConfig config = GretlServerConfig.parse("--log-level=TRACE");
        assertEquals("INFO", config.logLevel());
    }

    @Test
    @DisplayName("parses --trace flag")
    void parseTrace() {
        GretlServerConfig config = GretlServerConfig.parse("--stdio", "--trace");
        assertTrue(config.trace());
    }

    @Test
    @DisplayName("parses --metadata path")
    void parseMetadataPath() {
        GretlServerConfig config = GretlServerConfig.parse("--metadata=/path/to/metadata.json");
        assertNotNull(config.metadataPath());
        assertEquals("/path/to/metadata.json", config.metadataPath().toString().replace('\\', '/'));
    }

    @Test
    @DisplayName("rejects unknown --option")
    void rejectsUnknownOption() {
        assertThrows(IllegalArgumentException.class,
                () -> GretlServerConfig.parse("--unknown"));
    }

    @Test
    @DisplayName("usage text is not empty")
    void usageTextNotEmpty() {
        String usage = GretlServerConfig.usage();
        assertNotNull(usage);
        assertTrue(usage.contains("--stdio"));
        assertTrue(usage.contains("--help"));
    }

    @Test
    @DisplayName("no args yields usable config")
    void noArgsYieldsUsableConfig() {
        GretlServerConfig config = GretlServerConfig.parse();
        assertFalse(config.stdio());
        assertFalse(config.help());
        assertEquals("INFO", config.logLevel());
    }
}
