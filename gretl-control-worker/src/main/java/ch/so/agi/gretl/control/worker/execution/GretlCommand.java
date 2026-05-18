package ch.so.agi.gretl.control.worker.execution;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record GretlCommand(
        List<String> command,
        Path workingDirectory,
        Map<String, String> environment) {
}
