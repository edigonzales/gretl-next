package ch.so.agi.gretl.test.execution;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;

public final class GretlBuildExecutors {
    public static GretlBuildExecutor forCurrentMode() {
        return forMode(GretlExecutionMode.current());
    }

    public static GretlBuildExecutor forMode(GretlExecutionMode mode) {
        return switch (mode) {
            case TESTKIT_CLASSPATH -> new TestKitClasspathBuildExecutor();
            case PUBLISHED_ARTIFACT -> new PublishedArtifactBuildExecutor();
            case RUNTIME_IMAGE -> new RuntimeImageBuildExecutor(
                    RuntimeImageDescriptor.fromSystemProperties(),
                    new DockerCli(),
                    new ContainerUserResolver(),
                    new RuntimeImageLifecycleArguments());
        };
    }

    private GretlBuildExecutors() {
    }
}
