package ch.so.agi.gretl.testkit;

import org.gradle.testkit.runner.GradleRunner;

public final class PublishedArtifactBuildExecutor extends AbstractGradleBuildExecutor {
    private final PublishedArtifactTestConfiguration configuration;

    public PublishedArtifactBuildExecutor(PublishedArtifactTestConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected GradleRunner customize(GradleRunner runner) {
        return runner.withTestKitDir(configuration.testKitDirectory().toFile());
    }
}
