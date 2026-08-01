package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.GradleTaskOutputParser;
import ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.runtime.RuntimeImageServiceContainer;
import ch.so.agi.gretl.test.process.ProcessResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.util.Optional;

public final class RuntimeImageServiceJobExecutionBackend implements TestJobExecutionBackend {
    private final RuntimeImageServiceContainer service;
    private final Path mountedJobsRoot;

    private RuntimeImageServiceJobExecutionBackend(RuntimeImageServiceContainer service, Path mountedJobsRoot) {
        this.service = service;
        this.mountedJobsRoot = mountedJobsRoot.toAbsolutePath().normalize();
    }

    public static RuntimeImageServiceJobExecutionBackend start(RuntimeImageDescriptor image, Path jobsRoot,
                                                               Path gradleUserHome, Optional<String> network,
                                                               Optional<String> user) {
        return new RuntimeImageServiceJobExecutionBackend(
                RuntimeImageServiceContainer.start(image, jobsRoot, gradleUserHome, network, user), jobsRoot);
    }

    @Override public TestJobExecutionTarget target() { return TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE; }

    @Override public GretlBuildResult execute(TestJobExecutionRequest request) { return executeInternal(request); }
    @Override public GretlBuildResult executeAndExpectFailure(TestJobExecutionRequest request) { return executeInternal(request); }

    private GretlBuildResult executeInternal(TestJobExecutionRequest request) {
        Path project = request.job().projectDirectory().toAbsolutePath().normalize();
        if (!project.startsWith(mountedJobsRoot)) throw new IllegalArgumentException("Service job must be below mounted jobs root: " + project);
        Path relative = mountedJobsRoot.relativize(project);
        var arguments = new RuntimeImageLifecycleArguments().arguments(
                ch.so.agi.gretl.test.execution.RuntimeExecutionMode.SERVICE,
                runtimeArguments(request, relative));
        ProcessResult result = service.execGretl(relative, arguments, request.secretValues(), request.timeout());
        return new GretlBuildResult(result.exitCode(), result.standardOutput(), result.standardError(), result.duration(),
                result.sanitizedCommand(), new GradleTaskOutputParser().parse(result.output()));
    }

    @Override public void close() { service.close(); }

    private List<String> runtimeArguments(TestJobExecutionRequest request, Path relativeProject) {
        List<String> arguments = new ArrayList<>(request.effectiveArguments());
        String containerProject = "/home/gradle/project/" + relativeProject.toString().replace('\\', '/');
        String hostProject = request.job().projectDirectory().toString();
        for (int i = 0; i < arguments.size(); i++) {
            String value = arguments.get(i);
            if (value.equals(hostProject + "/.gretl-test/task-trace.init.gradle")) {
                arguments.set(i, containerProject + "/.gretl-test/task-trace.init.gradle");
            } else if (value.startsWith("-Dgretl.test.traceFile=")) {
                arguments.set(i, "-Dgretl.test.traceFile=" + containerProject + "/.gretl-test/task-trace.jsonl");
            }
        }
        return List.copyOf(arguments);
    }
}
