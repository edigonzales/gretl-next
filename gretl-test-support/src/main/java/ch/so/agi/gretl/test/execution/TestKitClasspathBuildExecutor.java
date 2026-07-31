package ch.so.agi.gretl.test.execution;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

public final class TestKitClasspathBuildExecutor implements GretlBuildExecutor {
    @Override
    public GretlBuildResult execute(GretlBuildRequest request) {
        return result(runner(request).build(), request);
    }

    @Override
    public GretlBuildResult executeAndExpectFailure(GretlBuildRequest request) {
        return result(runner(request).buildAndFail(), request);
    }

    private GradleRunner runner(GretlBuildRequest request) {
        return GradleRunner.create()
                .withProjectDir(request.projectDirectory().toFile())
                .withArguments(request.arguments())
                .withPluginClasspath()
                .forwardOutput();
    }

    private GretlBuildResult result(BuildResult result, GretlBuildRequest request) {
        return new GretlBuildResult(0, result.getOutput(), "", java.time.Duration.ZERO,
                request.arguments(), new GradleTaskOutputParser().parse(result.getOutput()));
    }
}
