package ch.so.agi.gretl.test.execution;

import ch.so.agi.gretl.testkit.PublishedArtifactTestConfiguration;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

public final class PublishedArtifactBuildExecutor implements GretlBuildExecutor {
    @Override
    public GretlBuildResult execute(GretlBuildRequest request) {
        return result(runner(request).build(), request);
    }

    @Override
    public GretlBuildResult executeAndExpectFailure(GretlBuildRequest request) {
        return result(runner(request).buildAndFail(), request);
    }

    private GradleRunner runner(GretlBuildRequest request) {
        PublishedArtifactTestConfiguration configuration = PublishedArtifactTestConfiguration.fromSystemProperties();
        return GradleRunner.create()
                .withProjectDir(request.projectDirectory().toFile())
                .withArguments(request.arguments())
                .withTestKitDir(configuration.testKitDirectory().toFile())
                .forwardOutput();
    }

    private GretlBuildResult result(BuildResult result, GretlBuildRequest request) {
        return new GretlBuildResult(0, result.getOutput(), "", java.time.Duration.ZERO,
                request.arguments(), new GradleTaskOutputParser().parse(result.getOutput()));
    }
}
