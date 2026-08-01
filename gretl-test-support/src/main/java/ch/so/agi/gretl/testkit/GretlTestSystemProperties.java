package ch.so.agi.gretl.testkit;

public final class GretlTestSystemProperties {
    public static final String EXECUTION_MODE = "gretl.test.executionMode";
    public static final String EXPLICIT_PLUGIN_CLASSPATH = "gretl.test.explicitPluginClasspath";
    public static final String PUBLISHED_REPOSITORY = "gretl.test.publishedRepository";
    public static final String PLUGIN_VERSION = "gretl.test.pluginVersion";
    public static final String TEST_KIT_DIRECTORY = "gretl.test.testKitDirectory";
    public static final String GRADLE_JVM_ARGS = "gretl.test.gradleJvmArgs";
    public static final String DEFAULT_GRADLE_JVM_ARGS = "-Xmx768m -XX:MaxMetaspaceSize=512m";
    public static final String COMBINED_TEST = "gretl.test.combined";

    private GretlTestSystemProperties() {
    }
}
