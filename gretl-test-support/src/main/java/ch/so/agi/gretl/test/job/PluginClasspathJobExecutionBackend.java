package ch.so.agi.gretl.test.job;

public final class PluginClasspathJobExecutionBackend extends TestKitJobExecutionBackend {
    public PluginClasspathJobExecutionBackend(ch.so.agi.gretl.testkit.GretlBuildExecutor delegate) {
        super(delegate, TestJobExecutionTarget.PLUGIN_CLASSPATH);
    }
}
