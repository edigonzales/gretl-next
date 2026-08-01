package ch.so.agi.gretl.testkit;

public final class GretlBuildExecutors {
    public static GretlBuildExecutor current() {
        return switch (GretlTestExecutionMode.current()) {
            case PLUGIN_CLASSPATH -> explicitPluginClasspathOrDefault();
            case PUBLISHED_ARTIFACT -> new PublishedArtifactBuildExecutor(
                    PublishedArtifactTestConfiguration.fromSystemProperties());
        };
    }

    private static GretlBuildExecutor explicitPluginClasspathOrDefault() {
        String classpath = System.getProperty(GretlTestSystemProperties.EXPLICIT_PLUGIN_CLASSPATH);
        if (classpath != null && !classpath.isBlank()) {
            return new ExplicitPluginClasspathBuildExecutor(
                    ExplicitPluginClasspathTestConfiguration.fromSystemProperties());
        }
        return new PluginClasspathBuildExecutor();
    }

    private GretlBuildExecutors() {
    }
}
