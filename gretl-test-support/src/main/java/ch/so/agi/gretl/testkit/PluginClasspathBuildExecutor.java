package ch.so.agi.gretl.testkit;

import org.gradle.testkit.runner.GradleRunner;

public final class PluginClasspathBuildExecutor extends AbstractGradleBuildExecutor {
    @Override
    protected GradleRunner customize(GradleRunner runner) {
        return runner.withPluginClasspath();
    }
}
