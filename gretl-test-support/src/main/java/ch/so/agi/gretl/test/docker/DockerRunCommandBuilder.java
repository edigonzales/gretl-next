package ch.so.agi.gretl.test.docker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DockerRunCommandBuilder {
    public List<String> build(DockerRunRequest request) {
        List<String> command = new ArrayList<>();
        command.addAll(List.of("docker", "run", "--rm", "--pull=never", "--name", request.containerName()));
        request.user().ifPresent(value -> command.addAll(List.of("--user", value)));
        command.addAll(List.of("--mount", mount(request.projectDirectory(), "/home/gradle/project")));
        command.addAll(List.of("--mount", mount(request.gradleUserHome(), "/home/gradle/.gradle")));
        request.additionalReadOnlyMounts().forEach((host, target) ->
                command.addAll(List.of("--mount", mount(host, target) + ",readonly")));
        request.additionalReadWriteMounts().forEach((host, target) ->
                command.addAll(List.of("--mount", mount(host, target))));
        command.addAll(List.of("--workdir", "/home/gradle/project", "--env", "GRADLE_USER_HOME=/home/gradle/.gradle"));
        for (Map.Entry<String, String> environment : request.environment().entrySet()) {
            command.addAll(List.of("--env", environment.getKey() + "=" + environment.getValue()));
        }
        request.network().ifPresent(network -> command.addAll(List.of("--network", network)));
        command.add(request.imageReference());
        command.addAll(request.commandArguments());
        return List.copyOf(command);
    }

    private String mount(Path host, String target) {
        return "type=bind,src=" + host.toAbsolutePath().normalize() + ",dst=" + target;
    }
}
