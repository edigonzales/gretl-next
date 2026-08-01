package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import org.gradle.testkit.runner.BuildResult;

import java.time.Duration;

public abstract class TestKitJobExecutionBackend implements TestJobExecutionBackend {
    private final ch.so.agi.gretl.testkit.GretlBuildExecutor delegate;
    private final TestJobExecutionTarget target;
    private final TestKitBuildResultAdapter adapter;

    protected TestKitJobExecutionBackend(ch.so.agi.gretl.testkit.GretlBuildExecutor delegate,
                                         TestJobExecutionTarget target) {
        this.delegate = delegate;
        this.target = target;
        this.adapter = new TestKitBuildResultAdapter();
    }

    @Override public TestJobExecutionTarget target() { return target; }

    @Override public GretlBuildResult execute(TestJobExecutionRequest request) {
        long started = System.nanoTime();
        BuildResult result = delegate.run(request.job().projectDirectory(), request.effectiveArguments().toArray(String[]::new));
        return adapter.adapt(result, 0, Duration.ofNanos(System.nanoTime() - started),
                request.effectiveArguments(), request.secretValues());
    }

    @Override public GretlBuildResult executeAndExpectFailure(TestJobExecutionRequest request) {
        long started = System.nanoTime();
        BuildResult result = delegate.runAndFail(request.job().projectDirectory(), request.effectiveArguments().toArray(String[]::new));
        return adapter.adapt(result, 1, Duration.ofNanos(System.nanoTime() - started),
                request.effectiveArguments(), request.secretValues());
    }
}
