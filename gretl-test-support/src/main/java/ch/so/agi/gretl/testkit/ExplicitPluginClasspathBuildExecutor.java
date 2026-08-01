package ch.so.agi.gretl.testkit;

import org.gradle.testkit.runner.GradleRunner;

/** TestKit executor for a deliberately explicit two-plugin classpath. */
public final class ExplicitPluginClasspathBuildExecutor extends AbstractGradleBuildExecutor {
    private final ExplicitPluginClasspathTestConfiguration configuration;

    public ExplicitPluginClasspathBuildExecutor(ExplicitPluginClasspathTestConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected GradleRunner customize(GradleRunner runner) {
        return runner
                .withPluginClasspath(configuration.readClasspath())
                .withTestKitDir(configuration.testKitDirectory().toFile());
    }
}
