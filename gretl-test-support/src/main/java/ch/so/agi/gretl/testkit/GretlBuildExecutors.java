package ch.so.agi.gretl.testkit;

public final class GretlBuildExecutors {
    public static GretlBuildExecutor current() {
        return switch (GretlTestExecutionMode.current()) {
            case PLUGIN_CLASSPATH -> new PluginClasspathBuildExecutor();
            case PUBLISHED_ARTIFACT -> new PublishedArtifactBuildExecutor(
                    PublishedArtifactTestConfiguration.fromSystemProperties());
        };
    }

    private GretlBuildExecutors() {
    }
}
