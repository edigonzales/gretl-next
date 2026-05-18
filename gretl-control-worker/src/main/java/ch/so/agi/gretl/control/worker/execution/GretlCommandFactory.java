package ch.so.agi.gretl.control.worker.execution;

import ch.so.agi.gretl.control.api.ClaimedRun;
import ch.so.agi.gretl.control.worker.WorkerProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GretlCommandFactory {
    private final WorkerProperties properties;

    public GretlCommandFactory(WorkerProperties properties) {
        this.properties = properties;
    }

    public GretlCommand create(ClaimedRun run) {
        List<String> command = new ArrayList<>();
        command.add(properties.getGretlExecutable());
        command.addAll(run.tasks());
        run.parameters().forEach((name, value) -> command.add("-P" + name + "=" + value));

        Map<String, String> environment = new LinkedHashMap<>();
        String gradleOpts = buildGradleOpts(run);
        if (!gradleOpts.isBlank()) {
            environment.put("GRADLE_OPTS", gradleOpts);
        }
        run.parameters().forEach((name, value) -> environment.put("GRETL_PARAM_" + envName(name), String.valueOf(value)));
        run.secrets().forEach((name, value) -> environment.put("GRETL_SECRET_" + envName(name), value));

        Path workingDirectory = properties.getWorkspaceRoot().resolve(run.projectDir()).normalize().toAbsolutePath();
        return new GretlCommand(List.copyOf(command), workingDirectory, Map.copyOf(environment));
    }

    private String buildGradleOpts(ClaimedRun run) {
        List<String> args = new ArrayList<>();
        if (run.jvmMaxHeap() != null && !run.jvmMaxHeap().isBlank()) {
            args.add("-Xmx" + run.jvmMaxHeap());
        }
        args.addAll(run.jvmArgs());
        return String.join(" ", args);
    }

    private String envName(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
    }
}
