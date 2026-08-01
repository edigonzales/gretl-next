package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeExecutionMode;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;

import java.nio.file.Path;
import java.util.List;

public final class RuntimeImageOneShotJobExecutionBackend implements TestJobExecutionBackend {
    private final RuntimeImageBuildExecutor executor;

    public RuntimeImageOneShotJobExecutionBackend(RuntimeImageBuildExecutor executor) {
        this.executor = executor;
    }

    @Override public TestJobExecutionTarget target() { return TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT; }

    @Override public GretlBuildResult execute(TestJobExecutionRequest request) {
        return executor.execute(map(request));
    }

    @Override public GretlBuildResult executeAndExpectFailure(TestJobExecutionRequest request) {
        return executor.executeAndExpectFailure(map(request));
    }

    private GretlBuildRequest map(TestJobExecutionRequest request) {
        GretlBuildRequest.Builder builder = GretlBuildRequest.builder(request.job().projectDirectory())
                .arguments(runtimeArguments(request))
                .timeout(request.timeout())
                .runtimeExecutionMode(RuntimeExecutionMode.ONE_SHOT)
                .runtimeImageOptions(request.dockerNetwork().map(RuntimeImageRunOptions::onNetwork)
                        .orElseGet(RuntimeImageRunOptions::defaults));
        request.environment().forEach(builder::environment);
        request.secretValues().forEach(builder::secret);
        return builder.build();
    }

    static java.util.List<String> runtimeArguments(TestJobExecutionRequest request) {
        java.util.List<String> arguments = new java.util.ArrayList<>(request.effectiveArguments());
        Path project = request.job().projectDirectory();
        String containerProject = "/home/gradle/project";
        for (int i = 0; i < arguments.size(); i++) {
            String value = arguments.get(i);
            if (value.equals(project.resolve(".gretl-test/task-trace.init.gradle").toString())) {
                arguments.set(i, containerProject + "/.gretl-test/task-trace.init.gradle");
            } else if (value.startsWith("-Dgretl.test.traceFile=")) {
                arguments.set(i, "-Dgretl.test.traceFile=" + containerProject + "/.gretl-test/task-trace.jsonl");
            }
        }
        return List.copyOf(arguments);
    }
}
